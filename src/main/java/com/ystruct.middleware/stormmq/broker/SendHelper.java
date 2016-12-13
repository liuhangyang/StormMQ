package broker;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import model.InvokeFuture;
import model.RequestResponseFromType;
import model.ResponseType;
import model.StormResponse;
import smq.Message;
import tool.QueueFile;
import tool.Tool;

import java.io.IOException;
import java.util.Map;
import java.util.Observable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by yang on 16-12-2.
 */
public class SendHelper {
    private long timeout = 3000; //默认超时时间
    public static volatile Map<String, InvokeFuture<Object>> futures = new ConcurrentHashMap<String, InvokeFuture<Object>>();
    //用于发送队列的信息到对应的consumer
    public static void sendMessageByKey(String key){
        QueueFile queue =  QueueManager.findQueue(key);
     //   System.out.println("SendHelper:queue.length:"+queue);
        try {
            //取出下一个发送的消息
            if(queue.size() > 0){
                Message pullmsg = (Message) Tool.deserialize(queue.peek(),Message.class);
                StormResponse response = new StormResponse();
                response.setFromtype(RequestResponseFromType.Broker);
                response.setResponseType(ResponseType.Message);
                response.setResponse(pullmsg);

                //找到对应的组,取得下一个要发送的channel
                if(QueueManager.getGroup(key)!=null){
                    ClientChannelInfo channelInfo = ConsumerManager.findGroupByGroupID(QueueManager.getGroup(key)).getNextChannelInfo();
                    //发送消息
                    channelInfo.getChannel().writeAndFlush(response);
                    //记录下来这个队列的发送时间
                   QueueManager.recordTime(key,System.currentTimeMillis());
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    public Object brokerSend(final Channel channel, StormResponse response){
        if(channel != null){
            final InvokeFuture<Object> future = new InvokeFuture<Object>();
            futures.put(response.getRequestId(),future);
            //设置这次的请求ID
            future.setRequestId(response.getRequestId());
            ChannelFuture cfuture = channel.writeAndFlush(response);
            cfuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if(!channelFuture.isSuccess()){
                        future.setCause(channelFuture.cause());
                    }
                }
            });
            try{
                Object result = future.getResult(timeout, TimeUnit.MILLISECONDS);
                return  result;
            }catch (RuntimeException e){
                throw e;
            }finally {
                //这个结果已经收到
                futures.remove(response.getRequestId());
            }
        }else{
            return null;
        }
    }

public static boolean containsFuture(String key) {
    return futures.containsKey(key);
}

    public static InvokeFuture<Object> removeFuture(String key) {
        if(futures.containsKey(key)) {
            return futures.remove(key);
        }
        else
            return null;
    }
}