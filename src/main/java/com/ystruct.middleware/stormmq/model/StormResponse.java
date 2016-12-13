package model;

import java.io.Serializable;

/**
 * Created by yang on 16-11-8.
 */

/**
 * broker回应生产者或者
 */
public class StormResponse implements Serializable{
    private String requestId; //对应的回应的是哪个请求
    private Object response;// 回应的消息
    private RequestResponseFromType fromtype; //消息来自哪里
    private ResponseType responseType; //响应的类型

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Object getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = response;
    }

    public RequestResponseFromType getFromtype() {
        return fromtype;
    }

    public void setFromtype(RequestResponseFromType fromtype) {
        this.fromtype = fromtype;
    }

    public ResponseType getResponseType() {
        return responseType;
    }

    public void setResponseType(ResponseType responseType) {
        this.responseType = responseType;
    }
}
