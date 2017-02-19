package io.featureflow.client;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okio.BufferedSource;
import okio.Okio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by oliver on 6/06/2016.
 */
public class EventSource implements ConnectionHandler, Closeable {

    private final AtomicReference<State> state;
    private final URI uri;
    private final ExecutorService executor;
    private volatile long reconnectTimeMillis = 0L;
    private final Headers headers;
    private final EventSourceHandler eventSourceHandler; //handles the event. Great comment.
    private final OkHttpClient client;
    private volatile Call call;

    public static final Logger log = LoggerFactory.getLogger(EventSource.class);


    public EventSource(URI uri, long reconnectTimeMillis, Headers headers, EventSourceHandler eventSourceHandler) {

        this.state = new AtomicReference<>(State.UNINITIALISED);
        this.uri = uri;
        this.executor = Executors.newCachedThreadPool();
        this.reconnectTimeMillis = reconnectTimeMillis;
        this.headers = headers;
        this.eventSourceHandler = eventSourceHandler;
        this.client = new OkHttpClient().newBuilder().readTimeout(0L, TimeUnit.SECONDS).writeTimeout(0L, TimeUnit.SECONDS).connectTimeout(0L, TimeUnit.SECONDS).retryOnConnectionFailure(true).build();
    }

    public void init(){
        if(!this.state.compareAndSet(State.UNINITIALISED, State.CONNECTING)) {
            log.info("Already starting.");
        } else {
            log.debug("state change: " + State.UNINITIALISED  + " to " + State.CONNECTING);
            log.info("Starting EventSource client using URI: " + this.uri);
            this.executor.execute(new Runnable() {
                public void run() {
                    EventSource.this.doConnect();
                }
            });
        }
    }


    private void doConnect() {
        Response response = null;

        try {
            while(!Thread.currentThread().isInterrupted() && this.state.get() != State.SHUTDOWN) {


                State currentState = this.state.getAndSet(State.CONNECTING);
                log.debug("state change: " + currentState + " to " + State.CONNECTING);

                try {
                    okhttp3.Request.Builder ioe = (new okhttp3.Request.Builder()).headers(this.headers).url(this.uri.toASCIIString()).get();
                    /*if(this.lastEventId != null && !this.lastEventId.isEmpty()) {
                        ioe.addHeader("Last-Event-ID", this.lastEventId);
                    }*/

                    this.call = this.client.newCall(ioe.build());
                    response = this.call.execute();
                    if(response.isSuccessful()) {
                        currentState = (State) this.state.getAndSet(State.OPEN);
                        if(currentState != State.CONNECTING) {
                            log.warn("Unexpected state change: " + currentState + " to " + State.OPEN);
                        } else {
                            log.debug("state change: " + currentState + " to " + State.OPEN);
                        }

                        log.info("Connected to Feature Control SSE Stream");
                        BufferedSource bs = Okio.buffer(response.body().source());
                        EventStreamParser parser = new EventStreamParser(this.uri, this.eventSourceHandler, this);

                        String line;
                        while(!Thread.currentThread().isInterrupted() && (line = bs.readUtf8LineStrict()) != null) {
                            parser.line(line);
                            log.info(line);
                        }
                    } else {
                        log.debug("Failed Response: " + response);
                        this.eventSourceHandler.onError(new FailedResponseException(response.code()));
                    }
                } catch (EOFException var14) {
                    log.warn("Connection unexpectedly closed.");
                } catch (IOException var15) {
                    log.debug("Connection problem.", var15);
                    this.eventSourceHandler.onError(var15);
                } finally {
                    currentState = (State) this.state.getAndSet(State.CLOSED);
                    log.debug("state change: " + currentState + " to " + State.CLOSED);
                    if(response != null && response.body() != null) {
                        response.body().close();
                    }

                    if(this.call != null) {
                        this.call.cancel();
                    }

                }
                if(this.reconnectTimeMillis > 0L) {
                    log.info("Waiting to reconnect.." + this.reconnectTimeMillis);

                    try {
                        Thread.sleep(this.reconnectTimeMillis);
                    } catch (InterruptedException var13) {
                        ;
                    }
                }
            }
        } catch (RejectedExecutionException var17) {
            ;
        }

    }

    @Override
    public void setReconnectionTimeMillis(long reconnectionTimeMillis) {
        this.reconnectTimeMillis = reconnectionTimeMillis;
    }

    @Override
    public void setLastEventId(String lastEventId) {

    }

    @Override
    public void close() throws IOException {

    }

    enum State {
        UNINITIALISED,
        CONNECTING,
        OPEN,
        CLOSED,
        SHUTDOWN
    }


}
