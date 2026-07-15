package com.mowtiie.feedit.net;

/**
 * Outcome of one HTTP fetch against a feed URL. Exactly one of
 * (body present), notModified, or a non-null errorMessage applies.
 */
public class FetchResult {

    private final int statusCode;
    private final byte[] body;
    private final boolean notModified;
    private final String newEtag;
    private final String newLastModified;
    private final String errorMessage;

    private FetchResult(int statusCode, byte[] body, boolean notModified,
                         String newEtag, String newLastModified, String errorMessage) {
        this.statusCode = statusCode;
        this.body = body;
        this.notModified = notModified;
        this.newEtag = newEtag;
        this.newLastModified = newLastModified;
        this.errorMessage = errorMessage;
    }

    public static FetchResult success(int statusCode, byte[] body, String etag, String lastModified) {
        return new FetchResult(statusCode, body, false, etag, lastModified, null);
    }

    public static FetchResult notModified(int statusCode) {
        return new FetchResult(statusCode, null, true, null, null, null);
    }

    public static FetchResult error(String message) {
        return new FetchResult(-1, null, false, null, null, message);
    }

    public boolean isSuccess() {
        return errorMessage == null && !notModified;
    }

    public boolean isNotModified() {
        return notModified;
    }

    public boolean isError() {
        return errorMessage != null;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public byte[] getBody() {
        return body;
    }

    public String getNewEtag() {
        return newEtag;
    }

    public String getNewLastModified() {
        return newLastModified;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
