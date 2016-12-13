package broker;

import broker.netty.FlushTool;
import model.SendTask;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by yang on 16-12-1.
 */
public class TaskManager {
    private static ConcurrentLinkedQueue<SendTask> taskQueue = new ConcurrentLinkedQueue<SendTask>();
    //需要重新发送的队列
    private static ConcurrentLinkedQueue<SendTask> resendTaskQueue = new ConcurrentLinkedQueue<SendTask>();
    //保存重发的任务
    private static Map<String, SendTask> map = new HashMap<String, SendTask>();

    /**
     * 添加一个发送任务
     *
     * @param task
     */
    public static boolean pushTask(SendTask task) {
        return taskQueue.offer(task);
    }

    public static boolean pushTask(List<SendTask> tasks) {
        boolean flag = false;
        for (SendTask sendTask : tasks) {
            flag = taskQueue.offer(sendTask);
        }
        return flag;
    }

    public static SendTask getTask() {
        return taskQueue.poll();
    }

    /**
     * 恢复之前的发送任务
     */

    public static void RecoverySendTask() {
        if (FlushTool.log != null) {
            try {
                //从文件中读出未发送的任务
                List<SendTask> list = FlushTool.log.Restore();
                System.out.println("recover size: " + list.size());
                //从文件读取任务,然后增加其SendTask.
                for (SendTask sendTask : list) {
                    pushTask(sendTask);
                    SemaphoreManager.increase("SendTask");
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public static boolean pushResendTask(SendTask task){
        String key = task.getGroupId()+task.getTopic()+task.getMessage().getMsgId();
        map.put(key,task);
        return resendTaskQueue.offer(task);
    }
    public static SendTask getResendTask(){
        //出队操作
        SendTask task = resendTaskQueue.poll();
        if(task == null){
            return null;
        }
        String key = task.getGroupId()+task.getTopic()+task.getMessage().getMsgId();
        map.remove(key);
        return task;
    }
    public static int getResendNumber(){
        return resendTaskQueue.size();
    }
    public static boolean findInResend(String groupID,String Topic,String MsgId){
        String key = groupID + Topic + MsgId;
        return map.containsKey(key);
    }
}