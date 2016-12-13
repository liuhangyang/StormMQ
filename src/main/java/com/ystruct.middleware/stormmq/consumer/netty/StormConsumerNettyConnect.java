package consumer.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import model.InvokeFuture;
import model.InvokeListener;
import model.StormRequest;
import model.StormResponse;
import producer.netty.StormProducerConnection;
import serializer.RpcDecoder;
import serializer.RpcEncoder;
import smq.SendCallback;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by yang on 16-11-24.
 */

/**
 * 描述Consumer与broker服务器的连接 主要是被动接收消息,主动发消息
 */
public class StormConsumerNettyConnect implements StormConsumerConnection{
    private InetSocketAddress inetAddr;
    private volatile Channel channel;
    private ChannelInboundHandlerAdapter handle; //网络通信处理器
    private Map<String/*requestId*/, InvokeFuture<Object>> futures = new ConcurrentHashMap<String, InvokeFuture<Object>>();
    private Map<String/*ip地址*/,Channel> channels = new ConcurrentHashMap<String, Channel>(); //连接数组
    private Bootstrap bootstrap;
    private long timeout = 3000; //默认超时
    private boolean connected = false;
    StormConsumerNettyConnect(){

    }
    public StormConsumerNettyConnect(String host,int port){
        inetAddr=new InetSocketAddress(host,port);
    }
    private Channel getChannel(String key){
        return channels.get(key);
    }
    @Override
    public void init() {
        try {
            EventLoopGroup group = new NioEventLoopGroup();
            bootstrap = new Bootstrap();
            bootstrap.group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    socketChannel.pipeline().addLast(new RpcDecoder(StormResponse.class));
                    socketChannel.pipeline().addLast(new RpcEncoder(StormRequest.class));
                    socketChannel.pipeline().addLast(handle);
                }
            }).option(ChannelOption.SO_KEEPALIVE,true);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void connect() {
        //连接的时候初始化
        if(handle!=null) {
            init();
         //   System.out.println("init");
        }
        else
        {
            System.out.println("handle is null");
            System.exit(0);
        }
        try
        {
            ChannelFuture future = bootstrap.connect(this.inetAddr).sync();
           // System.out.println("connect success");
            channels.put(this.inetAddr.toString(), future.channel());
            connected=true;
        }
        catch(InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void connect(String host, int port) {
        ChannelFuture future = bootstrap.connect(new InetSocketAddress(host,port));
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                Channel channel = channelFuture.channel();
                channels.put(channel.remoteAddress().toString(),channel);
            }
        });
    }

    @Override
    public void sethandle(ChannelInboundHandlerAdapter handler) {
        this.handle = handler;
    }

    //同步发送消息给服务器，并且收到服务器结果
    @Override
    public Object Send(StormRequest request) {
        if(channel==null)
            channel=getChannel(inetAddr.toString());
        if(channel!=null)
        {
            final InvokeFuture<Object> future=new InvokeFuture<Object>();
            futures.put(request.getRequestId(), future);
            //设置这次请求的ID
            future.setRequestId(request.getRequestId());
            ChannelFuture cfuture=channel.writeAndFlush(request);
            cfuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture rfuture) throws Exception {
                    if(!rfuture.isSuccess()){
                        future.setCause(rfuture.cause());
                    }
                }
            });
            try
            {
                Object result= future.getResult(timeout, TimeUnit.MILLISECONDS);
                return result;
            }
            catch(RuntimeException e)
            {
                throw e;
            }
            finally
            {
                //这个结果已经收到
                futures.remove(request.getRequestId());
            }
        }
        else
        {
            return null;
        }
    }

    @Override
    public void SendSync(StormRequest request) {
        if(channel==null)
            channel=getChannel(inetAddr.toString());
        if(channel!=null)
        {
            final InvokeFuture<Object> future=new InvokeFuture<Object>();
            futures.put(request.getRequestId(), future);
            //设置这次请求的ID
            future.setRequestId(request.getRequestId());
            //设置回调函数
            future.addInvokerListener(new InvokeListener<Object>() {
                @Override
                public void onResponse(Object t) {
                    // TODO Auto-generated method stub
                    StormResponse response=(StormResponse)t;
                    //TODO回调函数
                }
            });

            try
            {
                ChannelFuture cfuture;
                cfuture = channel.writeAndFlush(request).sync();
                cfuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture rfuture) throws Exception {
                        if(!rfuture.isSuccess()){
                            future.setCause(rfuture.cause());
                        }
                    }
                });
                //Object result=future.getResult(timeout, TimeUnit.MILLISECONDS);
            }
            catch(Throwable e)
            {
                //throw e;
            }
            finally
            {
                //移除已经收到的消息
                futures.remove(request.getRequestId());
            }
        }
    }

    @Override
    public void close() {
        if(channel != null)
            try {
                channel.close().sync();
            }catch (InterruptedException e){
                e.printStackTrace();
            }
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public boolean isClosed() {
        return  (null == channel) || !channel.isOpen() || !channel.isWritable() || !channel.isActive();
    }

    @Override
    public InvokeFuture<Object> removeFuture(String key) {
        if(ContainsFuture(key))
            return futures.remove(key);
        else
            return null;
    }

    @Override
    public void setTimeOut(long timeOut) {
        this.timeout = timeOut;
    }

    @Override
    public boolean ContainsFuture(String key) {
        if(key == null)
            return  false;
        return futures.containsKey(key);
    }
}
