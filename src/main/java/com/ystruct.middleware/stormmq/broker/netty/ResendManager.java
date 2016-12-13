package broker.netty;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yang on 16-12-3.
 */
public class ResendManager {
    //正在重发的线程数
    public static int resendThreadNumber = 0;
    private static List<Long> sendList = new ArrayList<Long>();
    public static int recordTime = 5;//5秒记录一次
    public static void record(){
        sendList.add(SendThread.sendToal.get());
    }
    //获取发送速率
    public static int getSendSpeek(){
        if(sendList.size() < 2){
            return  0;
        }
        //上次和这次的差值,除以时间就是速度
        long num = sendList.get(sendList.size() -1) -sendList.get(sendList.size()-2);
        int speed = (int)(num/10);
            return speed;
    }
    public static void startResend(){
        new Thread(new ResendThread()).start();
        resendThreadNumber++;
    }
}
