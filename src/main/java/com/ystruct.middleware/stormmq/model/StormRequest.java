package model;

import java.io.Serializable;

/**
 * Created by yang on 16-11-8.
 */

/**
 * 生产者和消费者的一次请求的类.
 */
public class StormRequest implements Serializable {
    //一次请求的id
    private String requestId;
    //请求的参数
    private Object parameters;
    //消息是从哪里来的
    private RequestResponseFromType fromType;
    //请求的类型
    private RequestType requestType;

    public RequestType getRequestType() {
        return requestType;
    }
    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }
    public RequestResponseFromType getFromType() {
        return fromType;
    }
    public void setFromType(RequestResponseFromType fromType) {
        this.fromType = fromType;
    }
    public String getRequestId() {
        return requestId;
    }
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    public Object getParameters() {
        return parameters;
    }
    public void setParameters(Object parameters) {
        this.parameters = parameters;
    }
}
