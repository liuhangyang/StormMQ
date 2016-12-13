package model;

/**
 * Created by yang on 16-11-8.
 */

/**
 * 请求数据和回应数据的来源
 */
public enum RequestResponseFromType {
    Consumer,   //这个消息来自消费者.
    Broker, //这个消息broker.
    Produce  //此消息来自生产者.
}
