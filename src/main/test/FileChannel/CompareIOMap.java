package FileChannel;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import  java.nio.channels.FileChannel;
import java.util.Scanner;
/**
 *
 *
 *
 */

/**
 * Created by yang on 16-11-26.
 */
public class CompareIOMap {
    public static void readFile(String path){
        long start = System.currentTimeMillis();//开始时间
        File file = new File(path);
        if(file.isFile()){
            BufferedReader bufferedReader = null;
            FileReader fileReader = null;
            try {
                fileReader = new FileReader(file);
                bufferedReader = new BufferedReader(fileReader);
                String line = bufferedReader.readLine();
                System.out.println("===============传统IO读取数据,使用虚拟机内存==================");
                while (line != null) { //按行读数据
                    System.out.println(line);
                    line = bufferedReader.readLine();
                }
            }catch(FileNotFoundException e){
                e.printStackTrace();
            }catch (IOException e){
                e.printStackTrace();
            }finally {
                try {
                    fileReader.close();
                    bufferedReader.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
            long end = System.currentTimeMillis();
            System.out.println("传统IO读取数据,不指定缓冲区大小,总耗时："+(end-start)+"ms");

        }
    }
    public static void readFile2(String path) throws FileNotFoundException{
        long start = System.currentTimeMillis();//开始时间
        int bufSize = 1024 * 1024 * 5;//5M缓冲区
        File fin = new File(path); // 文件大小200M
        FileChannel fcin = new RandomAccessFile(fin, "r").getChannel();
        ByteBuffer rBuffer = ByteBuffer.allocate(bufSize);

        String enterStr = "";
        long len = 0L;
        try {
            byte[] bs = new byte[bufSize];
            String tempString = null;
            while (fcin.read(rBuffer) != -1) {
                int rSize = rBuffer.position();
                rBuffer.rewind();
                rBuffer.get(bs);
                rBuffer.clear();
                tempString = new String(bs, 0, rSize);
                int fromIndex = 0;//缓冲区起始
                int endIndex = 0;//缓冲区结束
                //按行读缓冲区数据
                while ((endIndex = tempString.indexOf(enterStr, fromIndex)) != -1) {
                    String line = tempString.substring(fromIndex, endIndex);//转换一行
                    System.out.print(line);
                    fromIndex = endIndex + 1;
                }
            }
        } catch (FileNotFoundException ee) {
                ee.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        long end = System.currentTimeMillis();
        System.out.println("传统IO读取数据,指定缓冲区大小,总耗时："+(end-start)+"ms");
    }
    public static void readFile3(String path){
        long start = System.currentTimeMillis();
        long fileLenth = 0;
        final int BUFFER_SZIE = 0x300000;//3M的缓冲
        File file = new File(path);
        fileLenth = file.length();
        try {
            MappedByteBuffer inputBuffer = new RandomAccessFile(file,"r").getChannel().map(FileChannel.MapMode.READ_ONLY,0,fileLenth);
            byte[]  dst = new byte[BUFFER_SZIE];
            for(int offset = 0;offset < fileLenth;offset += BUFFER_SZIE){
                if(fileLenth - offset >= BUFFER_SZIE){
                    for(int i = 0;i < BUFFER_SZIE; ++i){
                        dst[i] = inputBuffer.get(offset+i);
                    }
                }else{
                    for(int i = 0;i< fileLenth - offset;++i){
                        dst[i] = inputBuffer.get(offset+i);
                    }
                }
                Scanner scan = new Scanner(new ByteArrayInputStream(dst));
                while (scan.hasNext()) {
                    // 这里为对读取文本解析的方法
                    System.out.print(scan.next() +" " );
                }
                scan.close();
            }
            System.out.println();
            long end = System.currentTimeMillis();//结束时间
            System.out.println("NIO 内存映射读大文件，总共耗时："+(end - start)+"ms");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws FileNotFoundException{
       CompareIOMap.readFile3("/home/yang/yanghello.txt");
      //  CompareIOMap.readFile2();
       // CompareIOMap.readFile3();

    }
}
