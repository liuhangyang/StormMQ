package file;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by yang on 16-12-1.
 */
public class ChannelFileHander implements FileHandler {

    private FileChannel channel = null;
    private RandomAccessFile fs = null;
    private boolean isForce = false;
    private ByteBuffer buf = ByteBuffer.allocate(4*1024*8); //4kb
    @Override
    public void Open(String filePath, boolean isForce) {
        try {
            fs = new RandomAccessFile(filePath,"rw");
            channel = fs.getChannel();
            this.isForce = isForce;
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }
    }

    @Override
    public boolean Write(String filePath, int position, byte[] data) {
        return false;
    }

    @Override
    public byte[] Read(int position, int length) {
        return new byte[0];
    }

    @Override
    public void PrepareForReadNextLine() {
        try {
            channel.position(0);
        }catch (IOException e){
             e.printStackTrace();
        }
    }


    @Override
    public byte[] ReadNextObject() {
        try {
            ByteBuffer byteBuffer = ByteBuffer.allocate(4);
            while((channel.read(byteBuffer)) > 0);

            if(byteBuffer.array().length != 4){
                System.out.println("Read length info failed");
                return null;
            }
            int len = MessageLog.toInt(byteBuffer.array());
            //System.out.println("len-----------------<:"+len);
            ByteBuffer dataBuffer = ByteBuffer.allocate(len);
            while((channel.read(dataBuffer)) > 0);
           // System.out.println("一直到文件末尾");
            if(dataBuffer.array().length > 0){
             //  System.out.println("ChannelFileHandler:"+dataBuffer.array().length);
               // System.out.println("Read data successfully");
                return dataBuffer.array();
            }else {
                System.out.println("Read data failed.");
                return  null;
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean AppendBytes(byte[] data) {
        buf.clear();
        buf.put(data);
        buf.flip();
       // System.out.println("ChannelFileHandler"+ "data is:"+data.toString()+", in buf:"+buf.array().toString());
        try{
            //判断position和limit之间时候还有数据.
            while(buf.hasRemaining()){
                channel.write(buf);
            }
            if(isForce){
                channel.force(false);
            }
        }catch (IOException e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean AppendObject(byte[] data) {
        byte[] len = MessageLog.toByteArray(data.length,4);
        byte[] res = MessageLog.byteMerger(len,data);
        //System.out.println("保存的消费结果长度:"+res.length);
        return AppendBytes(res);
    }

    @Override
    public void close() {

    }
}
