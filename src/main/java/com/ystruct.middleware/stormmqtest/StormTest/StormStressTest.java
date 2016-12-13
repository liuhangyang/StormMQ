package StormTest;


import smq.*;


import java.nio.charset.Charset;
import java.util.Properties;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by yang on 16-12-11.
 */
public class StormStressTest {
    private static String TOPIC = "StormMQ"; //主题
    private static String PID = "PID_"; //生产者的组id
    private static String CID = "CID_"; //消费者的组id
    private static String BODY = "hello Storm";
    private static String AREA = "area_";
    private static Charset  character = Charset.forName("utf-8");
    private static Random random = new Random();
    private static AtomicLong sendCount = new AtomicLong();//发送的总数
    private static AtomicLong recvCount = new AtomicLong();//接收的总数
    private static AtomicLong totalRT = new AtomicLong(); //总的rt;
    private static  AtomicLong totalDelay = new AtomicLong(); //总的时延
    private static int c = Integer.valueOf(System.getProperty("C","200"));
    private static ExecutorService executorService = Executors.newFixedThreadPool(c);

    private static TestResult testResult = new TestResult();

    public static void main(String[] args) {
        testBasic();
        //testFilter();
        if (!testResult.isSuccess()) {
            System.out.println(testResult);
            FileIO.write(testResult.toString());
            Runtime.getRuntime().exit(0);
        }
        System.out.println(testResult);
        FileIO.write(testResult.toString());
        Runtime.getRuntime().exit(0);
    }
    private static void testBasic(){
        final int code = random.nextInt(100000);
        final ConsumeResult consumeResult = new ConsumeResult();
        consumeResult.setStatus(ConsumeStatus.SUCCESS);
        final String topic = TOPIC+code;
        try {
            Consumer consumer = new DefaultConsumer();
            consumer.setGroupId(CID+code);
            consumer.subscribe(topic, " ", new MessageListener() {
                @Override
                public ConsumeResult onMessage(Message message) {
                    if (!message.getTopic().equals(topic)) {
                        testResult.setSuccess(false);
                        testResult.setInfo("expect topic:"+topic+", actual topic:"+message.getTopic());
                    }
                    long delay = System.currentTimeMillis() - message.getBornTime();
                  /*  if (delay>1000) {
						testResult.setSuccess(false);
						testResult.setInfo("msg "+message.getMsgId()+" delay "+(System.currentTimeMillis()-message.getBornTime())+" ms");
					}*/
                    totalDelay.addAndGet(delay);
                    recvCount.incrementAndGet();
                    return  consumeResult;
                }
            });
            consumer.start();
            final Producer producer = new DefaultProducer();
            producer.setGroupId(PID+code);
            producer.setTopic(topic);
            producer.start();
            long start = System.currentTimeMillis();
            for(int i = 0 ;i < c; ++i){
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        while(true){
                            try {
                                Message msg = new Message();
                                msg.setBody(BODY.getBytes(character));
                                msg.setProperty("area","hz"+code);
                                final long startRt = System.currentTimeMillis();
                                SendResult result = producer.sendMessage(msg);
                                if(result.getStatus() == SendStatus.SUCCESS){
                                    sendCount.incrementAndGet();
                                    totalRT.addAndGet(System.currentTimeMillis() - startRt);
                                }
                              /*  producer.asyncSendMessage(msg, new SendCallback() {

                                    @Override
                                    public void onResult(SendResult result) {
                                        if (result.getStatus()==SendStatus.SUCCESS) {
                                            sendCount.incrementAndGet();
                                            totalRT.addAndGet(System.currentTimeMillis()-startRt);
                                        }
                                    }
                                });*/

                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
            Thread.sleep(40000);
            if(!testResult.isSuccess()){
                return;
            }
            long totalTime = System.currentTimeMillis()-start;
            System.out.println("totalTime:"+totalTime+"SendCount:"+sendCount+"recvCount:"+sendCount);
            long sendTps = sendCount.get()*1000/totalTime;
            long recvTps = recvCount.get()*1000 /totalTime;
            long rt = totalRT.get() / sendCount.get();
            long delay = totalDelay.get() / recvCount.get();
            testResult.setInfo("send tps:"+sendTps+", recv tps:"+recvTps+", send rt:"+rt+", avg delay:"+delay);
        }catch (Exception e){
            testResult.setSuccess(false);
            testResult.setInfo(e.getMessage());
        }
    }
    private static void testFilter(){
        int code = random.nextInt(100000);
        final ConsumeResult consumeResult = new ConsumeResult();
        consumeResult.setStatus(ConsumeStatus.SUCCESS);
        final String topic = TOPIC+code;
        final String k = AREA+code;
        final String v ="hz_"+code;
        try {
            Consumer consumer = new DefaultConsumer();
            consumer.setGroupId(CID+code);
            consumer.subscribe(topic, k+"="+v, new MessageListener() {
                @Override
                public ConsumeResult onMessage(Message paramMessage) {
                    if(!paramMessage.getTopic().equals(topic)){
                        testResult.setSuccess(false);
                        testResult.setInfo("expect topic:"+topic+",actual topic:"+paramMessage.getTopic());
                    }
                    if(System.currentTimeMillis()-paramMessage.getBornTime() > 1000){
                        testResult.setSuccess(false);
                        testResult.setInfo("msg "+paramMessage.getMsgId()+" delay "+(System.currentTimeMillis()-paramMessage.getBornTime())+" ms");
                    }
                    if(!paramMessage.getProperty(k).equals(v)){
                        testResult.setSuccess(false);
                        testResult.setInfo("msg "+paramMessage.getMsgId()+" expect k"+k+"  value is "+ v+", but actual value is "+paramMessage.getProperty(k));
                    }
                    return consumeResult;
                }
            });
            consumer.start();
            Producer producer = new DefaultProducer();
            producer.setGroupId(PID+code);
            producer.setTopic(topic);
            producer.start();
            Message message = new Message();
            message.setBody(BODY.getBytes(character));
            message.setProperty(k,v);
            SendResult result = producer.sendMessage(message);
            if(result.getStatus() != SendStatus.SUCCESS){
                testResult.setSuccess(false);
                testResult.setInfo(result.toString());
                return;
            }
            message=new Message();
            message.setBody(BODY.getBytes(character));
            result=producer.sendMessage(message);
            if (result.getStatus()!=SendStatus.SUCCESS) {
                testResult.setSuccess(false);
                testResult.setInfo(result.toString());
                return;
            }
            Thread.sleep(5000);
            if (!testResult.isSuccess()) {
                return ;
            }

        }catch (Exception e){
            testResult.setSuccess(false);
            testResult.setInfo(e.getMessage());
        }
    }
    private static void checkMsg(Queue<String> sendMsg,Queue<String> recvMsg){
        if(sendMsg.size() > recvMsg.size()){
            testResult.setSuccess(false);
            testResult.setInfo("send msg num is "+sendMsg.size()+",but recv msg num is "+recvMsg.size());
            return;
        }
        if((recvMsg.size() - sendMsg.size()) / sendMsg.size() > 0.001){
            testResult.setSuccess(false);
            testResult.setInfo("repeat rate too big "+(recvMsg.size() - sendMsg.size())/sendMsg.size());
            return;
        }

        for (String send : sendMsg) {
            boolean find=false;
            for (String recv : recvMsg) {
                if (recv.equals(send)) {
                    find=true;
                    break;
                }
            }
            if (!find) {
                testResult.setSuccess(false);
                testResult.setInfo("msg "+send+" is miss");
                return ;
            }
        }
    }
}
