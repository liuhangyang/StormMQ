import org.junit.Test;

import java.io.InputStream;

/**
 * Created by yang on 16-12-1.
 */
public class FileDemo {
    @Test
    public void testFileDemo(){
        InputStream absolutePath1 = this.getClass().getResourceAsStream("/a.txt");
        System.out.println(absolutePath1 == null ? "can't find file" : "file can use");
    }
}
