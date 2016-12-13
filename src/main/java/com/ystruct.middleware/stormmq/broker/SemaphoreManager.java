package broker;

/**
 * Created by yang on 16-11-26.
 */

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 * 信号量管理
 */
public class SemaphoreManager {
//每一个队列一个信号量管理里面的消费同步
    private static Map<String,Semaphore> semMap = new HashMap<String, Semaphore>();

    //创建一个信号量
    public static void createSemaphore(String key){
        if(semMap.containsKey(key))
            return;
        Semaphore semaphore = new Semaphore(0);
        semMap.put(key,semaphore);
    }
    //信号量的值加1
    public static void increase(String key){
        semMap.get(key).release();
    }
    //信号量的值减1
    public static void descrease(String key){
        try {
            semMap.get(key).acquire();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }
}
