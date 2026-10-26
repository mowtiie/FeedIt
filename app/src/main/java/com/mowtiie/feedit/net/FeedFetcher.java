package com.mowtiie.feedit.net;

import com.mowtiie.feedit.BuildConfig;
import com.mowtiie.feedit.sync.SyncLog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownServiceException;
import java.util.zip.GZIPInputStream;

public class FeedFetcher {

    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 15_000;
    private static final int MAX_BODY_BYTES = 5 * 1024 * 1024;

    public FetchResult fetch(String url, String previousEtag, String previousLastModified) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("Accept-Encoding", "gzip");
            connection.setRequestProperty("User-Agent", "FeedIt/" + BuildConfig.VERSION_NAME + " (Android RSS reader)");

            if (previousEtag != null) {
                connection.setRequestProperty("If-None-Match", previousEtag);
            }
            if (previousLastModified != null) {
                connection.setRequestProperty("If-Modified-Since", previousLastModified);
            }

            long connectStart = System.currentTimeMillis();
            int status = connection.getResponseCode();
            SyncLog.d("      HTTP " + status + " headers in " + (System.currentTimeMillis() - connectStart) + "ms");

            if (status == HttpURLConnection.HTTP_NOT_MODIFIED) {
                return FetchResult.notModified(status);
            }
            if (status >= 300 && status < 400) {
                String location = connection.getHeaderField("Location");
                if (location != null && location.regionMatches(true, 0, "http://", 0, 7)) {
                    return FetchResult.error("Redirected to insecure " + location + " (cleartext HTTP is blocked)");
                }
            }
            if (status < 200 || status >= 300) {
                return FetchResult.error("HTTP " + status + " fetching " + url);
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