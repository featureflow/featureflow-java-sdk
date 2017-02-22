package io.featureflow.client;

        import okhttp3.Headers;

        import java.net.URI;
        import java.util.concurrent.CountDownLatch;

public class DebugClient {
    public static void main(String[] args) throws InterruptedException {
        Headers headers = new Headers.Builder()

                .add("Authorization", "Bearer " + "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI1ODNjZGQ0NWFkNDE0NDAwMDhhZWY3NzUiLCJhdXRoIjoiUk9MRV9FTlZJUk9OTUVOVCJ9.leo9BlSeycUaPSQ9iVNDpz22OVOvagQ1pI573jahfcK1xEuBEyD4C8TEmIDdxXaSQmPMXSCac7ib5_UTS3qrzw")
                //ff.io prod env .add("Authorization", "Bearer " + "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI1ODNjZGQ0NWFkNDE0NDAwMDhhZWY3NzUiLCJhdXRoIjoiUk9MRV9FTlZJUk9OTUVOVCJ9.leo9BlSeycUaPSQ9iVNDpz22OVOvagQ1pI573jahfcK1xEuBEyD4C8TEmIDdxXaSQmPMXSCac7ib5_UTS3qrzw")
                //.add("Authorization", "Bearer " + "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI1ODk4MzFiZThhZmQxODgzZDg4ZTQzMWEiLCJhdXRoIjoiUk9MRV9FTlZJUk9OTUVOVCJ9.1EvoDmtqOaAfYTtB3B1q7kSMp_Y27kQAa8GKM3fdHZcr1s6BQXHPW88U1j1K3Gwd4f0pHfZnSEJyZL0bd8kriA")
                .add("User-Agent", "FeatureflowClient-Java/" + "1.0")
                //.add("Accept", "text/event-stream")
                .build();
        /*Headers headers = new Headers.Builder()
                .add("Authorization", "api_key abc123")
                .add("User-Agent", "JavaClient/" + "1.0")
                .add("Accept", "text/event-stream")
                .build();*/
        //EventSource es = new EventSource(URI.create("http://localhost:7999/api/sdk/v1/controls/stream"), 10000l, headers, new EventSourceHandler() {
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
