package test;

/**
 * Created by yang on 16-11-23.
 */
public class Data {
    private int n;
    private int m;
    public Data(int n,int m){
        this.n = n;
        this.m = m;
    }

    @Override
    public String toString() {
        int r = n/m;
        return  n+"/"+m+" = "+r;
    }
}
