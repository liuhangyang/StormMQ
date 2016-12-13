package serializer;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;



/**
 * Created by yang on 16-11-22.
 */

/**
 * 对消息进行序列化操作,发送的消息采用消息定长,
 * 即在消息的头部用4个字节表示要发送的数据的长度.
 *
 */
public class RpcEncoder extends MessageToByteEncoder {
    private Class<?> generiClass;
    private KryoSerialization kryo;
    public RpcEncoder(Class<?> generiClass){
        this.generiClass = generiClass;
        kryo = new KryoSerialization();
        kryo.register(generiClass);
    }

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object o, ByteBuf byteBuf) throws Exception {

        byte[] body = kryo.Serialize(o);
      // System.out.println("body.length:"+body.length);
        byteBuf.writeInt(body.length); //byte数组中的前四个字节是发送数据的长度.
        byteBuf.writeBytes(body);
      //  System.out.println("enocde后的长度:"+ byteBuf.readableBytes());
    }
}
