package broker;

/**
 * Created by yang on 16-11-24.
 */

/**
 * consumer的订阅信息
 */
public class SubscriptionInfo {
    private String topic; //主题
    private String subString; //
    private String fitlerName; //属性过滤名
    private String fitlerValue; //属性过滤值


    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getSubString() {
        return subString;
    }

    public void setSubString(String subString) {
        this.subString = subString;
    }

    public String getFitlerName() {
        return fitlerName;
    }

    public void setFitlerName(String fitlerName) {
        this.fitlerName = fitlerName;
    }

    public String getFitlerValue() {
        return fitlerValue;
    }

    public void setFitlerValue(String fitlerValue) {
        this.fitlerValue = fitlerValue;
    }
}
