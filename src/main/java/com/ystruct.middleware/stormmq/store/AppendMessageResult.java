package store;

/**
 * Created by yang on 16-11-30.
 */

/**
 *
 */
public class AppendMessageResult {
    private AppendMessageStatus status; //返回码
    private long wroteOffset; //这个message在文件中的偏移量
    private int wroteBytes; //写的长度
    private String msgId; //消息ID
    private long storeTimestamp;
    private long logicsOffset; //
    public AppendMessageResult(AppendMessageStatus status){
        this(status,0,0,"",0,0);
    }
    public AppendMessageResult(AppendMessageStatus status, long wroteOffset, int wroteBytes, String msgId,
                               long storeTimestamp, long logicsOffset) {
        this.status = status;
        this.wroteOffset = wroteOffset;
        this.wroteBytes = wroteBytes;
        this.msgId = msgId;
        this.storeTimestamp = storeTimestamp;
        this.logicsOffset = logicsOffset;
    }

    public boolean isOk() {
        return this.status == AppendMessageStatus.PUT_OK;
    }

    public AppendMessageStatus getStatus() {
        return status;
    }

    public void setStatus(AppendMessageStatus status) {
        this.status = status;
    }

    public long getWroteOffset() {
        return wroteOffset;
    }

    public void setWroteOffset(long wroteOffset) {
        this.wroteOffset = wroteOffset;
    }

    public int getWroteBytes() {
        return wroteBytes;
    }

    public void setWroteBytes(int wroteBytes) {
        this.wroteBytes = wroteBytes;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public long getStoreTimestamp() {
        return storeTimestamp;
    }

    public void setStoreTimestamp(long storeTimestamp) {
        this.storeTimestamp = storeTimestamp;
    }

    public long getLogicsOffset() {
        return logicsOffset;
    }

    public void setLogicsOffset(long logicsOffset) {
        this.logicsOffset = logicsOffset;
    }


    @Override
    public String toString() {
        return "AppendMessageResult [status=" + status + ", wroteOffset=" + wroteOffset + ", wroteBytes="
                + wroteBytes + ", msgId=" + msgId + ", storeTimestamp=" + storeTimestamp + ", logicsOffset="
                + logicsOffset + "]";
    }

}
