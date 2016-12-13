package model;

/**
 * Created by yang on 16-11-8.
 */

/**
 * broker的回应消息
 */
public enum ResponseType {
    SendResult, //broker回应给生产者的ACK
    Message, //消息 由broker发送给消费者的消息.
    AckSubscript  //消费者订阅主题后,broker
}
