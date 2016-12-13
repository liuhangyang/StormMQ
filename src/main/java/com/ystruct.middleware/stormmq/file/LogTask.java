package file;

import model.SendTask;

/**
 * Created by yang on 16-11-26.
 */
public class LogTask {
    private SendTask task;
    /**
     * status: 0代表message 已经被接收到了
     * status: 1代表消息已经成功的发送给了客户端
     */
    private int status;

    public LogTask(SendTask task,int status){
        this.task = task;
        this.status = status;
    }

    public SendTask getTask() {
        return task;
    }

    public void setTask(SendTask task) {
        this.task = task;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "LogTask{" +
                "task=" + task +
                ", status=" + status +
                '}';
    }
}
