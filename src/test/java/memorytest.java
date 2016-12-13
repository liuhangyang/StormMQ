/**
 * Created by yang on 16-12-5.
 */
public class memorytest {
    public static long useableMemory(){
        Runtime run = Runtime.getRuntime();
        long max  = run.maxMemory();
        long total = run.totalMemory();
        long free = run.freeMemory();
        long usable = max - total + free;
        return usable;
    }

    public static void main(String[] args) {
        System.out.println("use"+ useableMemory());
    }
}
