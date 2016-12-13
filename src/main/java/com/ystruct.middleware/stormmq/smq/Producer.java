package smq;


/**
 * Created by yang on 16-11-22.
 */
public abstract interface Producer {
    public abstract void start();
    public abstract void  setTopic(String paramString);
    public abstract void setGroupId(String paramString);
    public abstract SendResult sendMessage(Message paramMessage);
    public abstract void asyncSendMessage(Message paramMessage, SendCallback paramSendCallback);
    public abstract void stop();
}
