package broker;

import io.netty.channel.Channel;

/**
 * Created by yang on 16-11-24.
 */

/**
 * 每个consumer的信息
 */
public class ClientChannelInfo {
    private final Channel channel; //关联的channel;
    private final String clientId; //客户端的Id;
    private SubscriptionInfo subcript; //该客户端的订阅信息.

    public ClientChannelInfo(Channel channel, String clientId) {
        this.channel = channel;
        this.clientId = clientId;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getClientId() {
        return clientId;
    }

    public SubscriptionInfo getSubcript() {
        return subcript;
    }

    public void setSubcript(SubscriptionInfo subcript) {
        this.subcript = subcript;
    }
}
