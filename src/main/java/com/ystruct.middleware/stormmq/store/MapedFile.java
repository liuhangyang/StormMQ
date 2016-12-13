package store;

/**
 * Created by yang on 16-11-26.
 */

import com.esotericsoftware.kryo.Registration;
import io.netty.buffer.ByteBuf;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Objects;
import java.util.concurrent.Exchanger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * PageCache 文件访问封装
 *
 */
public class MapedFile extends ReferenceResource {
    public static final int OS_PAGE_SIZE = 1024 * 4;
    private static final Logger log = LoggerFactory.getLogger(LoggerName.StoreLoggerName);
    //当前JVM中映射的虚拟内存总大小
    private static final AtomicLong TotalMapedVitualMemory = new AtomicLong(0);
    //当前JVM中mmap句柄的数量
    private static final AtomicInteger TotalMapedFiles = new AtomicInteger(0);
    //映射的文件名
    private final String fileName;
    //映射的起始偏移量
    private final long fileFromOffset;
    //映射的文件大小,定长
    private final int fileSize;
    //映射的文件
    private final File file;
    //映射的内存对象,position永远不变
    private final MappedByteBuffer mappedByteBuffer;
    //当前写到什么位置
    private final AtomicInteger wrotePostion = new AtomicInteger(0);
    //Flush到什么位置
    private final AtomicInteger committedPostion = new AtomicInteger(0);
    //映射的FileChannel对象
    private FileChannel fileChannel;
    //最后一条消息存储的时间
    private volatile long storeTimestamp = 0;
    private boolean firstCreateInQueue = false;

    public MapedFile(String fileName, int fileSize) throws IOException {
        this.fileName = fileName;
        this.file = new File(fileName);
        this.fileFromOffset = Long.parseLong(this.file.getName());
        this.fileSize = fileSize;
        boolean ok = false;
        ensureDirOk(this.file.getParent());

        try {
            this.fileChannel = new RandomAccessFile(this.file, "rw").getChannel();
            this.mappedByteBuffer = this.fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, fileSize);
            TotalMapedVitualMemory.addAndGet(fileSize);
            TotalMapedFiles.incrementAndGet();
        } catch (FileNotFoundException e) {
            log.error("create file channel " + this.fileName + " Failed.", e);
            throw e;
        } catch (IOException e) {
            log.error("map file " + this.fileName + " Failed. ", e);
            throw e;
        } finally {
            if (!ok && this.fileChannel != null) {
                this.fileChannel.close();
            }
        }

    }

    public static void ensureDirOk(final String dirName) {
        if (dirName != null) {
            File f = new File(dirName);
            if (!f.exists()) {
                boolean result = f.mkdirs();
                log.info(dirName + " mkdir " + (result ? "OK" : "Failed"));
            }
        }
    }

    public static void clean(final ByteBuffer buffer) {
        if (buffer == null || !buffer.isDirect() || buffer.capacity() == 0)
            return;
        invoke(invoke(viewed(buffer), "cleaner"), "clean");

    }

    private static Object invoke(final Object target, final String methodName, final Class<?>... args) {
        return AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                try {
                    Method method = method(target, methodName, args);
                    method.setAccessible(true);
                    return method.invoke(target);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }

    private static Method method(Object target, String methodName, Class<?>[] args) throws NoSuchMethodException {
        try {
            return target.getClass().getMethod(methodName, args);
        } catch (NoSuchMethodException e) {
            return target.getClass().getDeclaredMethod(methodName, args);
        }
    }

    private static ByteBuffer viewed(ByteBuffer buffer) {
        String methodName = "viewedBuffer";

        //JDK7中将DirectByteBuffer类中的viewedBuffer方法换成了attachment方法
        Method[] methods = buffer.getClass().getMethods();
        for (int i = 0; i < methods.length; ++i) {
            if (methods[i].getName().equals("attachment")) {
                methodName = "attachment";
                break;
            }
        }
        ByteBuffer viewedBuffer = (ByteBuffer) invoke(buffer, methodName);
        if (viewedBuffer == null) {
            return buffer;
        } else {
            return viewed(viewedBuffer);
        }
    }

    public static int getTotalmapedfiles() {
        return TotalMapedFiles.get();
    }

    public static long getTotalMapedVitualMemory() {
        return TotalMapedVitualMemory.get();
    }

    public long getLastModifiedTimestamp() {
        return this.file.lastModified();
    }

    public String getFileName() {
        return fileName;
    }

    /**
     * 获取文件的大小
     */
    public int getFileSize() {
        return fileSize;
    }

    public FileChannel getFileChannel() {
        return fileChannel;
    }

    /**
     * 向MapedBuffer追加消息
     *
     * @param msg 要追加的消息
     * @param cb  用来对消息进行序列化,
     * @return 是否写入成功, 写入了多少数据.
     */


    public AppendMessageResult appendMessage(final Object msg, final AppendMessageCallback cb) {
        assert msg != null;
        assert cb != null;

        int currentPos = this.wrotePostion.get();

        //代表还有空余的空间
        if (currentPos < this.fileSize) {
            ByteBuffer bytebuffer = this.mappedByteBuffer.slice();
            bytebuffer.position(currentPos);
            AppendMessageResult result = cb.doAppend(this.getFileFromOffset(), bytebuffer, this.fileSize - currentPos, msg);
            this.wrotePostion.addAndGet(result.getWroteBytes());
            this.storeTimestamp = result.getStoreTimestamp();
            return result;
        }
        // 上层应用应该保证不会走到这里
        log.error("MapedFile.appendMessage return null, wrotePostion: " + currentPos + " fileSize: "
                + this.fileSize);
        return new AppendMessageResult(AppendMessageStatus.UNKNOWN_ERROR);
    }

    /**
     * 文件起始偏移量
     */
    public long getFileFromOffset() {
        return this.fileFromOffset;
    }

    /**
     * 向存储层追加数据,一般在SLAVE存储结构中使用.
     */
    public boolean appendMessage(final byte[] data) {
        int currentPos = this.wrotePostion.get();

        //表示有空余的空间
        if ((currentPos + data.length) <= this.fileSize) {
            ByteBuffer byteBuffer = this.mappedByteBuffer.slice();
            byteBuffer.position(currentPos);
            byteBuffer.put(data);
            this.wrotePostion.addAndGet(data.length);
            return true;
        }
        return false;
    }

    /**
     * 消息刷盘,至少刷几个page.
     *
     * @param flushLeastPages
     * @return
     */
    public int commit(final int flushLeastPages) {
        if (this.isAbleToFlush(flushLeastPages)) {
            if (this.hold()) {
                int value = this.wrotePostion.get();
                this.mappedByteBuffer.force();
                this.committedPostion.set(value);
                this.release();
            } else {
                log.warn("im commit,hold failed,commit offset = " + this.committedPostion.get());
                this.committedPostion.set(this.wrotePostion.get());
            }
        }
        return this.getCommittedPostion();
    }

    public int getCommittedPostion() {
        return committedPostion.get();
    }

    public void setCommittedPostion(int pos) {
        this.committedPostion.set(pos);
    }

    private boolean isAbleToFlush(final int flushLeastPages) {
        int flush = this.committedPostion.get();
        int write = this.wrotePostion.get();

        //如果当前文件已经写满,应该立刻刷盘
        if (this.isFull()) {
            return true;
        }
        if (flushLeastPages > 0) {
            return ((write / OS_PAGE_SIZE) - (flush / OS_PAGE_SIZE) >= flushLeastPages);

        }
        return write > flush;
    }

    public boolean isFull() {
        return this.fileSize == this.wrotePostion.get();
    }

    public SelectMapedBufferResult selectMapedBuffer(int pos, int size) {
        //有消息
        if ((pos + size) <= this.wrotePostion.get()) {
            //从MappedBuffer读
            if (this.hold()) {
                ByteBuffer byteBuffer = this.mappedByteBuffer.slice();
                byteBuffer.position(pos);
                ByteBuffer byteBufferNew = byteBuffer.slice();
                byteBufferNew.limit(size);
                return new SelectMapedBufferResult(this.fileFromOffset + pos, byteBufferNew, size, this);
            } else {
                log.warn("matched,but hold failed,requested pos: " + pos + ",fileFromOffset: " + this.fileFromOffset);
            }
        }
        //请求参数非法
        else {
            log.warn("selectMapedBuffer request pos invalid, request pos: " + pos + ", size: " + size
                    + ", fileFromOffset: " + this.fileFromOffset);
        }
        // 非法参数或者mmap资源已经被释放
        return null;
    }
    /**
     * 读逻辑分区
     */
    public SelectMapedBufferResult selectMapedBuffer(int pos) {
        if (pos < this.wrotePostion.get() && pos >= 0) {
            if (this.hold()) {
                ByteBuffer byteBuffer = this.mappedByteBuffer.slice();
                byteBuffer.position(pos);
                int size = this.wrotePostion.get() - pos;
                ByteBuffer byteBufferNew = byteBuffer.slice();
                byteBufferNew.limit(size);
                return new SelectMapedBufferResult(this.fileFromOffset + pos, byteBufferNew, size, this);
            }
        }

        // 非法参数或者mmap资源已经被释放
        return null;
    }

    @Override
    public boolean cleanup(long currentRef) {
        // 如果没有被shutdown，则不可以unmap文件，否则会crash
        if (this.isAvailable()) {
            log.error("this file[REF:" + currentRef + "] " + this.fileName
                    + " have not shutdown, stop unmaping.");
            return false;
        }

        // 如果已经cleanup，再次操作会引起crash
        if (this.isCleanupOver()) {
            log.error("this file[REF:" + currentRef + "] " + this.fileName
                    + " have cleanup, do not do it again.");
            // 必须返回true
            return true;
        }

        clean(this.mappedByteBuffer);
        TotalMapedVitualMemory.addAndGet(this.fileSize * (-1));
        TotalMapedFiles.decrementAndGet();
        log.info("unmap file[REF:" + currentRef + "] " + this.fileName + " OK");
        return true;
    }

    public boolean destroy(final long intervalForcibly) {
        this.shutdown(intervalForcibly);
        if (this.isCleanupOver()) {
            try {
                this.fileChannel.close();
                log.info("close file channel " + this.fileName + "OK");

                long beginTime = System.currentTimeMillis();
                boolean result = this.file.delete();
                log.info("delete file[REF:" + this.getRefCount() + "] " + this.fileName
                        + (result ? " OK, " : " Failed, ") + "W:" + this.getWrotePostion() + " M:"
                        + this.getCommittedPostion() + ", "
                        + UtilAll.computeEclipseTimeMilliseconds(beginTime));
            }catch (Exception e){
                log.warn("close file channel "+this.fileName+" Failed. ",e);
            }
            return true;
        }else {
            log.warn("destroy maped file[REF:" + this.getRefCount() + "] " + this.fileName
                    + " Failed. cleanupOver: " + this.cleanupOver);
        }
        return false;
    }


    public int getWrotePostion() {
        return wrotePostion.get();
    }


    public void setWrotePostion(int pos) {
        this.wrotePostion.set(pos);
    }


    public MappedByteBuffer getMappedByteBuffer() {
        return mappedByteBuffer;
    }

    /**
     * 方法不能在运行时调用，不安全。来对消息进行序列化只在启动时，reload已有数据时调用
     */
    public ByteBuffer sliceByteBuffer() {
        return this.mappedByteBuffer.slice();
    }


    public long getStoreTimestamp() {
        return storeTimestamp;
    }


    public boolean isFirstCreateInQueue() {
        return firstCreateInQueue;
    }


    public void setFirstCreateInQueue(boolean firstCreateInQueue) {
        this.firstCreateInQueue = firstCreateInQueue;
    }
}

