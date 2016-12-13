package producer.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import model.InvokeFuture;
import model.InvokeListener;
import model.StormRequest;
import model.StormResponse;
import serializer.RpcDecoder;
import serializer.RpcEncoder;
import smq.SendCallback;
import smq.SendResult;


import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;



/**
 * Created by yang on 16-11-22.
 */
public class StormProducerNettyConnect implements StormProducerConnection {
    private InetSocketAddress inetAddr; //
    private volatile Channel channel;
    //连接处理类
    private ChannelInboundHandlerAdapter handle;
    private Map<String,InvokeFuture<Object>> futures = new ConcurrentHashMap<String, InvokeFuture<Object>>();
    private Map<String,Channel> channels = new ConcurrentHashMap<String, Channel>(); //ip和channel的映射关系
    private Bootstrap bootstrap;
    private long timeout = 10000; //默认超时时间.
    private boolean connected = false;

    public StormProducerNettyConnect() {
    }

    public StormProducerNettyConnect(String host,int port) {
        inetAddr = new InetSocketAddress(host,port);
    }
    //设置要处理连接的类
    public void setHandle(ChannelInboundHandlerAdapter handle){
        this.handle = handle;
    }
    public Channel getChannel(String key){
        return channels.get(key);
    }
    @Override
    public void init() {
        try {
            EventLoopGroup group = new NioEventLoopGroup();
            bootstrap = new Bootstrap();
            bootstrap.group(group).channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new RpcDecoder(StormResponse.class));//解码
                            socketChannel.pipeline().addLast(new RpcEncoder(StormRequest.class));//编码
                            socketChannel.pipeline().addLast(handle);
                        }
                    }).option(ChannelOption.SO_KEEPALIVE,true);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     *同步连接broker.
     */
    @Override
    public void connect() {
        //连接的时候进行初始化
        if(handle != null){
            init();
        }else{
            System.out.println("handle is null");
            System.exit(0);
        }
        try{
            ChannelFuture future = bootstrap.connect(this.inetAddr).sync();
            channels.put(this.inetAddr.toString(),future.channel());
            connected = true;
        }catch (InterruptedException e){
            System.out.println("StormProducerNettyConnect");
            e.printStackTrace();
        }
    }

    /**
     * 异步连接broker
     * @param host
     * @param port
     */
    @Override
    public void connect(String host, int port) {
        ChannelFuture future = bootstrap.connect(new InetSocketAddress(host,port));
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                Channel channel = channelFuture.channel();
                //添加进连接数组.
                channels.put(channel.remoteAddress().toString(),channel);
            }
        });
    }

    @Override
    public void setHandler(ChannelInboundHandlerAdapter handler){
        this.handle = handler;
    }

    /**
     * 同步发送消息给服务器,并且收到服务器结果
     * @param request
     * @return
     */
    @Override
    public Object Send(StormRequest request) {
        if(channel == null){
            channel = getChannel(inetAddr.toString());
        }
        if(channel != null){
            final InvokeFuture<Object> future = new InvokeFuture<Object>();
            futures.put(request.getRequestId(),future);
            //设置本次请求的id.
            future.setRequestId(request.getRequestId());
           // System.out.println("StrormProducerNettyConnect:writeAndFlush Before:");
            ChannelFuture cFuture = channel.writeAndFlush(request);
            cFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                  //  System.out.println("StormProduceNettyConnect:"+"request发送完成");
                    if(!channelFuture.isSuccess()){
                        future.setCause(channelFuture.cause());
                    }
                }
            });
            try {
                Thread.sleep(0);
                Object result = future.getResult(timeout, TimeUnit.MILLISECONDS);
               // System.out.println("没有超时");
                return result;
            }catch (RuntimeException e){
                throw e;
            }catch (InterruptedException e){
                e.printStackTrace();
                return null;
            }
            finally {
                //这个结果已经收到
                futures.remove(request.getRequestId());
            }
        }else {
            return null;
        }
    }

    /**
     * 异步发送请求
     * @param request
     * @param listener
     */
    @Override
    public void  Send(StormRequest request, final SendCallback listener) {
        if(channel == null) {
            channel = getChannel(inetAddr.toString());
        }
        if(channel != null){
                final InvokeFuture<Object> future = new InvokeFuture<Object>();
                futures.put(request.getRequestId(),future);
                //设置这次请求的ID，
                future.setRequestId(request.getRequestId());
                //设置回调函数
                future.addInvokerListener(new InvokeListener<Object>() {
                    @Override
                    public void onResponse(Object o) {
                        StormResponse response = (StormResponse)o;
                        //回调函数
                        listener.onResult((SendResult)response.getResponse());
                    }
                });
            final ChannelFuture cfuture = channel.writeAndFlush(request);
            cfuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if(!channelFuture.isSuccess()){
                        future.setCause(channelFuture.cause());
                    }

                }
            });
            try
            {
                //Object result=future.getResult(timeout, TimeUnit.MILLISECONDS);
            }
            catch(RuntimeException e)
            {
                throw e;
            }
            finally
            {
                //移除已经收到的消息
               // futrues.remove(request.getRequestId());
            }
        }
    }

    @Override
    public void close() {
        if(channel != null){
            try{
                channel.close().sync();
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public boolean isClosed() {
        return (channel == null) || !channel.isOpen() || !channel.isWritable() || !channel.isActive();
    }

    @Override
    public boolean ContrainsFuture(String key) {
        if(key == null){
            return false;
        }
        return futures.containsKey(key);
    }

    @Override
    public InvokeFuture<Object> removeFuture(String key) {
        if(ContrainsFuture(key)){
            return futures.remove(key);
        }else{
            return null;
        }
    }

    @Override
    public void setTimeOut(long timeOut) {
        this.timeout = timeOut;
    }
}
