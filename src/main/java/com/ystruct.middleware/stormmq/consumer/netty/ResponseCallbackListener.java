package consumer.netty;

/**
 * Created by yang on 16-11-24.
 */
public interface ResponseCallbackListener {
    Object onResponse(Object response);
    void onTimeout();
    void onException(Throwable e);
    void onDisconnect(String msg);
}
