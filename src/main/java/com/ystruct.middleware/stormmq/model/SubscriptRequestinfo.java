package model;

/**
 * Created by yang on 16-11-8.
 */

/**
 *
 */
public class SubscriptRequestinfo {
    private String groupId; //消费者属于哪个消费组.
    private String topic; //消费者要订阅的主题.
    private String propertieName; //订阅的过滤属性名
    private String propertieValue; //订阅的过滤值
    private String clientKey; //客户端的id.

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

    public String getPropertieName() {
        return propertieName;
    }

    public void setPropertieName(String propertieName) {
        this.propertieName = propertieName;
    }

    public String getPropertieValue() {
        return propertieValue;
    }

    public void setPropertieValue(String propertieValue) {
        this.propertieValue = propertieValue;
    }

    public String getClientKey() {
        return clientKey;
    }

    public void setClientKey(String clientKey) {
        this.clientKey = clientKey;
    }
}
