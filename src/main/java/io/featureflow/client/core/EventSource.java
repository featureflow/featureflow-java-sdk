package io.featureflow.client.core;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okio.Buffer;
import okio.BufferedSource;
import okio.Okio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class EventSource implements ConnectionHandler, Closeable {

    private final AtomicReference<State> state;
    private final URI uri;
    private final ExecutorService executor;
    private volatile long reconnectTimeMillis = 0L;
    private final Headers headers;
    private final EventSourceHandler eventSourceHandler;
    private final OkHttpClient client;
    private volatile Call call;

    public static final Logger log = LoggerFactory.getLogger(EventSource.class);
    private String lastEventId;


    public EventSource(URI uri, long reconnectTimeMillis, Headers headers, EventSourceHandler eventSourceHandler) {
        this.state = new AtomicReference<>(State.UNINITIALISED);
        this.uri = uri;
        this.executor = Executors.newCachedThreadPool();
        this.reconnectTimeMillis = reconnectTimeMillis;
        this.headers = headers;
        this.eventSourceHandler = eventSourceHandler;

        X509TrustManager trustManager;
        SSLSocketFactory sslSocketFactory;
        try {
            trustManager = trustManagerForCertificates(trustedCertificatesInputStream());
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { trustManager }, null);
            sslSocketFactory = sslContext.getSocketFactory();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
        this.client = new OkHttpClient().newBuilder()
                .sslSocketFactory(sslSocketFactory)
                .readTimeout(0L, TimeUnit.SECONDS).writeTimeout(0L, TimeUnit.SECONDS).connectTimeout(0L, TimeUnit.SECONDS).retryOnConnectionFailure(true).build();
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
                    if(this.lastEventId != null && !this.lastEventId.isEmpty()) {
                        ioe.addHeader("Last-Event-ID", this.lastEventId);
                    }
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
                        EventSourceParser parser = new EventSourceParser(this.uri, this.eventSourceHandler, this);

                        String line;
                        while(!Thread.currentThread().isInterrupted() && (line = bs.readUtf8LineStrict()) != null) {
                            parser.line(line);
                        }
                    } else {
                        log.debug("Failed Response: " + response);
                        this.eventSourceHandler.onError(new FailedResponseException(response.code()));
                    }
                } catch (EOFException eof) {
                    log.warn("Connection unexpectedly closed due to {}.", eof.getMessage());
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
        this.lastEventId = lastEventId;
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




    /**
     * These trusted certificates are for letsenrypt certificates. We want to ensure that all users regardless of java version have a matching truststore.
     *
     */
    private InputStream trustedCertificatesInputStream() {
        // PEM files for root certificates of LetsEncrypt Intermediary and Root.
        String letsEncryptRootCa =
                "-----BEGIN CERTIFICATE-----\n" +
                "MIIFCDCCA/CgAwIBAgISA7R03EyEk4Q9cB6bXqBQ3pe+MA0GCSqGSIb3DQEBCwUA\n" +
                "MEoxCzAJBgNVBAYTAlVTMRYwFAYDVQQKEw1MZXQncyBFbmNyeXB0MSMwIQYDVQQD\n" +
                "ExpMZXQncyBFbmNyeXB0IEF1dGhvcml0eSBYMzAeFw0xNzAyMjExMDMxMDBaFw0x\n" +
                "NzA1MjIxMDMxMDBaMB0xGzAZBgNVBAMTEnJ0bS5mZWF0dXJlZmxvdy5pbzCCASIw\n" +
                "DQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAM4ofQz/i23IaUZO1BwqP4zyhirq\n" +
                "EvxhC9SUBe2izectFZ0kMJfPyR7sTYu4wXEMSyu6IoltbbcZDnRWJAbkcwA3hqrb\n" +
                "XEJCw5/dXeKh4EtmcXcoZLSiwiF7q3XUDJDBXapuiQlojqndH4fiovUCoQUq4lBn\n" +
                "alUchUdPx3reQHfwENwxQHROldB2yOfxyR+JNYQtVEZ0putzhdDQ7djdI9xcZ0EP\n" +
                "vXmw61/PHHPGbG1KBT71uPNocNLf4DYXvGoqsP5zcPh0nPBSofJS4yj2O3argtIK\n" +
                "SHqLyDyoAopz6LAUnYKcrRheCmNMLCQUmTwMRYG295ZLwTEFxl9J66osNXECAwEA\n" +
                "AaOCAhMwggIPMA4GA1UdDwEB/wQEAwIFoDAdBgNVHSUEFjAUBggrBgEFBQcDAQYI\n" +
                "KwYBBQUHAwIwDAYDVR0TAQH/BAIwADAdBgNVHQ4EFgQUG2Vo0hyMNPakxPED6sTd\n" +
                "qal6hpIwHwYDVR0jBBgwFoAUqEpqYwR93brm0Tm3pkVl7/Oo7KEwcAYIKwYBBQUH\n" +
                "AQEEZDBiMC8GCCsGAQUFBzABhiNodHRwOi8vb2NzcC5pbnQteDMubGV0c2VuY3J5\n" +
                "cHQub3JnLzAvBggrBgEFBQcwAoYjaHR0cDovL2NlcnQuaW50LXgzLmxldHNlbmNy\n" +
                "eXB0Lm9yZy8wHQYDVR0RBBYwFIIScnRtLmZlYXR1cmVmbG93LmlvMIH+BgNVHSAE\n" +
                "gfYwgfMwCAYGZ4EMAQIBMIHmBgsrBgEEAYLfEwEBATCB1jAmBggrBgEFBQcCARYa\n" +
                "aHR0cDovL2Nwcy5sZXRzZW5jcnlwdC5vcmcwgasGCCsGAQUFBwICMIGeDIGbVGhp\n" +
                "cyBDZXJ0aWZpY2F0ZSBtYXkgb25seSBiZSByZWxpZWQgdXBvbiBieSBSZWx5aW5n\n" +
                "IFBhcnRpZXMgYW5kIG9ubHkgaW4gYWNjb3JkYW5jZSB3aXRoIHRoZSBDZXJ0aWZp\n" +
                "Y2F0ZSBQb2xpY3kgZm91bmQgYXQgaHR0cHM6Ly9sZXRzZW5jcnlwdC5vcmcvcmVw\n" +
                "b3NpdG9yeS8wDQYJKoZIhvcNAQELBQADggEBAFlLnzLSonW8/+avm6xAQAowofz9\n" +
                "Dm87g2EkpFhuKEc9XG2c/j8rKtT2ATTche8NQ2Y3yX+W7a4WB3At8J+S0FpkeLig\n" +
                "o5zMUfgH6rrMETkvhAnMdRSdg2/Ug4Ijt5XeUGVLima8dd+f5hYzzuCnN9c0CFle\n" +
                "jQqVYgaYe/CIy0JBJ4OgJ/dUvzmrlPP7AHc2OVpo2DJKtVcSJ038ZIKa7q+5pibA\n" +
                "74wMMfk1/w4wxBPJBash05bMw9n1uPLLMqCwOEsxO7LShbXW7r7Qg1aSoLD1T/BU\n" +
                "lBaYcxRrcPCJyT9aWya3BkiWs7rxR0h1XIHy7XUNeJ5B94pZ2Wmkmx+Uo3s=\n" +
                "-----END CERTIFICATE-----\n";
        String letsEncryptIntermediaryCa =
                "-----BEGIN CERTIFICATE-----\n" +
                "MIIEkjCCA3qgAwIBAgIQCgFBQgAAAVOFc2oLheynCDANBgkqhkiG9w0BAQsFADA/\n" +
                "MSQwIgYDVQQKExtEaWdpdGFsIFNpZ25hdHVyZSBUcnVzdCBDby4xFzAVBgNVBAMT\n" +
                "DkRTVCBSb290IENBIFgzMB4XDTE2MDMxNzE2NDA0NloXDTIxMDMxNzE2NDA0Nlow\n" +
                "SjELMAkGA1UEBhMCVVMxFjAUBgNVBAoTDUxldCdzIEVuY3J5cHQxIzAhBgNVBAMT\n" +
                "GkxldCdzIEVuY3J5cHQgQXV0aG9yaXR5IFgzMIIBIjANBgkqhkiG9w0BAQEFAAOC\n" +
                "AQ8AMIIBCgKCAQEAnNMM8FrlLke3cl03g7NoYzDq1zUmGSXhvb418XCSL7e4S0EF\n" +
                "q6meNQhY7LEqxGiHC6PjdeTm86dicbp5gWAf15Gan/PQeGdxyGkOlZHP/uaZ6WA8\n" +
                "SMx+yk13EiSdRxta67nsHjcAHJyse6cF6s5K671B5TaYucv9bTyWaN8jKkKQDIZ0\n" +
                "Z8h/pZq4UmEUEz9l6YKHy9v6Dlb2honzhT+Xhq+w3Brvaw2VFn3EK6BlspkENnWA\n" +
                "a6xK8xuQSXgvopZPKiAlKQTGdMDQMc2PMTiVFrqoM7hD8bEfwzB/onkxEz0tNvjj\n" +
                "/PIzark5McWvxI0NHWQWM6r6hCm21AvA2H3DkwIDAQABo4IBfTCCAXkwEgYDVR0T\n" +
                "AQH/BAgwBgEB/wIBADAOBgNVHQ8BAf8EBAMCAYYwfwYIKwYBBQUHAQEEczBxMDIG\n" +
                "CCsGAQUFBzABhiZodHRwOi8vaXNyZy50cnVzdGlkLm9jc3AuaWRlbnRydXN0LmNv\n" +
                "bTA7BggrBgEFBQcwAoYvaHR0cDovL2FwcHMuaWRlbnRydXN0LmNvbS9yb290cy9k\n" +
                "c3Ryb290Y2F4My5wN2MwHwYDVR0jBBgwFoAUxKexpHsscfrb4UuQdf/EFWCFiRAw\n" +
                "VAYDVR0gBE0wSzAIBgZngQwBAgEwPwYLKwYBBAGC3xMBAQEwMDAuBggrBgEFBQcC\n" +
                "ARYiaHR0cDovL2Nwcy5yb290LXgxLmxldHNlbmNyeXB0Lm9yZzA8BgNVHR8ENTAz\n" +
                "MDGgL6AthitodHRwOi8vY3JsLmlkZW50cnVzdC5jb20vRFNUUk9PVENBWDNDUkwu\n" +
                "Y3JsMB0GA1UdDgQWBBSoSmpjBH3duubRObemRWXv86jsoTANBgkqhkiG9w0BAQsF\n" +
                "AAOCAQEA3TPXEfNjWDjdGBX7CVW+dla5cEilaUcne8IkCJLxWh9KEik3JHRRHGJo\n" +
                "uM2VcGfl96S8TihRzZvoroed6ti6WqEBmtzw3Wodatg+VyOeph4EYpr/1wXKtx8/\n" +
                "wApIvJSwtmVi4MFU5aMqrSDE6ea73Mj2tcMyo5jMd6jmeWUHK8so/joWUoHOUgwu\n" +
                "X4Po1QYz+3dszkDqMp4fklxBwXRsW10KXzPMTZ+sOPAveyxindmjkW8lGy+QsRlG\n" +
                "PfZ+G6Z6h7mjem0Y+iWlkYcV4PIWL1iwBi8saCbGS5jN2p8M+X+Q7UNKEkROb3N6\n" +
                "KOqkqm57TH2H3eDJAkSnh6/DNFu0Qg==\n" +
                "-----END CERTIFICATE-----";

        return new Buffer()
                .writeUtf8(letsEncryptRootCa)
                .writeUtf8(letsEncryptIntermediaryCa)
                .inputStream();
    }
    private X509TrustManager trustManagerForCertificates(InputStream in)
            throws GeneralSecurityException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        Collection<? extends Certificate> certificates = certificateFactory.generateCertificates(in);
        if (certificates.isEmpty()) {
            throw new IllegalArgumentException("expected non-empty set of trusted certificates");
        }

        // Put the certificates a key store.
        char[] password = "password".toCharArray(); // Any password will work.
        KeyStore keyStore = newEmptyKeyStore(password);
        int index = 0;
        for (Certificate certificate : certificates) {
            String certificateAlias = Integer.toString(index++);
            keyStore.setCertificateEntry(certificateAlias, certificate);
        }

        // Use it to build an X509 trust manager.
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
            throw new IllegalStateException("Unexpected default trust managers:"
                    + Arrays.toString(trustManagers));
        }
        return (X509TrustManager) trustManagers[0];
    }

    private KeyStore newEmptyKeyStore(char[] password) throws GeneralSecurityException {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            InputStream in = null; // By convention, 'null' creates an empty key store.
            keyStore.load(in, password);
            return keyStore;
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

}
