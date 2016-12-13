package model;

/**
 * Created by yang on 16-11-8.
 */

/**
 * 生产者或者消费者的请求类型.
 */

public enum RequestType
{
    Message,//smq.Producer 发送消息
    ConsumeResult,//QueueConsumer 消费消息的结果
    Subscript,//QueueConsumer 订阅消息的请求
    Stop,//退订消息
}
