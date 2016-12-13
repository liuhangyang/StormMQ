package broker.netty;

import broker.AckManager;
import broker.SemaphoreManager;
import file.MessageLog;
import smq.SendResult;
import smq.SendStatus;
import tool.LogWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * Created by yang on 16-11-26.
 */
public class FlushTool {
    public static MessageLog log = null;
    public static LogWriter logWriter = null;
    static {
        try {
            log = new MessageLog("message"); //初始化持久化文件的实例
            logWriter = logWriter.getLogWriter();
        }catch (IOException e){
            e.printStackTrace();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public static Object syncObj = new Object(); //用来刷磁盘的时候同步的

    //阻塞每个线程是否可以返回了
    public static Semaphore canReturn = new Semaphore(0);
    //一段时间收到的消息缓存在这里,由一个单独的线程来刷磁盘
    public static List<byte[]> cacheList = new ArrayList<byte[]>();
    //存储正在等待ack消息的requestID和messageID
    public static List<String> requestCacheList= new ArrayList<String>();
    public static Semaphore semp = null; //一个标识收到多少数据后就开始刷盘的信号量
    public static int threadNum = Conf.connNum;
    static {
        semp = new Semaphore(-threadNum);//与发送线程数量相同
    }
    public static void writeToCache(byte[] data,String requestId){
       // System.out.println("write in  cache");
        synchronized (cacheList){
            cacheList.add(data);
            semp.release();
        }
    }
    public static void writeToCache(List<byte[]> list,String requestId){

     //   System.out.println("write in cache");
        synchronized (cacheList){
            cacheList.addAll(list);
            requestCacheList.add(requestId);
            semp.release();//收到一个数据,释放一下,当释放足够多时,线程会刷盘
        }
    }

    /**
     * 把写在缓充区中中的数据先拷贝一份,然后清空缓冲,供其它线程进行写缓冲操作,
     * 用拷贝的备份进行刷盘操作.
     */
    public static void reset(){
        semp = new Semaphore(-threadNum);
    }
    //将缓冲区的数据写入硬盘,唤醒在等待刷盘操作的线程
    public static void flush(){
        List<byte[]> temp = null;
        List<String> requestTemp = null;
        synchronized (cacheList){
            temp = new ArrayList<byte[]>();
            requestTemp = new ArrayList<String>();
            temp.addAll(cacheList);
            requestTemp.addAll(requestCacheList);
            cacheList.clear();
            requestCacheList.clear();
            reset();
        }
        if(temp != null && temp.size() > 0) //刷已经写好的数据,此时其它线程可以把数据写入cacheList
        {
            long start = System.currentTimeMillis();
            boolean error = false;
            //同步把数据存储到磁盘
            if(!log.SynSave(temp))
                error = true;
            long end = System.currentTimeMillis();

         //  logWriter.log("save use time: "+(end- start)+ " number:"+temp.size()+" cachelist:"+cacheList.size());
            //存储结束后,把刷盘成功的消息,生成对应的ack消息,设置消息id
            for(int i = 0; i < requestTemp.size(); ++i){
                SendResult ack = new SendResult();
                String[] arr = requestTemp.get(i).split("@");
                ack.setMsgId(arr[1]); //message id
                ack.setInfo(arr[0]); //request id
                if(error)
                    ack.setStatus(SendStatus.FAIL);
                else
                    ack.setStatus(SendStatus.SUCCESS);

                AckManager.pushAck(ack); //往ack队列里面放一个ack消息
                SemaphoreManager.increase("Ack");

            }
        }
    }
    public static boolean writeConsumerResult(byte[] data){
       // System.out.println("写消费结果.");
        if(log != null)
            return  log.AsynSave(data);

        else
            return  false;
    }


}
