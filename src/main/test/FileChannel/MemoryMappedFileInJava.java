package FileChannel;

import java.io.*;
import java.nio.MappedByteBuffer;
import  java.nio.channels.FileChannel;
/**
 * Created by yang on 16-11-27.
 */
public class MemoryMappedFileInJava {
    private static int count = 1024*1024*10; // 10 MB
    public static void readWriteMapped() throws Exception{
        long start = System.currentTimeMillis();
        RandomAccessFile memoryMappedFile = new RandomAccessFile("largeFile.txt", "rw");


        // Mapping a file into memory
        MappedByteBuffer out = memoryMappedFile.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, count);


        // Writing into Memory Mapped File
        for (int i = 0; i < count; i++) {
            out.put((byte) 'A');
        }
       long end=  System.currentTimeMillis();
        System.out.println("Writing to Memory Mapped File is completed,count time:"+(end-start));


        // reading from memory file in Java
        start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            //System.out.print((char) out.get(i));
            out.get(i);
        }
         end=  System.currentTimeMillis();
        System.out.println("Reading from Memory Mapped File is completed,count time:"+(end-start));


        memoryMappedFile.close();
    }
    public static void readWriteFile()throws Exception{
        long start1 =System.currentTimeMillis();
        FileWriter writer = new FileWriter("largeFile1.txt");
        for(int i=0;i<count;++i){
            writer.write("A");
        }
        long end1 =System.currentTimeMillis();
        System.out.println("Write File count time:"+(end1-start1));
        long start = System.currentTimeMillis();
        FileReader fileReader = new FileReader("largeFile1.txt");
        while(fileReader.read()!=-1){

        }
        long end = System.currentTimeMillis();
        System.out.println("read  file count time:"+(end-start));
    }

    public static void main(String[] args)throws Exception {
        MemoryMappedFileInJava.readWriteMapped();
        //MemoryMappedFileInJava.readWriteFile();


    }
}
