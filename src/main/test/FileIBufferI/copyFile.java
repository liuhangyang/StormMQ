package FileIBufferI;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
/**
 * Created by yang on 16-11-27.
 */
public class copyFile {
    public void copyFile()throws IOException{
        File source = new File("./largeFile.txt");
        File dest = new File("feifei.txt");
        FileChannel in = null, out = null;
        try {
            in = new FileInputStream(source).getChannel();
            out = new FileOutputStream(dest).getChannel();
            long size = in.size();
            MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);
            out.write(buf);
            in.close();
            out.close();
            System.out.println(source.delete()?"true":"false");//文件复制完成后，删除源文件
        }catch(Exception e){
            e.printStackTrace();
        } finally {
            in.close();
            out.close();
        }
    }

    public static void main(String[] args) throws IOException{
        new copyFile().copyFile();
    }
}
