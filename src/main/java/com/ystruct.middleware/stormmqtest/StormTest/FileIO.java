package StormTest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by yang on 16-12-11.
 */
public class FileIO {
    private static File file;
    private static FileWriter fw;
    static {
        file = new File(System.getProperty("file","result"));
        try {
            fw = new FileWriter(file);
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    public static void write(String text){
        try {
            fw.write(text);
            fw.flush();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
