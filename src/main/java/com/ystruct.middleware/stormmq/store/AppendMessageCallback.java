package store;

/**
 * Created by yang on 16-11-30.
 */

import java.nio.ByteBuffer;

/**
 * 写 messages的回调接口
 */
public interface AppendMessageCallback {

    /**
     * message　序列化后,写进内内存映射文件 MapedByteBuffer
     * @param fileFromOffset
     * @param byteBuffer
     * @param maxBlank
     * @param msg
     * @return
     */

    public AppendMessageResult doAppend(final long fileFromOffset, final ByteBuffer byteBuffer,final int maxBlank,final Object msg);

}
