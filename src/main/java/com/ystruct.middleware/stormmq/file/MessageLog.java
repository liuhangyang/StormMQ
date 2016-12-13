package file;

import model.SendTask;
import store.AllocateMappedFileService;
import store.MapedFileQueue;
import tool.Tool;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Vector;


/**
 * Created by yang on 16-11-26.
 */
public class MessageLog {
    private final int mapSize = 1024;
    private MapedFileQueue mapedFileQueue;
    final private String dir= "/"+System.getProperty("user.home")+"/store/";
    private FileHandler synFileHandler = null;          // 同步文件处理
    private FileHandler asynFileHandler = null;        // 异步处理文件

    public MessageLog(String fileName) throws IOException{
        AllocateMappedFileService allocateMappedFileService = new AllocateMappedFileService();
        allocateMappedFileService.start();
        mapedFileQueue = new MapedFileQueue(dir,mapSize,allocateMappedFileService);
        mapedFileQueue.load();

        File fileDir = new File(dir);
        if(fileDir.exists() == false && fileDir.isDirectory() == false){
            if(fileDir.mkdirs() == false){
                throw new IOException("create dir error");
            }
        }
        String fullSyncPath = dir + fileName+"_syn";
        String fullAsynPath = dir + fileName+"_asyn";
        System.out.println(fullSyncPath);
        System.out.println(fullAsynPath);

        File synFile = new File(fullSyncPath);
        File asynFile = new File(fullAsynPath);
        try {
            if(!synFile.exists()){
                synFile.createNewFile();
            }
            if(!asynFile.exists()){
                asynFile.createNewFile();
            }

        }catch (IOException e){
            e.printStackTrace();
        }
        String abosoluteSynPath = synFile.getAbsolutePath();
        System.out.println(abosoluteSynPath);
        //		synFileHandler = (FileHandler) new FileWRFactory().getRandomAccessFileHandler();
        synFileHandler  = new FileWRFactory().getChannelFileHandler();
        synFileHandler.Open(abosoluteSynPath,true);

        String abosoluteAsynPath = asynFile.getAbsolutePath();
        System.out.println(abosoluteAsynPath);
    //    asynFileHandler = (FileHandler) new FileWRFactory().getRandomAccessFileHandler();
        asynFileHandler = new FileWRFactory().getChannelFileHandler();
        asynFileHandler.Open(abosoluteAsynPath, false);

    }

    /**
     * 异步的保存消费结果.
     * @param data LogTask
     * @return
     */
    public boolean AsynSave(byte[] data){
        return asynFileHandler.AppendObject(data);
    }

    /**
     * 同步的保存任务,因为在落盘成功后,要给
     * @param list logTask 列表,有缓冲的进行输盘,，每一秒把缓存的所有LogTask进行刷盘.
     * @return
     *
     */
    public boolean SynSave(List<byte[]> list){
        byte[] temp = toByteArray(list.get(0).length,4);
        temp = byteMerger(temp, list.get(0));
        for (int i = 1; i < list.size(); i++) {
            temp = byteMerger(temp, toByteArray(list.get(i).length, 4));
            temp = byteMerger(temp, list.get(i));
        }

        return synFileHandler.AppendBytes(temp);
    }
    public boolean SynSave(byte[] data) {
        return synFileHandler.AppendObject(data);
    }

    /**
     *
     * 从文件中读取未发送的任务
     * @return 未发送的任务的List
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public List<SendTask> Restore() throws  IOException,ClassNotFoundException{
        //Vector是线程安全的.
        List<SendTask> res = new Vector<SendTask>();

        synFileHandler.PrepareForReadNextLine(); //把文件的position置为0;

        byte[] temp = null;
        while((temp = synFileHandler.ReadNextObject()) != null) {
//
//          ByteArrayInputStream is = new ByteArrayInputStream(temp);
//			ObjectInputStream in = new ObjectInputStream(is);
//			LogTask task = (LogTask) in.readObject();
            LogTask task = Tool.deserialize(temp, LogTask.class);

            res.add(task.getTask());
        }
       // System.out.println("syc读完");

        asynFileHandler.PrepareForReadNextLine();
        while((temp = asynFileHandler.ReadNextObject()) != null){
            //System.out.println("temp的长度:$$$$$$---->:"+temp.length);
           // System.out.println("asyc在读");
            LogTask task = Tool.deserialize(temp,LogTask.class);
           // System.out.println(task.toString());
            boolean isOk = res.remove(task.getTask());
            if(isOk == false){
              //  System.out.println("something error,don't contain this object");
            }
        }
       // System.out.println("syc-asyc完");
        return  res;

    }
    static byte[] byteMerger(byte[] byte_1, byte[] byte_2){
        byte[] byte_3 = new byte[byte_1.length+byte_2.length];
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
        System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
       // System.out.println("byte_3:"+byte_3.length);
        return byte_3;
    }
    static byte[] toByteArray(int iSource, int iArrayLen) {
        byte[] bLocalArr = new byte[iArrayLen];
        for (int i = 0; (i < 4) && (i < iArrayLen); i++) {
            bLocalArr[i] = (byte) (iSource >> 8 * i & 0xFF);
        }
       // System.out.println("bLocalArr:"+bLocalArr.length);
        return bLocalArr;
    }

    // 将byte数组bRefArr转为一个整数,字节数组的低位是整型的低字节位
    static int toInt(byte[] bRefArr) {
        int iOutcome = 0;
        byte bLoop;

        for (int i = 0; i < bRefArr.length; i++) {
            bLoop = bRefArr[i];
            iOutcome += (bLoop & 0xFF) << (8 * i);
        }
       // System.out.println("toInt.length:"+iOutcome);
        return iOutcome;
    }
}
