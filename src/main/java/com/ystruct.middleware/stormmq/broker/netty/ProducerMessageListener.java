package broker.netty;


import broker.*;
import com.sun.javafx.tk.Toolkit;
import file.LogTask;
import io.netty.channel.Channel;
import model.SendTask;
import smq.Message;
import smq.SendResult;
import smq.SendStatus;
import tool.MemoryTool;
import tool.Tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by yang on 16-11-24.
 */
//broker收到producer的消息的时候的监听类
public class  ProducerMessageListener extends MessageListener {
    @Override
    void onProducerMessageReceived(Message msg, String requestId, Channel channel) {
        //把一个请求对应的requestId对应的channel放入队列.
        AckManager.pushRequest(requestId,channel);
        boolean isError = false;
        String mapstr = "";
        for(Map.Entry<String,String> entry:msg.getProperties().entrySet()){
            mapstr+=entry.getKey()+"="+entry.getValue();
        }
       // System.out.println("filterName:"+mapstr);
        //System.out.println("receive producer message msgid:"+msg.getMsgId()+" topic:"+msg.getTopic()+" filter:"+mapstr);
        String topic = msg.getTopic();

        //找到订阅这个消息的所有组
        List<ConsumerGroupInfo> allgroups = ConsumerManager.findGroupByTopic(topic);
       // System.out.println("订阅所有相同topic的主题的组:"+allgroups.size());
        //找到所有符合过滤消息的组
        List<ConsumerGroupInfo> groups = new ArrayList<ConsumerGroupInfo>();
        for(ConsumerGroupInfo groupInfo :allgroups){
            String filterName = groupInfo.findSubscriptionData(topic).getFitlerName();
            String filterValue = groupInfo.findSubscriptionData(topic).getFitlerValue();
            if(filterName == null){
                groups.add(groupInfo);
            }else {
                //找到符合过滤消息的组
                String value = msg.getProperty(filterName);
                if(value != null && msg.getProperty(filterName).equals(filterValue)){
                    groups.add(groupInfo);
                }
            }
        }
        //说明没有找打订阅这个消息的组,需要把消息存储在默认队列里面.
        if(groups.isEmpty()){
            //查找有没有专属于存储这个topic的队列
            System.out.println("don't have match group");

            //返回这个ack
            SendResult ack = new SendResult();
            ack.setMsgId(msg.getMsgId());  //message id
            ack.setInfo(requestId);
            ack.setStatus(SendStatus.SUCCESS);
            AckManager.pushAck(ack);
            SemaphoreManager.increase("Ack");
        }
        //遍历所有的订阅该消息的组
        List<byte[]> logList = new ArrayList<byte[]>();
        List<SendTask> taskList = new ArrayList<SendTask>();
        for(ConsumerGroupInfo consumerGroupInfo:groups){
            SendTask task = new SendTask();
            task.setGroupId(consumerGroupInfo.getGroupId());
            task.setTopic(topic);
            task.setMessage(msg);

            taskList.add(task);
            LogTask log = new LogTask(task,0);
            byte[] data = Tool.serialize(log);
            logList.add(data);
        }
        try {
            //生成一个requestID和messageID组成的键值
            String key = requestId+"@"+msg.getMsgId();
           // System.out.println("key:"+key);
            //先把这些任务写到缓冲区,这时候ack消息还没有生成,producer还是等待ack消息
            //等到一定的时机生成ACK消息加入ack消息队列等待ack发送线程发送ack消息
           // System.out.println("logList.size:"+logList.size());
            FlushTool.writeToCache(logList,key);

        }catch (Exception e){
            e.printStackTrace();
        }
        //添加一个发送任务
        if(MemoryTool.moreThan(1024*1024*1024*8)) //100MB可用内存
        {
            TaskManager.pushTask(taskList); //把这些任务放到内存中的任务队列
           // System.out.println("taskList.size:"+taskList.size());
            for(int i = 0; i < taskList.size();++i){
                //System.out.println("SendTask");
                SemaphoreManager.increase("SendTask");
            }
        }else {
            //不添加到发送队列了.
        }











    }

}
