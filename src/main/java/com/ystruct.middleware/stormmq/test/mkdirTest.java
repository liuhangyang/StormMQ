package test;

import java.io.File;

/**
 * Created by yang on 16-11-26.
 */
public class mkdirTest {
    public static void main(String[] args) {
        File f = new File("/home/yang/Stormmq");
        System.out.println(f.getParent());
        boolean result = f.mkdirs();
        System.out.println(result?"true":"false");
    }
}
