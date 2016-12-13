package test;



/**
 * Created by yang on 16-11-23.
 */
public class MyFetcher implements Fetcher {
    final Data data;
    public MyFetcher(Data data){
        this.data = data;
    }

    @Override
    public void fetchData(FetcherCallback callback) {
        try {
            callback.onData(data);
        }catch (Exception e){
            callback.onError(e);
        }
    }
}
