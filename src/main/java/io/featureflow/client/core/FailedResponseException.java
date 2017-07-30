package io.featureflow.client.core;

/**
 * Created by oliver on 6/06/2016.
 */
public class FailedResponseException extends Throwable {
    private final int responseCode;

    public FailedResponseException(String responseMessage, int responseCode, String url) {
        super("Failed Response for url " + url + " " + responseMessage + " (" + responseCode + ")");

        this.responseCode = responseCode;

    }

    public int getResponseCode() {
        return responseCode;
    }
}
