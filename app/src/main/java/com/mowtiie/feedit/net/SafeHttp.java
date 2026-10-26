package com.mowtiie.feedit.net;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * Opens HTTP connections the way a reader needs: redirects are followed manually so that
 * a hop to {@code http://} is upgraded to {@code https://}. Android blocks cleartext traffic
 * and {@link HttpURLConnection} never follows an https-to-http redirect on its own, which
 * breaks feeds such as Blogger (redirects to http://feeds.feedburner.com).
 */
public final class SafeHttp {

    private static final int MAX_REDIRECTS = 5;

    private SafeHttp() {
    }

    /** Upgrades an {@code http://} URL to {@code https://}; other URLs are returned unchanged. */
    public static String upgradeToHttps(String url) {
        if (url != null && url.regionMatches(true, 0, "http://", 0, 7)) {
            return "https://" + url.substring(7);
        }
        return url;
    }

    /** Resolves a {@code Location} header (possibly relative) against the current URL, upgrading http to https. */
    public static String resolveRedirect(String currentUrl, String location) throws MalformedURLException {
        return upgradeToHttps(new URL(new URL(currentUrl), location).toString());
    }

    static boolean isRedirect(int status) {
        return status == HttpURLConnection.HTTP_MOVED_PERM
                || status == HttpURLConnection.HTTP_MOVED_TEMP
                || status == HttpURLConnection.HTTP_SEE_OTHER
                || status == 307
                || status == 308;
    }

    /** Connects to the URL, follows up to five redirects and returns the connection with its response ready. */
    public static HttpURLConnection open(String url, int connectTimeoutMs, int readTimeoutMs,
                                         Map<String, String> headers) throws IOException {
        String current = upgradeToHttps(url);
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            HttpURLConnection connection = (HttpURLConnection) new URL(current).openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(connectTimeoutMs);
            connection.setReadTimeout(readTimeoutMs);
            for (Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }

            int status = connection.getResponseCode();
            String location = connection.getHeaderField("Location");
            if (!isRedirect(status) || location == null) {
                return connection;
            }
            connection.disconnect();
            current = resolveRedirect(current, location);
        }
        throw new IOException("Too many redirects");
    }
}
