package test;

/**
 * Created by yang on 16-11-23.
 */
public class Worker {
    public void doWork(){
        Fetcher fetcher = new MyFetcher(new Data(1,1));
        fetcher.fetchData(new FetcherCallback() {
            @Override
            public void onData(Data data) throws Exception {
                System.out.println("Data received: "+data);
            }

            @Override
            public void onError(Throwable cause) {
                System.out.println("An error accour: "+cause.getMessage());
            }
        });
    }

    public static void main(String[] args) {
        Worker w = new Worker();
        w.doWork();
    }
}
