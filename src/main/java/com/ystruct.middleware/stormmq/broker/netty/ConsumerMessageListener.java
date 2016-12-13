package broker.netty;

import broker.ClientChannelInfo;
import broker.ConsumerManager;
import broker.SubscriptionInfo;
import broker.TaskManager;
import file.LogTask;
import model.SendTask;
import model.SubscriptRequestinfo;
import smq.ConsumeResult;
import smq.ConsumeStatus;
import smq.Message;
import tool.Tool;

/**
 * Created by yang on 16-11-25.
 */
public class ConsumerMessageListener extends MessageListener {
    @Override
    void onConsumerResultReceived(ConsumeResult msg) {
        if(msg.getStatus() == ConsumeStatus.SUCCESS) { //消费成功的话,找到对应的队列删除那个消息,然后继续发送下一个消息.
            if(TaskManager.findInResend(msg.getGroupID(),msg.getTopic(),msg.getMsgId())){
                //在重发队列中找到了对应的消息,说明此条消息已经已经无效.
               // System.out.println("无效－－－－－－－－－－－－－－－－－－>");
                return;
            }
            //记录一个发送成功的日志


            SendTask task = new SendTask();
            task.setGroupId(msg.getGroupID());
            task.setTopic(msg.getTopic());
            Message message = new Message();
            message.setMsgId(msg.getMsgId());
            task.setMessage(message);
            LogTask logTask = new LogTask(task,1);
            byte[] data = Tool.serialize(logTask);
            //消费情况直接写入文件.
            //System.out.println("data.length:"+data.length);
            FlushTool.writeConsumerResult(data);
        }
    }
    //收到订阅消息后的操作

    @Override
    void onConsumerSubcriptReceived(SubscriptRequestinfo msg, ClientChannelInfo channel) {
      //  System.out.println("receive subcript info groupid:"+msg.getGroupId()+"topic: "+msg.getTopic()+" filterName:"+msg.getPropertieName()+" filterValue:"+msg.getPropertieValue()+" clientId:"+channel.getClientId());
        SubscriptionInfo subscriptionInfo = new SubscriptionInfo();
        subscriptionInfo.setTopic(msg.getTopic());
        subscriptionInfo.setFitlerName(msg.getPropertieName());
        subscriptionInfo.setFitlerValue(msg.getPropertieValue());
        channel.setSubcript(subscriptionInfo); //设置订阅信息

        /**
         * 加入某个组
         * 相同的组和相同的topic,更新订阅条件就好
         * 如果组不存在,则创建该组.
         */
      //  System.out.println("加入组之前");
        ConsumerManager.addGroupInfo(msg.getGroupId(),channel);
       // System.out.println("加入组成功");
    }
}
