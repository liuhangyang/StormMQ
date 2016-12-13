package FileIBufferI;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
/**
 * Created by yang on 16-11-27.
 */
public class FileDelete {
    public static void main(String[] args) {
        FileInputStream fis = null;
        File f = new File("/home/yang/yanghello.txt");
        try {
            fis = new FileInputStream(f);
            FileChannel fc = fis.getChannel();

            // 把文件映射到内存
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0,
                    (int) fc.size());

            // TODO

           // fc.close();
          //  fis.close();
        } catch (FileNotFoundException ex) {
            System.err.println("Error! " + ex.getMessage());
            System.exit(2);
        } catch (IOException e) {
            System.err.println("Error! " + e.getMessage());
            System.exit(3);
        }

        // 删除文件
        boolean deleted = f.delete();
        if (!(deleted)) {
            System.err.println("Could not delete file " + f.getName());
        }
    }
}
