package broker.netty;

import broker.AckManager;
import broker.SemaphoreManager;
import io.netty.channel.Channel;
import model.RequestResponseFromType;
import model.ResponseType;
import model.StormResponse;
import smq.SendResult;

/**
 * Created by yang on 16-12-1.
 */
public class AckSendThread implements Runnable {
    @Override
    public void run() {
        while (true){
        //    System.out.println("得到一个ack");
            SemaphoreManager.descrease("Ack");//获取一个ACK的信号量
           // System.out.println("得到一个ack1");
            //获取ack消息
            SendResult ack = AckManager.getAck();
            //获取requestId
            String requested = ack.getInfo();
            ack.setInfo(null);

            //根据requestID找到那个对应的channel来发送数据
            Channel channel = AckManager.findChannel(requested);
            if(channel != null && channel.isActive() && channel.isOpen() && channel.isWritable()){
                StormResponse response = new StormResponse();
                response.setRequestId(requested);
                response.setFromtype(RequestResponseFromType.Broker);
                response.setResponseType(ResponseType.SendResult);
                response.setResponse(ack);
                channel.writeAndFlush(response); //发送ack到生产者
            }
        }
    }
}
