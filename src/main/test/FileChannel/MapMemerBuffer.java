package FileChannel;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by yang on 16-11-27.
 */
public class MapMemerBuffer {
    public static void main(String[] args) throws Exception{
        ByteBuffer byteBuffer =ByteBuffer.allocate(1024*14*1024);
        byte[] bb = new byte[14*1024*1024];
        FileInputStream fis = new FileInputStream("/home/yang/yanghello.txt");
        FileOutputStream fos = new FileOutputStream("/home/yang/yanghello.txt");
        FileChannel fc = fis.getChannel();
        long start = System.currentTimeMillis();
       // fc.read(byteBuffer); //读取
        MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY,0,fc.size());
        System.out.println(fc.size()/1024);
        long timeEnd = System.currentTimeMillis();// 得到当前的时间
        System.out.println("Read time :" + (timeEnd - start) + "ms");
        start = System.currentTimeMillis();
       // fos.write(bb);//2.写入
        mbb.flip();
        timeEnd = System.currentTimeMillis();
        System.out.println("Write time :" + (timeEnd - start) + "ms");
        fos.flush();
        fc.close();
        fis.close();

    }
}
