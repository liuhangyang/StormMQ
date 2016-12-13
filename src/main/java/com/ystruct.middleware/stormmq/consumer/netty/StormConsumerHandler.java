package consumer.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import model.*;
import smq.ConsumeResult;

/**
 * Created by yang on 16-11-24.
 */
public class StormConsumerHandler extends ChannelInboundHandlerAdapter {
    private StormConsumerConnection connect;
    private Throwable cause;
    private ResponseCallbackListener listener;
    StormConsumerHandler(){
    }
    public StormConsumerHandler(StormConsumerConnection conn,ResponseCallbackListener listener){
        this.connect = conn;
        this.listener = listener;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      //  System.out.println("收到消息");
        StormResponse response = (StormResponse)msg;

        String key = response.getRequestId();
        if(connect.ContainsFuture(key)){
            InvokeFuture<Object> future = connect.removeFuture(key);
            //没有找到对应的发送请求,则返回
            if(future == null){
                return;
            }
            if(this.cause != null){
                //设置异常结果,会触发里面的回调函数
               // System.out.println("StormConr  sumerHandler:::-->cause:"+cause);
                future.setCause(cause);
                if(listener != null)
                    listener.onException(cause);
            }else {
                future.setResult(response);
            }
        }else {
            //如果不是consumer主动发送的数据,则说明是服务器主动发送的消息,则调用消息收到
            if(listener != null){
                ConsumeResult result = (ConsumeResult)listener.onResponse(response);
                //回答consumer的消费情况,相当于服务器调用rpc
                StormRequest request = new StormRequest();
                request.setRequestId(response.getRequestId());
                request.setFromType(RequestResponseFromType.Consumer);
                request.setRequestType(RequestType.ConsumeResult);
                request.setParameters(result);

                ctx.writeAndFlush(request);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        //super.exceptionCaught(ctx, cause);
        this.cause=cause;
        System.out.println("StormHandler caught exception");
        if(listener!=null)
            listener.onException(cause);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //super.channelInactive(ctx);
        if(listener != null){
            listener.onDisconnect(ctx.channel().remoteAddress().toString());
        }
    }
}
