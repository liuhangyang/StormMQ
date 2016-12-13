package FileIBufferI;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
/**
 * Created by yang on 16-11-27.
 */
public class MapBufDelete {
    public static void main(String[] args) {
        try {
            FileInputStream fis=new FileInputStream("./largeFile.txt");
            int sum=0;
            int n;
            long t1=System.currentTimeMillis();
            try {
                while((n=fis.read())>=0){
                    //  数据处理
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            long t=System.currentTimeMillis()-t1;
            System.out.println("传统IOread文件,不使用缓冲区,用时:"+t);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            FileInputStream fis=new FileInputStream("./largeFile.txt");
            BufferedInputStream bis=new BufferedInputStream(fis);
            int sum=0;
            int n;
            long t1=System.currentTimeMillis();
            try {
                while((n=bis.read())>=0){
                  //  数据处理
                }
            } catch (IOException e) {

                e.printStackTrace();
            }
            long t=System.currentTimeMillis()-t1;
            System.out.println("传统IOread文件,使用缓冲区,用时:"+t);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        MappedByteBuffer buffer=null;
        try {
            buffer=new RandomAccessFile("./largeFile.txt","rw").getChannel().map(FileChannel.MapMode.READ_WRITE, 0, 1253244);
            int sum=0;
            int n;
            long t1=System.currentTimeMillis();
            for(int i=0;i<1024*1024*10;i++){
                //  数据处理
            }
            long t=System.currentTimeMillis()-t1;
            System.out.println("内存映射文件读取文件,用时:"+t);
        } catch (FileNotFoundException e) {

            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
        }

    }
}
