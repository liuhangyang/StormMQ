package FileChannel;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
/**
 * Created by yang on 16-11-26.
 */
public class NioMap {
    public static void main(String[] args) {
            try {
                RandomAccessFile raf = new RandomAccessFile("nioblock","rw");
                FileChannel channel = raf.getChannel();
                MappedByteBuffer bytebuffer = channel.map(
                        FileChannel.MapMode.READ_WRITE,
                        20,18); // position and size
                CharBuffer charbuffer = bytebuffer.asCharBuffer();
                bytebuffer.load();
                char ch = charbuffer.get(3);
                System.out.println("Character: " + ch);
                charbuffer.put(3,'X');
                channel.close();
            } catch(IOException e) {
                System.out.println(e);
            }
        }
    }

