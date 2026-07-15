package com.mowtiie.feedit.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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
            connection.setRequestProperty("User-Agent", "FeedIt/1.0 (Android RSS reader)");

            if (previousEtag != null) {
                connection.setRequestProperty("If-None-Match", previousEtag);
            }
            if (previousLastModified != null) {
                connection.setRequestProperty("If-Modified-Since", previousLastModified);
            }

            int status = connection.getResponseCode();

            if (status == HttpURLConnection.HTTP_NOT_MODIFIED) {
                return FetchResult.notModified(status);
            }
            if (status < 200 || status >= 300) {
                return FetchResult.error("HTTP " + status + " fetching " + url);
            }

            byte[] body = readBody(connection);
            String etag = connection.getHeaderField("ETag");
            String lastModified = connection.getHeaderField("Last-Modified");
            return FetchResult.success(status, body, etag, lastModified);

        } catch (IOException e) {
            return FetchResult.error(e.getMessage() != null ? e.getMessage() : "Network error");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private byte[] readBody(HttpURLConnection connection) throws IOException {
        String encoding = connection.getContentEncoding();
        InputStream raw = connection.getInputStream();
        InputStream in = "gzip".equalsIgnoreCase(encoding) ? new GZIPInputStream(raw) : raw;

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
        in.close();
        return buffer.toByteArray();
    }
}
