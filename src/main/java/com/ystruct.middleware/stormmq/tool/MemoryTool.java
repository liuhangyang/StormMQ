package tool;

/**
 * Created by yang on 16-12-1.
 */
public class MemoryTool {
    public static long freeMemory(){
        Runtime run = Runtime.getRuntime();
        long free = run.freeMemory();
        return free;
    }
    public static long useableMemory(){
        Runtime run = Runtime.getRuntime();
        long max  = run.maxMemory();
        long total = run.totalMemory();
        long free = run.freeMemory();
        long usable = max - total + free;
        return usable;
    }
    public static boolean moreThan(long memory)
    {
       // System.out.println("可用的内存为:"+useableMemory());

        return useableMemory() > memory;
    }
}
