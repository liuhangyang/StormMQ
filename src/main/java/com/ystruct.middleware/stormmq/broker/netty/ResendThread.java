package broker.netty;

/**
 * Created by yang on 16-12-3.
 */

import broker.ClientChannelInfo;
import broker.ConsumerManager;
import broker.SendHelper;
import broker.TaskManager;
import model.*;
import smq.ConsumeResult;
import smq.ConsumeStatus;
import smq.Message;

import java.util.UUID;

/**
 *
 */
public class ResendThread implements Runnable{
   //发送的一个帮助类
    private SendHelper helper = new SendHelper();
    @Override
    public void run() {
        //需要知道消费方是否恢复消费,触发条件就是重发队列不增加.
        SendTask task = null;
        while((task = TaskManager.getResendTask())!=null){
            Message msg = task.getMessage();
            if(ConsumerManager.findGroupByGroupID(task.getGroupId()) == null){
               // System.out.println("组为空");
                TaskManager.pushResendTask(task); //放到重发线程去处理
                continue;
            }
            ClientChannelInfo channelInfo = ConsumerManager.findGroupByGroupID(task.getGroupId()).getNextChannelInfo();
            //发送消息
            StormResponse response = new StormResponse();
            response.setFromtype(RequestResponseFromType.Broker);
            response.setResponseType(ResponseType.Message);
            response.setResponse(msg);
            response.setRequestId(UUID.randomUUID().toString());
             //发送给consumer
            try {
                StormRequest request = (StormRequest)helper.brokerSend(channelInfo.getChannel(),response);
                ConsumeResult result = (ConsumeResult) request.getParameters();
                if(result.getStatus() != ConsumeStatus.SUCCESS){
                    //消费失败,
                  //  System.out.println("重发失败,又继续加入重发队列!");
                    TaskManager.pushResendTask(task);
                }else {
                    ConsumerManager.putConsumerGroupStat(task.getGroupId(),0);
                }
            }catch (Exception e){
                //发送超时,添加进入任务队尾
                TaskManager.pushResendTask(task);
            }

        }
        //减少当前重发线程数量
      //  System.out.println("重发线程数: "+ResendManager.resendThreadNumber);
        ResendManager.resendThreadNumber--;
    }
}
