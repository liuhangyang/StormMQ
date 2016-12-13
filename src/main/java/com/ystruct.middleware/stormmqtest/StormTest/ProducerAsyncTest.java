package StormTest;

import smq.*;

/**
 * Created by yang on 16-12-13.
 */
public class ProducerAsyncTest {
    public static void main(String[] args) {
        Producer producer=new DefaultProducer();
        producer.setGroupId("PG-test");
        producer.setTopic("T-test");
        producer.start();
        Message message=new Message();
        message.setBody("Hello MOM".getBytes());
        message.setProperty("area", "us");
        //调用此方法，当前线程不阻塞
        producer.asyncSendMessage(message, new SendCallback() {

            @Override
            public void onResult(SendResult result) {
                if (result.getStatus().equals(SendStatus.SUCCESS)) {
                    System.out.println("send success:"+result.getMsgId());
                }
            }
        });

    }

}
