import java.nio.ByteBuffer;

/**
 * Created by yang on 16-12-1.
 */
public class test {

    private static void writeInt(byte[] buffer, int offset, int value) {
        buffer[offset] = (byte) (value >> 24);
        buffer[offset + 1] = (byte) (value >> 16);
        buffer[offset + 2] = (byte) (value >> 8);
        buffer[offset + 3] = (byte) value;
    }
    private static int readInt(byte[] buffer,int offset){
        return ((buffer[offset]&0xff) << 24)
                +((buffer[offset+1] & 0xff) << 16)
                +((buffer[offset+2] & 0xff) << 8)
                +(buffer[offset+3] & 0xff);
    }
    public static void main(String[] args) {
        byte[] buffer = new byte[4];
        writeInt(buffer,0,1000);
        for(byte b:buffer){
            System.out.println("byte:"+b);
        }
       int t = readInt(buffer,0);
        System.out.println("t:"+t);
    }
}
