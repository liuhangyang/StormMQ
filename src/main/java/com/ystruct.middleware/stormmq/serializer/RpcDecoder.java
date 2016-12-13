package serializer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * Created by yang on 16-11-22.
 */

/**
 * 对消息进行反序列化
 */
public class RpcDecoder extends ByteToMessageDecoder{
    private Class<?> genericClass;
    private KryoSerialization kryo;
    public RpcDecoder(Class<?> genericClass){
        this.genericClass = genericClass;
        kryo = new KryoSerialization();
        kryo.register(genericClass);
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
       // System.out.println("decode: "+ byteBuf.readableBytes());
        int HEAD_LENGTH = 4;
        /**
         *
         * HEAD_LENGTH是表示传输的数据的长度,序列化过程中,我们把数据的长度写在byte数组的前面四个字节.
         *
         */
        if(byteBuf.readableBytes() < HEAD_LENGTH){
            return;
        }
        byteBuf.markReaderIndex(); //标记当前的readIndex的位置.
        int dataLength = byteBuf.readInt(); //读取发送过来的消息长度,前四个字节.
       // System.out.println("dataLength:"+dataLength);
        if(dataLength < 0){
            channelHandlerContext.close();
        }
        if(byteBuf.readableBytes() < dataLength) { //读到的消息体长度如果小于我们传送过来的消息长度,则resetReaderIndex,则把readIndex重置到mark的位置.
            byteBuf.resetReaderIndex();
            return;
        }
        byte[] body = new byte[dataLength]; //这个时候,我们读到的前四个字节长度等于发送过来的实际长度.
        byteBuf.readBytes(body);
        Object obj = kryo.Deserialize(body);
        list.add(obj);
    }
}
