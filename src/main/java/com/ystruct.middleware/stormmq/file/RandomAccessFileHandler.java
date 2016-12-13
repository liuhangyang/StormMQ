package file;


import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by yang on 16-12-1.
 */
public class RandomAccessFileHandler implements FileHandler {
    RandomAccessFile randomFile;
    public RandomAccessFileHandler(){

    }

    @Override
    public void Open(String filePath, boolean isForce) {
        try{
            if(isForce){
                randomFile = new RandomAccessFile(filePath,"rwd");
            }else {
                randomFile = new RandomAccessFile(filePath,"rw");
            }
            randomFile.seek(randomFile.length());
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public boolean Write(String filePath, int position, byte[] data) {
        return false;
    }

    @Override
    public byte[] Read(int position, int length) {
        byte[] bytes = null;
        try {
            long fileLength = randomFile.length();
            if(position + length > fileLength ){
                System.out.println(" don't  have enouch data from "+position);
                return null;
            }
            randomFile.seek(position);
            bytes = new byte[length];

            int byteread = randomFile.read(bytes);
            if(byteread != length){
                System.err.println("Read "+length+"bytes from line "+":"+position+" failed");
            }
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
        return  bytes;
    }

    @Override
    public void PrepareForReadNextLine() {
        try{
            randomFile.seek(0);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public byte[] ReadNextObject() {
        byte[] temp = null;
        try{
            byte[] len = new byte[4];
            long num = randomFile.read(len);
            if(num == -1)
                return  null;

            int length = toInt(len);
            temp = new byte[length];
            int readNum = randomFile.read(temp);
            if(readNum != length){
                System.out.println("read not enough bytes from file");
                return  null;
            }
        }catch (EOFException e){
            System.out.println("file end");
            return null;
        }catch (IOException e){
            System.out.println("io failed");
            e.printStackTrace();
        }
        return temp;
    }

    @Override
    public boolean AppendBytes(byte[] data) {
        try {
            randomFile.write(data);
        }catch (IOException e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean AppendObject(byte[] data) {
        try{
            byte[] len = toByteArray(data.length,4);
            byte[] res = byteMerger(len,data);
            randomFile.write(res);
        }catch (IOException e){
            e.printStackTrace();
            return false;
        }
        return  true;
    }

    @Override
    public void close() {
        try{
            randomFile.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    public static byte[] toByteArray(int iSource,int iArrayLen){
        byte[] bLocalArr = new byte[iArrayLen];
        for(int i = 0;(i < 4)&&(i < iArrayLen); i++){
            bLocalArr[i] = (byte)(iSource >> 8 *i & 0xFF);
        }
        return bLocalArr;
    }
    //将byte数组bRefArr转为一个整数,字节数据局的低位是在整形的低字节位
    public static int toInt(byte[] bRefArr){
        int iOutcome = 0;
        byte bLoop;
        for(int i = 0; i < bRefArr.length;++i){
            bLoop = bRefArr[i];
            iOutcome += (bLoop & 0xFF) << (8 * i);
        }
        return  iOutcome;
    }
    public static byte[] byteMerger(byte[] byte_1,byte[] byte_2){
        byte[] byte_3 = new byte[byte_1.length+byte_2.length];
        System.arraycopy(byte_1,0,byte_3,0,byte_1.length);
        System.arraycopy(byte_2,0,byte_3,byte_1.length,byte_2.length);
        return byte_3;
    }
}
