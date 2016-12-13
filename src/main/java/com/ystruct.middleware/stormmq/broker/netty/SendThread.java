package broker.netty;

import broker.*;
import model.RequestResponseFromType;
import model.SendTask;
import model.StormRequest;
import model.StormResponse;
import smq.ConsumeResult;
import smq.ConsumeStatus;
import smq.Message;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by yang on 16-12-2.
 */

/**
 * broker 给consumer发送数据的线程
 * 每次从任务队列获取一个任务,然后根据任务的信息,找到要发送的consumer
 * 同步发送,如果发送失败或超时则加入任务队列,继续下面的数据发送.
 */
public class SendThread implements Runnable {
    //总的发送数量
    public static AtomicLong sendToal = new AtomicLong(0L);
    //发送的一个帮助类
    private SendHelper helper = new SendHelper();

    @Override
    public void run() {
        while(true){
            SemaphoreManager.descrease("SendTask");
            SendTask task = TaskManager.getTask();
            Message msg = task.getMessage();
            //负载均衡
            if(ConsumerManager.findGroupByGroupID(task.getGroupId()) == null){
              //  System.out.println("放入重发");
                TaskManager.pushResendTask(task); //放到重发线程去处理
                continue;
            }
            ClientChannelInfo channelInfo = ConsumerManager.findGroupByGroupID(task.getGroupId()).getNextChannelInfo();
           // System.out.println("得到下一个成功");
            //发送消息
            StormResponse response = new StormResponse();
            response.setFromtype(RequestResponseFromType.Broker);
            response.setResponse(msg);
            response.setRequestId(UUID.randomUUID().toString());
            //发送给consumer

            try {
                //当取得的下一个要消费的消费者已经关闭时,加入重发队列
                /*try {
                    Thread.sleep(10000);
                }catch (InterruptedException e){

                }*/
                /**
                 *这里的判断是为了防止在获得下一个发送的客户端是channel可用,但当发送时不可用.那就加入重发队列.
                 */
                if(!channelInfo.getChannel().isActive() || !channelInfo.getChannel().isOpen()||!channelInfo.getChannel().isWritable()){
                    System.out.println("这里的判断是为了防止在获得下一个发送的客户端是channel可用,但当发送时不可用.那就加入重发队列.");
                    TaskManager.pushResendTask(task);
                    ConsumerManager.putConsumerGroupStat(task.getGroupId(),-1);
                    continue;
                }
                //如果发送的对象是一个超时对象就跳过它,由重发线程试着来发送这些数据
                if(ConsumerManager.getConsumerGroupStat(task.getGroupId()) != null && ConsumerManager.getConsumerGroupStat(task.getGroupId()) == 1){
                    System.out.println("如果发送的对象是一个超时对象就跳过它,由重发线程试着来发送这些数据");
                    TaskManager.pushResendTask(task); //放到重发线程去处理

                    continue;
                }
                long start = System.currentTimeMillis();
                //发送一个消息给消费者,默认超时时间是3秒,如果超过3秒,则由重发线程去
                StormRequest request = (StormRequest)helper.brokerSend(channelInfo.getChannel(),response);
                long end = System.currentTimeMillis();
                ConsumeResult result = (ConsumeResult)request.getParameters();
                if(result.getStatus() != ConsumeStatus.SUCCESS){
                    //消费失败
                   System.out.println("消费失败后加入重发队列");
                    TaskManager.pushResendTask(task);
                }else {
                    //设置状态为正常
                   // System.out.println("发送成功记录日志");
                    ConsumerManager.putConsumerGroupStat(task.getGroupId(),0);
                    sendToal.incrementAndGet();//增加一个已发送
                //    FlushTool.logWriter.log("sendThread:"+Thread.currentThread().getId()+" send msg id="+msg.getMsgId()+" groupid="+task.getGroupId()+" success! use time:"+(end-start));
                }
            }catch (Exception e){
                //发送超时，添加进任务队尾
                TaskManager.pushResendTask(task);
                //设置状态为1
                ConsumerManager.putConsumerGroupStat(task.getGroupId(),1);
                FlushTool.logWriter.log("send time out:"+task.getMessage().getMsgId());
            }
        }
    }
}
