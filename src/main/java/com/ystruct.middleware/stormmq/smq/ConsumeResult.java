package smq;

/**
 * Created by yang on 16-11-24.
 */
public class ConsumeResult {
    private ConsumeStatus status = ConsumeStatus.FAIL;
    private String info;
    private String groupID;
    private String topic;
    private String msgId;

    public ConsumeStatus getStatus() {
        return status;
    }

    public void setStatus(ConsumeStatus status) {
        this.status = status;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getGroupID() {
        return groupID;
    }

    public void setGroupID(String groupID) {
        this.groupID = groupID;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }
}
