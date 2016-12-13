package DirectByteBuffer;

import java.nio.ByteBuffer;

import static store.MapedFile.clean;

/**
 * Created by yang on 16-11-27.
 */
public class directBytebuffer {
    public static void sleep(long i){
        try {
            Thread.sleep(i);
        }catch (Exception e){

        }
    }
    public static void main(String[] args) throws Exception{
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024*1024*500);
        System.out.println("start");
        sleep(10000);
        clean(buffer);
        System.out.println("end");
        sleep(10000);
    }
}
