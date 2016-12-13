package store;

/**
 * Created by yang on 16-11-30.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 创
 *
 *
 *
 *
 * 建 MapedFile 文件
 */
public class AllocateMappedFileService extends ServiceThread{
    private static final Logger log = LoggerFactory.getLogger(LoggerName.StoreLoggerName);
    private static int WaitTimeOut = 1000 *5;
    private ConcurrentHashMap<String,AllocateRequest> requestTable = new ConcurrentHashMap<String, AllocateRequest>();
    private PriorityBlockingQueue<AllocateRequest> requestQueue = new PriorityBlockingQueue<AllocateRequest>();
    private volatile boolean hasException = false;

    public MapedFile putRequestAndReturnMapedFile(String nextFilePath,String nextNextFilePath,int fileSize) {
        AllocateRequest nextReq = new AllocateRequest(nextFilePath,fileSize);
        AllocateRequest nexNextReq = new AllocateRequest(nextNextFilePath,fileSize);
        boolean nextPutOK = (this.requestTable.putIfAbsent(nextFilePath,nexNextReq) == null);
        boolean nextNextPutOK = (this.requestTable.putIfAbsent(nextNextFilePath,nexNextReq) == null);
        if(nextPutOK) {
            boolean offerOk = this.requestQueue.offer(nextReq);
            if(!offerOk) {
                log.warn("add a request to preallocate queue failed");
            }
        }
        if(nextNextPutOK) {
            boolean offerOk = this.requestQueue.offer(nexNextReq);
            if(!offerOk) {
                log.warn("add a request to preallocate queue failed");
            }
        }
        if(hasException) {
            log.warn(this.getServiceName() + " service has exception.so return null");
            return  null;
        }
        AllocateRequest result = this.requestTable.get(nextFilePath);
        try {
            if(result != null) {
                boolean waitOk = result.getCountDownLatch().await(WaitTimeOut, TimeUnit.MILLISECONDS);
                if(!waitOk) {
                    log.warn("create mmap timeout " + result.getFilePath() + " "+ result.getFileSize());
                }
                this.requestTable.remove(nextFilePath);
                return  result.getMapedFile();
            }else {
                log.error("find preallocate mmap failed,this never happen");
            }
        }catch (InterruptedException e) {
            log.warn(this.getServiceName() + " serviuce has exception. ",e);

        }
        return  null;
    }



    @Override
    public String getServiceName() {
        return AllocateMappedFileService.class.getSimpleName();
    }
    public void shutdown() {
        this.stoped = true;
        this.thread.interrupt();
        try {
            this.thread.join(this.getJointime());
        }catch (InterruptedException e) {
            e.printStackTrace();
        }
        for(AllocateRequest req : this.requestTable.values()){
            if(req.mapedFile != null) {
                log.info("delete pre allocated maped file,{}",req.mapedFile.getFileName());
                req.mapedFile.destroy(1000);
            }
        }
    }

    public void run() {
        log.info(this.getServiceName() + " service started");
        while(!this.isStoped() && this.mmapOperation())
            ;
        log.info(this.getServiceName() + " service end");
    }

    public boolean mmapOperation() {
        AllocateRequest req = null;
        try {
            req = this.requestQueue.take();
            if(null == this.requestTable.get(req.getFilePath())) {
                log.warn("this mmap request expired, maybe cause timeout "+req.getFilePath()+ " "+req.getFileSize());
                return  true;
            }
            if(req.getMapedFile() == null) {
                long beginTime = System.currentTimeMillis();
                MapedFile mapedFile = new MapedFile(req.getFilePath(),req.getFileSize());
                long IdeaTime = UtilAll.computeEclipseTimeMilliseconds(beginTime);
                if(IdeaTime > 10) {
                    int queueSize = this.requestQueue.size();
                    log.warn("create mappedFile spent time(ms) "+ IdeaTime + " queue size "+ queueSize + " "+ req.getFilePath()+ " "+req.getMapedFile());

                }
                req.setMapedFile(mapedFile);
                this.hasException = false;
            }
        }catch (InterruptedException e) {
            log.warn(this.getServiceName() + " service has exception, maybe by shutdown ");
            this.hasException = true;
            return  false;
        }catch (IOException e){
            log.warn(this.getServiceName()+" service has exception,matbe by shutdown");
            this.hasException = true;
            return  false;
        }finally {
            if (req != null) {
                req.getCountDownLatch().countDown();
            }
        }
        return  true;

    }
    class AllocateRequest implements Comparable<AllocateRequest>{
        private String filePath; //文件路径
        private int fileSize; //文件大小
        private CountDownLatch countDownLatch = new CountDownLatch(1);
        private volatile  MapedFile mapedFile = null;

        public AllocateRequest(String filePath,int fileSize){
            this.filePath = filePath;
            this.fileSize = fileSize;
        }
        public String getFilePath(){
            return  filePath;
        }
        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }


        public int getFileSize() {
            return fileSize;
        }


        public void setFileSize(int fileSize) {
            this.fileSize = fileSize;
        }


        public CountDownLatch getCountDownLatch() {
            return countDownLatch;
        }


        public void setCountDownLatch(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }


        public MapedFile getMapedFile() {
            return mapedFile;
        }


        public void setMapedFile(MapedFile mapedFile) {
            this.mapedFile = mapedFile;
        }


        public int compareTo(AllocateRequest other) {
            return this.fileSize < other.fileSize ? 1 : this.fileSize > other.fileSize ? -1 : 0;
        }
    }
}
