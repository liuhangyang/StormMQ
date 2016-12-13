package StormTest;

/**
 * Created by yang on 16-12-11.
 */
public class TestResult {
    private boolean isSuccess = true;
    private String info;
    public boolean isSuccess(){
        return  isSuccess;
    }
    public void setSuccess(boolean isSuccess){
        this.isSuccess = isSuccess;
    }
    public String getInfo(){
        return  info;
    }
    public void setInfo(String info){
        this.info = info;
    }

    @Override
    public String toString() {
        return "TestResult{" +
                "isSuccess=" + isSuccess +
                ", info='" + info + '\'' +
                '}';
    }
}
