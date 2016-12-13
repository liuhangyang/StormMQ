package model;

import smq.Message;

import java.io.Serializable;

/**
 * Created by yang on 16-11-26.
 */
public class SendTask implements Serializable {
    private String groupId; //组id
    private String topic; //主题
    private Message message;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        SendTask other= (SendTask)obj;
        if(groupId == null){
            if(other.getGroupId() != null)
                return false;
        }else {
            if(other.getGroupId() == null)
                return false;
            else if(!groupId.equals(other.getGroupId()))
                return false;
        }
        if(topic == null){
            if(other.getTopic() != null)
                return false;
        }else {
            if(other.getTopic() == null)
                return false;
            else if (!topic.equals(other.getTopic()))
                return false;
        }
        if(message == null){
            if(other.getMessage() != null)
                return false;
        }else{
            if(other.getMessage() == null)
                return false;
            else if(message.equals(other.getMessage()))
                return false;
        }
        return true;
    }

}
