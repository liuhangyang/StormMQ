package broker;

import io.netty.channel.Channel;
import smq.SendResult;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by yang on 16-11-24.
 */
public class AckManager {
    //ConcurrentLinkedQueue是一个非阻塞的线程安全的队列,注意不要使用ConcurrentLinkQueue的size()方法,此方法会遍历所有的元素
    private static ConcurrentLinkedQueue<SendResult> ackQueue = new ConcurrentLinkedQueue<SendResult>();
    private static ConcurrentHashMap<String/*requestId*/,Channel> producerMap = new ConcurrentHashMap<String, Channel>();

    public static void pushRequest(String requestId,Channel channel){
        producerMap.put(requestId,channel);
    }
    //查找到这个message发送来的channel,并从这里面删除它
    public static Channel findChannel(String requestId){
        Channel channel = producerMap.remove(requestId);
        return  channel;
    }
    public static boolean pushAck(SendResult ack){
       // System.out.println("pushAck");
        return ackQueue.offer(ack);
    }
    public static  boolean pushAck(List<SendResult> acks){
        boolean flag = false;
        for(SendResult ack:acks){
            flag = ackQueue.offer(ack);
        }
        return  flag;
    }
    public static SendResult getAck(){
        return ackQueue.poll();
    }
}
