package smq;



/**
 * Created by yang on 16-11-24.
 */
public abstract interface Consumer {
    public abstract void  start();
    public abstract void subscribe(String paramString1, String paramString2, MessageListener listener);
    public abstract void setGroupId(String paramString);
    public abstract void stop();
}
