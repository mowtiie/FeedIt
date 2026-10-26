package com.mowtiie.feedit.net;

import com.mowtiie.feedit.BuildConfig;
import com.mowtiie.feedit.sync.SyncLog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.UnknownServiceException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class FeedFetcher {

    static final String USER_AGENT = "FeedIt/" + BuildConfig.VERSION_NAME + " (Android RSS reader)";
    static final String ACCEPT = "application/rss+xml, application/atom+xml, application/feed+json, "
            + "application/xml;q=0.9, text/xml;q=0.9, */*;q=0.8";

    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 15_000;
    private static final int MAX_BODY_BYTES = 5 * 1024 * 1024;

    public FetchResult fetch(String url, String previousEtag, String previousLastModified) {
        HttpURLConnection connection = null;
        try {
            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("Accept-Encoding", "gzip");
            headers.put("Accept", ACCEPT);
            headers.put("User-Agent", USER_AGENT);
            if (previousEtag != null) {
                headers.put("If-None-Match", previousEtag);
            }
            if (previousLastModified != null) {
                headers.put("If-Modified-Since", previousLastModified);
            }

            long connectStart = System.currentTimeMillis();
            connection = SafeHttp.open(url, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS, headers);
            int status = connection.getResponseCode();
            SyncLog.d("      HTTP " + status + " headers in " + (System.currentTimeMillis() - connectStart) + "ms");

            if (status == HttpURLConnection.HTTP_NOT_MODIFIED) {
                return FetchResult.notModified(status);
            }
            if (status < 200 || status >= 300) {
                return FetchResult.error(describeHttpError(status, url));
            }

            long bodyStart = System.currentTimeMillis();
            byte[] body = readBody(connection);
            SyncLog.d("      body " + body.length + " bytes in " + (System.currentTimeMillis() - bodyStart) + "ms");

            String etag = connection.getHeaderField("ETag");
            String lastModified = connection.getHeaderField("Last-Modified");
            return FetchResult.success(status, body, etag, lastModified);

        } catch (UnknownServiceException e) {
            return FetchResult.error("Cleartext HTTP is blocked, the feed must be served over https: " + url);
        } catch (IOException e) {
            SyncLog.w("      fetch threw " + e.getClass().getSimpleName() + ": " + e.getMessage() + " for " + url);
            return FetchResult.error(e.getClass().getSimpleName() + (e.getMessage() != null ? ": " + e.getMessage() : ""));
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    static String describeHttpError(int status, String url) {
        if (status == 403 || status == 429 || status == 503) {
            return "HTTP " + status + " fetching " + url + " (the site may be blocking automated readers)";
        }
        return "HTTP " + status + " fetching " + url;
    }

    private byte[] readBody(HttpURLConnection connection) throws IOException {
        String encoding = connection.getContentEncoding();
        InputStream raw = connection.getInputStream();

        try (InputStream in = "gzip".equalsIgnoreCase(encoding) ? new GZIPInputStream(raw) : raw) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            int total = 0;
            while ((read = in.read(chunk)) != -1) {
                total += read;
                if (total > MAX_BODY_BYTES) {
                    throw new IOException("Feed body exceeded " + MAX_BODY_BYTES + " bytes, aborting");
                }
                buffer.write(chunk, 0, read);
            }
            return buffer.toByteArray();
        }
    }
}
