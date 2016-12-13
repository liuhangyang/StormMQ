package broker;

import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Phaser;


/**
 * Created by yang on 16-11-26.
 */
public class ConsumerGroupInfo {
    private int offset = 0; //偏移
    private final String groupId;
    //这个消费者组订阅哪些topic
    private final ConcurrentHashMap<String/*topic*/,SubscriptionInfo> subscriptionTable = new ConcurrentHashMap<String, SubscriptionInfo>();
   //这个消费组所有的消费者
    private final ConcurrentHashMap<String/*client id*/,ClientChannelInfo> channelInfoTable = new ConcurrentHashMap<String, ClientChannelInfo>();

    public ConsumerGroupInfo(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupId() {
        return groupId;
    }

    public ConcurrentHashMap<String, SubscriptionInfo> getSubscriptionTable() {
        return subscriptionTable;
    }

    public ConcurrentHashMap<String, ClientChannelInfo> getChannelInfoTable() {
        return channelInfoTable;
    }

    /**
     *  //向此消费组里添加消费者
     * @param clientId
     * @param channelnifo
     */
    public void addChannel(String clientId,ClientChannelInfo channelnifo){
        if(findChannel(channelnifo.getClientId()) == null) //此消费者中没有此消费者,则加入此组
            channelInfoTable.put(clientId,channelnifo);
        else
            System.out.println("exists this consumer,clientId is:"+clientId);
    }

    /**
     *     通过clientId把消费从系消费者中删除
     * @param clientId
     */

    public void removeChannel(String clientId){
        channelInfoTable.remove(clientId);
    }

    //查找这个Group中,clientId对应的信息
    public ClientChannelInfo findChannel(final String clientId){
        for(Map.Entry<String,ClientChannelInfo> entry:channelInfoTable.entrySet()){
            if(entry.getValue().getClientId().equals(clientId)){
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 通话channel 把消费者从此消费组中移除
     * @param channel
     */
    public void removeChannel(Channel channel){
        String clientId = "";
        for(Map.Entry<String,ClientChannelInfo> entry:channelInfoTable.entrySet()){
            if(entry.getValue().getChannel().equals(channel)){
                clientId = entry.getKey();
                break;
            }
        }
        if(!clientId.isEmpty()){
            removeChannel(clientId);
        }
    }

    /**
     * 往消费者中添加订阅信息
     * @param subscript
     */
    public void addSubscript(SubscriptionInfo subscript){
        //如果之前有相同的主题的订阅会被覆盖掉
        this.subscriptionTable.put(subscript.getTopic(),subscript);
    }
    public SubscriptionInfo findSubscriptionData(final String topic){
        return this.subscriptionTable.get(topic);
    }
    //获取所有的channel的clientId
    public List<String> getAllChannelId(){
        List<String> result = new ArrayList<String>();
        result.addAll(this.channelInfoTable.keySet());
        return result;
    }

    /**
     * 采用轮询的方式进行消费负载.
     * @return
     */
    public ClientChannelInfo getNextChannelInfo(){
        List<ClientChannelInfo> list = new ArrayList<ClientChannelInfo>();
        for(Map.Entry<String,ClientChannelInfo> entry:channelInfoTable.entrySet()){
            list.add(entry.getValue());
        }
        int size = list.size();
        int pos = 0;
        while (size > 0){
            pos = offset % list.size();
            ++offset;
            Channel channel = list.get(pos).getChannel();
            if(channel.isActive() && channel.isOpen() && channel.isWritable()){
                break;
            }else {
                --size;
            }

        }
        return list.get(pos);
    }
}
