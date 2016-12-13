package test;

/**
 * Created by yang on 16-11-23.
 */
public interface FetcherCallback {
    void onData(Data data) throws Exception;
    void onError(Throwable cause);
}
