package io.featureflow.client;

/**
 * Created by oliver on 6/06/2016.
 */
public class FailedResponseException extends Throwable {
    private final int responseCode;

    public FailedResponseException(int responseCode) {
        super("Failed Response (" + responseCode + ")");

        this.responseCode = responseCode;

    }

    public int getResponseCode() {
        return responseCode;
    }
}
