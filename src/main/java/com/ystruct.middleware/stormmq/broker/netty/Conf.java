package broker.netty;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by yang on 16-12-1.
 */
public class Conf {
    public static int connNum = 199;
    public static AtomicInteger producerNum = new AtomicInteger(0);
    public static Map<String/*topic*/,String> producerMap = new HashMap<String, String>();
    public static int initValue = 0;
    public static void Increase(String topic){
        synchronized (producerMap){
            if(!producerMap.containsKey(topic)){
                producerNum.incrementAndGet();
                producerMap.put(topic,topic);
            }
        }
    }
    public static void Dcrease(String topic)
    {
        if(producerMap.containsKey(topic))
        {
            producerNum.decrementAndGet();
            producerMap.remove(topic);
        }
    }
    public static int getProducerNum()
    {
        return producerNum.get();
    }
}
