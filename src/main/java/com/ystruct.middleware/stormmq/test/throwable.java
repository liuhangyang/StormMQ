package test;

/**
 * Created by yang on 16-11-25.
 */
public class throwable {
    public Throwable cause;

    public void dd(){
        try {
            int a = 0;
            int b = 2;
            int d = b / a;
        }catch (Exception e){
            System.out.println("cause:"+cause);
        }

    }

    public static void main(String[] args) {
        new throwable().dd();
    }
}
