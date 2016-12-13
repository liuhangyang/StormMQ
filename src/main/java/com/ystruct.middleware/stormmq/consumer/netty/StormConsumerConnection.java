package consumer.netty;

import io.netty.channel.ChannelInboundHandlerAdapter;
import model.InvokeFuture;
import model.StormRequest;

/**
 * Created by yang on 16-11-24.
 */
public interface StormConsumerConnection {
    void init();
    void connect();
    void connect(String host,int port);
    void sethandle(ChannelInboundHandlerAdapter hanler);
    Object Send(StormRequest request);
    void SendSync(StormRequest request);
    void close();
    boolean isConnected();
    boolean isClosed();
    public boolean ContainsFuture(String key);
    public InvokeFuture<Object> removeFuture(String key);
    public void setTimeOut(long timeOut);
}

