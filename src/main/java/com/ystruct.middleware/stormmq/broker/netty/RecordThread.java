package broker.netty;

import broker.TaskManager;

/**
 * Created by yang on 16-12-3.
 */

/**
 * 记录发送速度的线程,同时控制重发线程的启动.
 */
public class RecordThread implements Runnable {
    private  int lastSpeed = 0;
    private int nowSpeed = 0;
    @Override
    public void run() {
        while (true){

            lastSpeed = ResendManager.getSendSpeek();
           // System.out.println("lastSpeed:"+lastSpeed);
            ResendManager.record();
            nowSpeed = ResendManager.getSendSpeek();
           // System.out.println("nowSpeed:"+nowSpeed);
            //如果有重发的数据,且重发线程为0,则启动重发线程
            if(TaskManager.getResendNumber() > 0 && ResendManager.resendThreadNumber == 0){
                System.out.println("start resend Thread");
                ResendManager.startResend();
            }
            if(lastSpeed != 0){
                float per = (float)(nowSpeed / lastSpeed);
                System.out.println("per:"+per);
                //当发送速率降低的时候,判断一下是否需要增加重发线程
                if(per < 1 && ResendManager.resendThreadNumber != 0){
                    System.out.println("add resend Thread");
                    ResendManager.startResend();
                }
            }
            try {
                Thread.sleep(ResendManager.recordTime * 1000);

            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }

    }
}
