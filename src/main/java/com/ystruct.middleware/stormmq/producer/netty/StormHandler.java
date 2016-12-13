package producer.netty;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ConnectTimeoutException;
import model.InvokeFuture;
import model.StormResponse;

/**
 * Created by yang on 16-11-22.
 */
public class StormHandler extends ChannelInboundHandlerAdapter{
    private StormProducerConnection connect;
    private Throwable cause;
    private ConnectListener listener;
    public StormHandler(){

    }
    public StormHandler(StormProducerConnection conn){
        this.connect = conn;
    }
    public StormHandler(StormProducerConnection conn, ConnectListener listener) {
        this.connect = conn;
        this.listener = listener;
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception{
        super.channelActive(ctx);
        System.out.println("connected on server: "+ ctx.channel().remoteAddress().toString());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
       // System.out.println("channelRead");
        StormResponse response = (StormResponse)msg;
        String key = response.getRequestId();
        if(connect.ContrainsFuture(key)){
            InvokeFuture<Object> future = connect.removeFuture(key);
            //没有找到的发送请求
            if(future == null){
                return;
            }
            if(this.cause != null){
                //设置异常结果,会触发里面的回调函数
                future.setCause(cause);
            }else {
                future.setResult(response);
            }

        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        this.cause = cause;
        cause.printStackTrace();
        System.out.println("StormHandler caught exception");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        System.out.println("disconnect to broker");
        if(listener != null){
            listener.onDisconnected(ctx.channel().remoteAddress().toString());
        }
    }
}
