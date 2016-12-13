package tool;


import sun.security.util.Length;

import javax.lang.model.element.Element;
import java.io.*;
import java.nio.channels.FileChannel;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Created by yang on 16-12-2.
 */
public class QueueFile {
    //文件的初始化长度是4096
    private static final int INITIAL_LENGTH = 4096; //one file system block
    //头部长
    static final int HEADER_LENGTH = 16;
    /**
     * 底层文件,采用一个环形的缓冲区来存储条目
     * Format:
     * Header(16　bytes)
     * Element Ring buffer(file length - 16 bytes)
     *
     * Header:
     * File length:(4 bytes)
     * Element Count(4 byes)
     * First Element Position(4 bytes, =0,if null)
     * Last Element Position(4 bytes,=0,if null)
     *
     * Element:
     * Length(4 bytes)
     * Data(Length bytes)
     */
    private final  RandomAccessFile raf;
    //缓存文件的长度
    int fileLength;
    //element 的数量
    private int elementCount;
    //指向第一个element
    private Element first;
    //指向最后一个element
    private Element last;
    private final byte[] buffer = new byte[16];

    public QueueFile(File file) throws IOException{
        if(!file.exists()) {
            initialize(file);
        }
            raf = open(file);
            readHeader();
    }

    /**
     * 测试用
     *      * @param raf
     * @throws IOException
     */

   QueueFile(RandomAccessFile raf) throws IOException{
        this.raf = raf;
        readHeader();
    }
    //把一个int 值的每一位取出来分别存放
    public static void writeInt(byte[] buffer,int offset,int value){
        buffer[offset] = (byte) (value >> 24);
        buffer[offset + 1] = (byte) (value >> 16);
        buffer[offset + 2] = (byte) (value >> 8);
        buffer[offset + 3] = (byte) value;
    }
    private static void writeInts(byte[] buffer, int... values) {
        int offset = 0;
        for (int value : values) {
            writeInt(buffer, offset, value);
            offset += 4;
        }
    }
    private static int readInt(byte[] buffer,int offset){
        return ((buffer[offset]&0xff) << 24)
                +((buffer[offset+1] & 0xff) << 16)
                +((buffer[offset+2] & 0xff) << 8)
                +(buffer[offset+3] & 0xff);
    }
    /**
     * Reads the header
     * 读16个字节
     */
    private void readHeader() throws IOException{
        raf.seek(0);
        raf.readFully(buffer);
        fileLength = readInt(buffer,0);
        elementCount = readInt(buffer,4);
        int firstOffset = readInt(buffer,8);
        int lastOffset = readInt(buffer,12);
        first= readElement(firstOffset);
        last = readElement(lastOffset);
    }

    /**
     * 写文件的头部,16个字节分为4部分,
     * @param fileLength  文件长度
     * @param elementCount 元素的数
     * @param firstPosition 第一个位置
     * @param lastPosition　最后一个位置
     * @throws IOException
     */
    private void writeHeader(int fileLength, int elementCount,
                             int firstPosition, int lastPosition) throws IOException {
        writeInts(buffer, fileLength, elementCount, firstPosition, lastPosition);
        raf.seek(0);
        raf.write(buffer);
    }

    /**
     * 读取一个Element
     * @param position
     * @return
     * @throws IOException
     */
    private Element readElement(int position)throws IOException{
        if(position == 0){
            return Element.NULL;
        }
        raf.seek(position);
        return new Element(position,raf.readInt());
    }

    /**
     * 原子的初始化一个新的文件
     * @param file
     * @throws IOException
     */
    private static void initialize(File file) throws IOException{
        //用一个临时的文件.
        File tempFile = new File(file.getPath()+".tmp");
        RandomAccessFile raf = open(tempFile);
        try {
            raf.setLength(INITIAL_LENGTH);
            raf.seek(0);
            byte[] headerBuffer = new byte[16];
            writeInts(headerBuffer,INITIAL_LENGTH,0,0,0);
            raf.write(headerBuffer);
        }finally {
            raf.close();
        }
        if(!tempFile.renameTo(file)){
            throw  new IOException("Rename failed");
        }
    }

    /**
     * Open a random access file
     * @param file
     * @return
     * @throws FileNotFoundException
     */
    private static RandomAccessFile open(File file) throws FileNotFoundException{
        return new RandomAccessFile(file,"rwd");
    }

    private int wrapPosition(int position){
        return position < fileLength ? position:HEADER_LENGTH+position - fileLength;
    }

    /**
     * 实现环形缓冲区的写
     * @param position
     * @param buffer
     * @param offset
     * @param count
     * @throws IOException
     */
    private void ringWrite(int position,byte[] buffer,int offset,int count) throws IOException{
        position = wrapPosition(position);
        if(position + count <= fileLength){
            raf.seek(position);
            raf.write(buffer,offset,count);
        }else {
            int beforeEof = fileLength -position;
            raf.seek(position);
            raf.write(buffer,offset,beforeEof);
            raf.seek(HEADER_LENGTH);
            raf.write(buffer,offset+beforeEof,count - beforeEof);
        }
    }
    private void ringRead(int position,byte[] buffer,int offset,int count) throws IOException{
        position = wrapPosition(position);
        if(position + count <= fileLength){
            raf.seek(position);
            raf.readFully(buffer,0,count);
        }else{
            int beforeEof = fileLength - position;
            raf.seek(position);
            raf.readFully(buffer,offset,beforeEof);
            raf.seek(HEADER_LENGTH);
            raf.readFully(buffer,offset + beforeEof,count - beforeEof);
        }
    }

    /**
     * 添加一个element 在queue的结尾
     * @param data
     * @throws IOException
     */
    public void add(byte[] data)throws IOException{
        add(data,0,data.length);
    }

    /**
     *
     * @param data
     * @param offset
     * @param count
     * @throws IOException
     */
    public synchronized void add(byte[] data,int offset,int count) throws IOException{
        Objects.requireNonNull(data,"buffer");
        if((offset | count) < 0 ||  count > data.length - offset){
            throw  new IndexOutOfBoundsException();
        }
        expandIfNecessary(count);

        //在当前的最后一个element后面插入一个element
        boolean wasEmpty = isEmpty();
        int position = wasEmpty ? HEADER_LENGTH : wrapPosition(last.position+Element.HEADER_LENGTH + last.length);
        Element newLast = new Element(position,count);

        //写长度
        writeInt(buffer,0,count);
        ringWrite(newLast.position,buffer,0,Element.HEADER_LENGTH);

        //写数据
        ringWrite(newLast.position+ Element.HEADER_LENGTH,data,offset,count);
        //提交,如果是空的,first == last;
        int firstPosition = wasEmpty ? newLast.position:first.position;
        writeHeader(fileLength,elementCount + 1,firstPosition,newLast.position);
        last = newLast;
        elementCount++;
        if(wasEmpty){
            first = last;
        }
    }

    /**
     * 返回已用的字节数
     * @return
     */
    private int usedBytes() {
        if (elementCount == 0) {
            return HEADER_LENGTH;
        }

            if (last.position >= first.position) {
                return (last.position - first.position) //all buf last entry
                        + Element.HEADER_LENGTH   //last entry
                        + last.length
                        + HEADER_LENGTH;
            } else {
                // tail < head. The queue wraps
                return last.position  // buffer front + header
                        + Element.HEADER_LENGTH
                        + last.length  //last entry
                        + fileLength - first.position; //buffer end
            }

    }

    /**
     * 返回空闲的buffer大小
     * @return
     */
    private int remainingBytes(){
        return fileLength - usedBytes();
    }

    /**
     * 判断queue是否是空的.
     * @return
     */
    public synchronized boolean isEmpty() {
        return elementCount == 0;
    }
    private void expandIfNecessary(int dataLength) throws IOException{
        int elementLength = Element.HEADER_LENGTH + dataLength;
        int remainingBytes = remainingBytes();
        if(remainingBytes >= elementLength ){
            return;
        }
        //扩展
        int previousLength = fileLength;
        int newLength;
        //double the length until we can fit the new data
        do{
            remainingBytes += remainingBytes;
            newLength =previousLength << 1;
            previousLength = newLength;
        }while (remainingBytes < elementLength);
        raf.setLength(newLength);

        //If the buffer is split,we need to make it contiguous
        if(last.position < first.position){
            FileChannel channel = raf.getChannel();
            channel.position(fileLength); //目的位置
            int count = last.position + Element.HEADER_LENGTH + last.length-HEADER_LENGTH;
            if(channel.transferTo(HEADER_LENGTH,count,channel)!= count){
                throw new AssertionError("Copied insuficient number of bytes!");
            }
            //提交这个扩展
            int newLastPosition = fileLength + last.position - HEADER_LENGTH;
            writeHeader(newLength,elementCount,first.position,newLastPosition);
            last = new Element(newLastPosition,last.length);

        }else {
            writeHeader(newLength,elementCount,first.position,last.position);
        }
        fileLength = newLength;
    }



    /**
     * 读最老的element,
     * 如果queue为空,返回空.
     *
     */
    public synchronized byte[] peek() throws IOException{
        if(isEmpty())
            return null;
        int length =  first.length;
        byte[] data = new byte[length];
        ringRead(first.position + Element.HEADER_LENGTH,data,0,length);
        return data;
    }
    /**
     * Invokes reader with the eldest element, if an element is available.
     */
    public synchronized void peek(ElementReader reader) throws IOException {
        if (elementCount > 0) {
            reader.read(new ElementInputStream(first), first.length);
        }
    }

    /**
     * 读每一个element;
     * @param reader
     * @throws IOException
     */
    public synchronized void forEach(ElementReader reader) throws IOException{
        int position = first.position;
        for(int i = 0;i < elementCount;i++){
            Element current = readElement(position);
            reader.read(new ElementInputStream(current),current.length);
            position = wrapPosition(current.position+Element.HEADER_LENGTH+ current.length);
        }
    }

    /**
     * Read s single element;
     */
    private class ElementInputStream extends InputStream{
        private int position;
        private int remaining;

        private ElementInputStream(Element element){
            position = wrapPosition(element.position + Element.HEADER_LENGTH);
            remaining = element.length;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            Objects.requireNonNull(buffer,"buffer");
            if((offset | length) < 0 || length > buffer.length - offset){
                throw  new ArrayIndexOutOfBoundsException();
            }
            if(length > remaining){
                length = remaining;
            }
            ringRead(position,buffer,offset,length);
            position = wrapPosition(position + length);
            remaining = length;
            return length;
        }
        @Override
        public int read() throws IOException {
            if (remaining == 0)
                return -1;
            raf.seek(position);
            int b = raf.read();
            position = wrapPosition(position + 1);
            remaining--;
            return b;
        }
    }

    /**
     *
     * 返回element的数量在this queue;
     * @return
     */
    public synchronized int size(){
        return  elementCount;
    }

    /**
     * 移除最老的element;
     */
    public synchronized void remove() throws IOException{
        if(isEmpty()){
            throw new NoSuchElementException();
        }
        if(elementCount == 1){
            clear();
        }else {
            int newFirstPosition = wrapPosition(first.position + Element.HEADER_LENGTH + first.length);
            ringRead(newFirstPosition,buffer,0,Element.HEADER_LENGTH);
            int length = readInt(buffer,0);
            writeHeader(fileLength,elementCount -1,newFirstPosition,last.position);
            elementCount --;
            first = new Element(newFirstPosition,length);
        }
    }

    /**
     * 清空queue,恢复文件到初始化尺寸
     */
    public synchronized void clear() throws IOException{
       if(fileLength > INITIAL_LENGTH)
           raf.setLength(INITIAL_LENGTH);
        writeHeader(INITIAL_LENGTH,0,0,0);
        elementCount = 0;
        first = last = Element.NULL;
        fileLength = INITIAL_LENGTH;
    }

    /**
     * 关闭底层的文件
     */
    public synchronized void close() throws IOException {
        raf.close();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName()).append('[');
        builder.append("fileLength=").append(fileLength);
        builder.append(", size=").append(elementCount);
        builder.append(", first=").append(first);
        builder.append(", last=").append(last);
        builder.append(", element lengths=[");
        try {
            forEach(new ElementReader() {
                boolean first = true;

                public void read(InputStream in, int length) throws IOException {
                    if (first) {
                        first = false;
                    } else {
                        builder.append(", ");
                    }
                    builder.append(length);
                }
            });
        } catch (IOException e) {
            //Square.warning(e);
        }
        builder.append("]]");
        return builder.toString();
    }


    static class Element{
        //每一个element的长度
       static final int HEADER_LENGTH = 4;
        //NULL element;
        static final Element NULL = new Element(0,0);
        //在文件中的position
        final int position;
        //data的长度
        final int length;
        Element(int position,int length){
            this.position = position;
            this.length = length;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + "position = " + position
                    + ", length = " + length + "]";
        }
    }
    public interface ElementReader{
            public void read(InputStream in,int length) throws IOException;
    }
}
