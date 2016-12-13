package tool;

/**
 * Created by yang on 16-12-1.
 */

import broker.netty.Conf;
import sun.security.util.DerEncoder;

import java.io.*;
import java.util.Date;
import java.util.Properties;

/**
 * 日志工具类,使用了单例模式,保证只有一个实例,
 * 为了更方便的配置日志文件名,使用属性文件配置,
 * 也可以在程序指定日志文件名.
 */
public class LogWriter {
    //日志的配置文件
    public static final String LOG_CONFIGFILE_NAME = "/home/yang/log.properties";
    //日志文件名在配置文件中的标签
    public static final String LOGFILE_TAG_NAME = "logfile";
    //默认的日志文件的路径和文件名称
    private final String DEFAULT_LOG_FILE_NAME = "/"+System.getProperty("user.home")+"/output.log";
    //该类的唯一实例
    private static LogWriter logWriter;
    //文件的输出流
    private PrintWriter write;
    //日志文件名
    private String logFileName;

    private LogWriter(int version) throws Exception{
        this.init();
    }
    private LogWriter(String fileName) throws Exception{
        this.logFileName = fileName;
        this.init();
    }
    /**
     * 获取LogWriter的唯一实例
     */
    public synchronized  static LogWriter getLogWriter() throws Exception{
        if(logWriter == null){
            System.out.println("getLogWriter:"+Conf.initValue++);
            logWriter = new LogWriter(Conf.initValue++);
        }
        return logWriter;
    }
    public synchronized static LogWriter getLogWriter(String logFileName) throws Exception{
        if(logWriter == null){
            logWriter = new LogWriter(logFileName);
        }
        return logWriter;
    }
    /**
     * 往日志中写一条日志信息,为了防止多线程写日志文件,造成文件"死锁",使用synchronized关键字
     *
     */
    public synchronized void log(String logMsg){
        //System.out.println("记录日志了");
        System.out.println(logMsg);
        this.write.println(new Date()+": "+logMsg);
    }
    /**
     * 往日志文件中写一条异常信息,使用synchronized关键字
     *
     */
    public synchronized void log(Exception e){
        write.println(new Date()+": ");
        e.printStackTrace(write);
    }
    private void init() throws Exception{
        //如果用户没有在参数中指定日志文件名,则从配置文件中获取.
        if(this.logFileName == null){
            this.logFileName = this.getLogFileNameConfigFile();
            //如果配置文件不存在或者也没有指定日志文件名,则用默认的日志文件名
            if(this.logFileName == null){
                this.logFileName = DEFAULT_LOG_FILE_NAME;
            }
         }
        File logFile = new File(this.logFileName);
        try {
            //其中的FileWriter()中的第二个参数的含义是:是否在文件中追加内容.
            //PrintWriter()中的第二个参数的含义是:自动将数据flush到文件中.
            write = new PrintWriter(new FileWriter(logFile,false),true);
            System.out.println("日志文件的位置: "+logFile.getAbsolutePath());
        }catch (IOException ex){
           String errmsg = "无法打开日志文件: "+logFile.getAbsolutePath();
            throw new Exception(errmsg,ex);
        }
    }


    private String getLogFileNameConfigFile(){
        try{
            Properties pro = new Properties();
            //在类的当前位置,查找属性配置文件log.properties
            InputStream fin = getClass().getResourceAsStream(LOG_CONFIGFILE_NAME);
            if(fin != null){
                pro.load(fin); //载入配置文件
                fin.close();
                return pro.getProperty(LOGFILE_TAG_NAME);
            }else {
                System.err.println("无法打开配置文件: log.properties");
            }
        }catch (IOException e){
            System.err.println("无法打开配置文件: log.properties");
        }
        return null;
    }
    //关闭LogWriter
    public void close(){
        logWriter = null;
        if(write != null){
            write.close();
        }
    }
    public static void main(String[] args){
        LogWriter logger = null;
        try {
        //    String fileName = "/"+System.getProperty("user.home")+"/output.log";
            logger = LogWriter.getLogWriter();
            logger.log("First log");
            logger.log("第一条日志");
        }catch (Exception e){
            e.printStackTrace();

        }
    }

}
