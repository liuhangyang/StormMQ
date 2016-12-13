package smq;

import consumer.netty.ResponseCallbackListener;
import consumer.netty.StormConsumerConnection;
import consumer.netty.StormConsumerHandler;
import consumer.netty.StormConsumerNettyConnect;
import model.RequestType;
import model.StormRequest;
import model.StormResponse;
import model.SubscriptRequestinfo;

import java.net.ConnectException;
import java.security.MessageDigestSpi;
import java.util.UUID;

/**
 * Created by yang on 16-11-24.
 */
public class DefaultConsumer implements Consumer {
    private String brokerIp; //broker服务器ip地址
    private StormConsumerConnection consumerConn; //连接broker服务器的连接
    private String groupId; //消费组id
    private String topic; //订阅的主题
    private String propertieName; //过滤的属性名
    private String propertieValue; //过滤的值
    private MessageListener listener; //监听器
    private boolean isFilter = false; //是否有属性过滤
    private boolean isSubscript = false; //是否输入订阅信息
    private boolean isRunning = false;
    private String clientKey = "";
    public DefaultConsumer(){
        brokerIp = "127.0.0.1";
        consumerConn = new StormConsumerNettyConnect(brokerIp,8888);
    }

    @Override
    public void start() {
        if(isSubscript){
            //设置处理器和结果回调
            System.out.println("start....");
            consumerConn.sethandle(new StormConsumerHandler(consumerConn, new ResponseCallbackListener() {
                @Override
                public Object onResponse(Object response) {
                    StormResponse mr = (StormResponse)response;
                   // System.out.println("DefaultConsumer::onResponse");
                    //调用上层设置的回调函数
                    ConsumeResult result = listener.onMessage((Message)mr.getResponse());
                    result.setGroupID(groupId);
                    result.setTopic(topic);
                    result.setMsgId(((Message) mr.getResponse()).getMsgId());
                    return result;
                }

                @Override
                public void onTimeout() {

                }

                @Override
                public void onException(Throwable e) {
                    if(e instanceof java.net.ConnectException){
                        System.out.println("connect error");
                        if(isRunning)
                            restartConnect();
                    }
                }

                @Override
                public void onDisconnect(String msg) {
                    System.out.println("onDisconnect");
                        if(isRunning){
                            restartConnect();
                        }
                }
            }));
            //连接服务器
           // System.out.println("connect before");
            consumerConn.connect();
           // System.out.println("connect after");
            clientKey = groupId + topic+ UUID.randomUUID().toString();
            //发送一个自己的订阅信息给服务器
            isRunning = true;
            StormRequest request = new StormRequest();
            request.setRequestType(RequestType.Subscript);
            request.setRequestId(UUID.randomUUID().toString());

            //构造订阅信息给broker
            SubscriptRequestinfo subscript = new SubscriptRequestinfo();
            subscript.setGroupId(groupId);
            subscript.setTopic(topic);
            subscript.setPropertieName(propertieName);
            subscript.setPropertieValue(propertieValue);
            subscript.setClientKey(clientKey);
            request.setParameters(subscript);
            consumerConn.Send(request);

        }
    }
    public void restartConnect(){
        boolean isConnected = false;
        System.out.println("restart");
        brokerIp = "127.0.0.1";
        consumerConn = new StormConsumerNettyConnect(brokerIp,8888);
        //重新设置处理器
        //设置处理器和结果回调函数
        consumerConn.sethandle(new StormConsumerHandler(consumerConn, new ResponseCallbackListener() {
            @Override
            public Object onResponse(Object response) {
                StormResponse mr = (StormResponse)response;
                //调用上层设置的回调函数
                ConsumeResult result = listener.onMessage((Message)mr.getResponse());
                result.setGroupID(groupId);
                result.setTopic(topic);
                result.setMsgId(((Message) mr.getResponse()).getMsgId());
                return  result;
            }

            @Override
            public void onTimeout() {

            }

            @Override
            public void onException(Throwable e) {
               // System.out.println(" error");
                if(e instanceof ConnectException){
                    System.out.println("connect error");
                    if(isRunning)
                        restartConnect();
                }
            }

            @Override
            public void onDisconnect(String msg) {
                System.out.println("restart:onDisconnect");
                    if(isRunning)
                        restartConnect();
            }
        }));
        //连接服务器
        try {
            consumerConn.connect();
            isConnected = true;
        }catch (Exception e){
            try {
                Thread.sleep(3000);
            }catch(InterruptedException ie){

            }
            isConnected =false;
            restartConnect();
        }
        if(isConnected)
        {
            StormRequest request=new StormRequest();
            request.setRequestType(RequestType.Subscript);
            request.setRequestId(UUID.randomUUID().toString());

            //构造订阅信息发送给
            SubscriptRequestinfo subscript=new SubscriptRequestinfo();
            subscript.setGroupId(groupId);
            subscript.setTopic(topic);
            subscript.setPropertieName(propertieName);
            subscript.setPropertieValue(propertieValue);
            subscript.setClientKey(clientKey);
            request.setParameters(subscript);
            consumerConn.Send(request);
        }
    }

    @Override
    public void subscribe(String topic, String filter, MessageListener listener) {
            this.topic = topic;
        if(filter.trim().length() > 0&&filter.contains("=")){
            this.propertieName = filter.split("=")[0];
            this.propertieValue =filter.split("=")[1];
            this.isFilter = true;
        }
        this.listener = listener; //设置监听
        isSubscript = true;
    }

    @Override
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    @Override
    public void stop() {
        if(isRunning){
            isRunning = false;
            //发送一个退订消息,把自己从订阅关系里面移除
            StormRequest request = new StormRequest();
            request.setRequestType(RequestType.Stop);
            request.setRequestId(UUID.randomUUID().toString());
            request.setParameters(new String(clientKey));
            consumerConn.SendSync(request);
            consumerConn.close();
        }
    }
}
