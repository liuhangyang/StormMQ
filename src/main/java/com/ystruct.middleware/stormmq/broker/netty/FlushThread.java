package broker.netty;

import java.util.concurrent.TimeUnit;

/**
 * Created by yang on 16-12-3.
 */
public class FlushThread implements Runnable{
    @Override
    public void run() {
        while (true){
            try {
                long start = System.currentTimeMillis();
                //等待一段时间,是否收集齐一定的数量的消息
                if(!FlushTool.semp.tryAcquire(10000, TimeUnit.MILLISECONDS)){
                    System.out.println("--------------------------------------------");
                    synchronized (FlushTool.syncObj){
                        //把cacheList里面的数据都刷入磁盘
                        FlushTool.flush();
                    }
                    continue;
                }else {
                    long end = System.currentTimeMillis();
                  //  FlushTool.logWriter.log("collect all:use time"+(end -start)+" num: "+FlushTool.cacheList.size());
                    synchronized (FlushTool.syncObj){
                        //把cacheList里面的数据刷入磁盘
                        FlushTool.flush();
                    }
                }
            }catch (InterruptedException e){
                throw new RuntimeException();
            }
        }
    }
}
