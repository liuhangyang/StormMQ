package broker;

/**
 * Created by yang on 16-11-26.
 */

import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订阅关系管理类
 */
public class ConsumerManager {
    //存储groupid -> GroupInfo
     private static  final ConcurrentHashMap<String/*Group*/,ConsumerGroupInfo> consumerTable = new ConcurrentHashMap<String, ConsumerGroupInfo>(1024);
     //存储每个group状态的表
    /**
     * 0:正常
     * -1:连接断开
     * 1:标识超时
     */
    private static final ConcurrentHashMap<String/*Group*/,Integer> consumerStat = new ConcurrentHashMap<String, Integer>(1024);

    public static void putConsumerGroupStat(String group,Integer stat){
        consumerStat.put(group,stat);
    }
    public static Integer getConsumerGroupStat(String group){
        return consumerStat.get(group);
    }
    public static ConsumerGroupInfo findGroupByGroupID(final String groupId){
        return consumerTable.get(groupId);
    }

    /**
     * 按照groupid 和topic寻找
     * @param groupId
     * @param topic
     * @return
     */
    public static ConsumerGroupInfo findGroupByGroupID(final String groupId,final String topic){
        ConsumerGroupInfo groupinfo = findGroupByGroupID(groupId);
        if(groupinfo.getSubscriptionTable().containsKey(topic)){
            return groupinfo;
        }
        return null;
    }

    /**
     * 查找所有订阅topic主题的消费组的信息
     * @param topic
     * @return
     */
    public static List<ConsumerGroupInfo> findGroupByTopic(final String topic){
        List<ConsumerGroupInfo> groups = new ArrayList<ConsumerGroupInfo>();
        for(Map.Entry<String,ConsumerGroupInfo> entry:consumerTable.entrySet()){
            if(entry.getValue().getSubscriptionTable().containsKey(topic)){
                groups.add(entry.getValue());
            }
        }
        return groups;
    }

    /**
     *　查找订阅topic且过滤条件一致的组
     * @param topic
     * @param property
     * @param value
     * @return
     */
    public static List<ConsumerGroupInfo> findGroupByTopic(final  String topic,final  String property,final String value ){
        List<ConsumerGroupInfo> groups = new ArrayList<ConsumerGroupInfo>();
        for(Map.Entry<String,ConsumerGroupInfo> entry:consumerTable.entrySet()) {
            ConcurrentHashMap<String,SubscriptionInfo> subscriptionTable = entry.getValue().getSubscriptionTable();
          //这个group订阅了topic并且过滤条件一致就返回
            if(subscriptionTable.containsKey(topic)){
                   //判断过滤条件是否一致，当过滤条件为空的时候 就是符合的
                if(subscriptionTable.get(topic).getFitlerName() == property && subscriptionTable.get(topic).getFitlerValue() == value){
                      groups.add(entry.getValue());
                    //如果该group 的过滤条件为空，则认为也是符合匹配的
                  }else if(subscriptionTable.get(topic).getFitlerName().isEmpty()&& subscriptionTable.get(topic).getFitlerValue().isEmpty()){
                      groups.add(entry.getValue());
                  }
            }
        }
        return groups;
    }

    /**
     * 根据clientID查找所属的消费组信息
     * @param clientID
     * @return
     */
    public static ConsumerGroupInfo findGroupByClientID(final String clientID){
        for(Map.Entry<String,ConsumerGroupInfo> entry:consumerTable.entrySet()) {
            if(entry.getValue().findChannel(clientID) != null){
                return entry.getValue();
            }

        }
              return null;
    }

    /**
     * 根据group和client 找到client对应的ClientChannelInfo;
     * @param group
     * @param clientId
     * @return
     */
    public static ClientChannelInfo findChannelInfoByID(final String group,final String clientId){
        for(Map.Entry<String,ConsumerGroupInfo> entry:consumerTable.entrySet()){
            ConsumerGroupInfo consumerGroupInfo = consumerTable.get(group);
            if(consumerGroupInfo != null){
                return consumerGroupInfo.findChannel(clientId);
            }
        }
        return null;
    }

    /**
     * 添加一个channelinfo 进入本组,如果组不存在,则创建.
     * @param group
     * @param channelInfo
     */
    public static void addGroupInfo(String group,ClientChannelInfo channelInfo){
          //如果此组不存在

       // System.out.println("#####"+findGroupByGroupID(group)==null?"yang":"long");
           if(findGroupByGroupID(group) == null) {
            //   System.out.println("addGroupInfo0");
                ConsumerGroupInfo newgroup = new ConsumerGroupInfo(group);
               //把此消费者添加到新创建的消费组里.
              // System.out.println("addGroupInfo1");
                newgroup.addChannel(channelInfo.getClientId(),channelInfo);
               //增加新的订阅信息.
              // System.out.println("addGroupInfo2");
               newgroup.addSubscript(channelInfo.getSubcript());
              // System.out.println("addGroupInfo3");

               consumerTable.put(group,newgroup); //加入一个新的组
              // System.out.println("addGroupInfo4");
               System.out.println("create group: "+group+ " topic:"+channelInfo.getSubcript().getTopic());
           }else if(findGroupByGroupID(group).findChannel(channelInfo.getClientId()) != null){
               //之前存在这个clientId,说明是重新连上的
               //把之前关联的旧的channel删除
               findGroupByGroupID(group).removeChannel(channelInfo.getClientId());
               findGroupByGroupID(group).addChannel(channelInfo.getClientId(),channelInfo);
               System.out.println("consumer reconnected");

           }else{
               //这里处理的是当消费者向broker发送Stop类型的消息时
               try{
                  // System.out.println("else");
                   String filterName = channelInfo.getSubcript().getFitlerName();
                   String topic = channelInfo.getSubcript().getTopic();

                   ConsumerGroupInfo groupInfo = findGroupByGroupID(group);
                   if(groupInfo.getSubscriptionTable().containsKey(topic)){
                       SubscriptionInfo sub = groupInfo.findSubscriptionData(topic);
                       if(filterName == null){
                           if(sub.getFitlerName() ==  null){
                               //添加进组
                               findGroupByGroupID(group).addChannel(channelInfo.getClientId(),channelInfo);
                               System.out.println("add into group 1");
                           }else{
                               //清除之前的组员,再添加进入
                               System.out.println("update subscript 1");
                               findGroupByGroupID(group).getSubscriptionTable().clear();
                               findGroupByGroupID(group).addSubscript(channelInfo.getSubcript());
                               findGroupByGroupID(group).getChannelInfoTable().clear();
                               findGroupByGroupID(group).addChannel(channelInfo.getClientId(),channelInfo);
                           }
                       }else {
                           if(sub.getFitlerName() != null && sub.getFitlerName().equals(filterName)){
                               //添加进入组
                               findGroupByGroupID(group).addChannel(channelInfo.getClientId(),channelInfo);
                               System.out.println("add into group 2");
                           }else{
                               //清除之前的组员,在添加进入
                               System.out.println("update subscript 2");
                               //如果加入的是新的订阅,之前的就无效了
                               findGroupByGroupID(group).getSubscriptionTable().clear();
                               findGroupByGroupID(group).addSubscript(channelInfo.getSubcript());

                               findGroupByGroupID(group).getChannelInfoTable().clear();
                               findGroupByGroupID(group).addChannel(channelInfo.getClientId(), channelInfo);
                           }
                       }
                   }else {
                       //清除之前的组员,在添加进入
                       System.out.println("update subscript 3");
                       //如果加入的是新的订阅,之前的就无效了.
                       findGroupByGroupID(group).getSubscriptionTable().clear();
                       findGroupByGroupID(group).addSubscript(channelInfo.getSubcript());

                       findGroupByGroupID(group).getChannelInfoTable().clear();
                       findGroupByGroupID(group).addChannel(channelInfo.getClientId(), channelInfo);
                   }
               }catch (Exception e){
                   e.printStackTrace();
               }
           }
    }
    //获取某个组某个主题的订阅信息

    /**
     * 获取某个组某个主题的订阅信息
     * @param group
     * @param topic
     * @return
     */
    public static SubscriptionInfo findSubscriptionInfo(final String group,final String topic){
        ConsumerGroupInfo consumerGroupInfo = findGroupByGroupID(group);
        if(consumerGroupInfo != null){
            return consumerGroupInfo.findSubscriptionData(topic);
        }
        return  null;
    }

    /**
     *  消费者在断开连接时,移除对应的订阅.
     * @param channel
     */
    public static  void  consumerDisconnect(Channel channel){
        for(Map.Entry<String,ConsumerGroupInfo> entry:consumerTable.entrySet()){
            entry.getValue().removeChannel(channel);
        }
    }

    /**
     * 根据channel ,移除相应的信息,如果channelInfoTable为空后,就删除组.
     * @param clientId
     */
    public static void stopConsumer(String clientId){
        for(Map.Entry<String,ConsumerGroupInfo> entry:consumerTable.entrySet()){
            String groupId = entry.getKey();
            ConsumerGroupInfo info = entry.getValue();
            if(info.findChannel(clientId) !=null){
                info.removeChannel(clientId);
                System.out.println(clientId+" stop");
            }
            if(info.getChannelInfoTable().isEmpty()){
                consumerTable.remove(groupId);
                System.out.println("remove group: "+ groupId);
            }
        }
    }

}
