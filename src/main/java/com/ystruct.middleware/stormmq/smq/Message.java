package smq;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by yang on 16-11-22.
 */
public class Message implements Serializable{
    //private static final long serialVersionUID = 5295808332504208830L;
    private String topic;
    private byte[] body;
    private String msgId;
    private long bornTime;
    private Map<String,String> properties = new HashMap<String, String>();

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public long getBornTime() {
        return bornTime;
    }

    public void setBornTime(long bornTime) {
        this.bornTime = bornTime;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
    public String getProperty(String key){
        return (String)this.properties.get(key);
    }
    public void setProperty(String key,String value){
        this.properties.put(key,value);
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return  false;
        if(getClass() != obj.getClass())
            return false;
        Message other=(Message)obj;
        if(topic == null){
            if(other.getTopic() != null)
                return false;
        }else{
            if(other.getTopic() == null)
                return  false;
            else if(!other.getTopic().equals(topic))
                return  false;
        }
        if(body == null){
            if(other.getBody() != null)
                return false;
        }else{
            if(other.getBody() == null)
                return  false;
            if(body.length != other.getBody().length)
                return  false;
            else
                for(int i = 0;i < body.length; ++i){
                    if(body[i] != other.getBody()[i])
                        return false;
                }
        }

        if(properties==null)
        {
            if(other.getProperties()!=null)
                return false;
        }
        else
        {
            if(other.getProperties()==null)
                return false;
            else if(!properties.equals(other.getProperties()))
                return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = topic.hashCode();
        result += properties.hashCode();
        if(body != null)
            for(int i = 0; i < body.length; ++i){
                result += body[i];
            }
        return  result;
    }
}
