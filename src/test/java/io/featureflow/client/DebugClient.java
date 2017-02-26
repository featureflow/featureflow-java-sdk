package io.featureflow.client;

        import okhttp3.Headers;

        import java.net.URI;
        import java.util.concurrent.CountDownLatch;

public class DebugClient {
    public static void main(String[] args) throws InterruptedException {
        Headers headers = new Headers.Builder()
            .add("Authorization", "Bearer " + "YOURKEY")
            .add("User-Agent", "FeatureflowClient-Java/" + "1.0")
            .build();
        EventSource es = new EventSource(URI.create("https://rtm.featureflow.io/api/sdk/v1/controls/stream"), 10000l, headers, new EventSourceHandler() {
            @Override
            public void onConnect() {
                System.out.println("CONNECTED");
            }

            @Override
            public void onMessage(String event, MessageEvent message) {
                System.out.println("event = " + event + ", message = " + message.getData());
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("ERROR");
                t.printStackTrace();
            }
        });

        es.init();
        new CountDownLatch(1).await();
    }
}
