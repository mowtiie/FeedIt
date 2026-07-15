package com.mowtiie.feedit.net;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FeedAutoDiscovery {

    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 15_000;
    private static final int MAX_HTML_BYTES = 500 * 1024;

    private static final Pattern LINK_TAG = Pattern.compile(
            "<link\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern REL_ALTERNATE = Pattern.compile(
            "rel=[\"']alternate[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern TYPE_FEED = Pattern.compile(
            "type=[\"'](application/(rss|atom)\\+xml)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern HREF = Pattern.compile(
            "href=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern TITLE_ATTR = Pattern.compile(
            "title=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    public static class Discovery {
        public final String feedUrl;
        public final String title;

        Discovery(String feedUrl, String title) {
            this.feedUrl = feedUrl;
            this.title = title;
        }
    }

    public List<Discovery> discover(String pageUrl) {
        List<Discovery> results = new ArrayList<>();
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(pageUrl).openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", "FeedIt/1.0 (Android RSS reader)");

            String html = readLimited(connection);
            Matcher linkMatcher = LINK_TAG.matcher(html);

            while (linkMatcher.find()) {
                String tag = linkMatcher.group();
                if (!REL_ALTERNATE.matcher(tag).find()) {
                    continue;
                }
                Matcher typeMatcher = TYPE_FEED.matcher(tag);
                if (!typeMatcher.find()) {
                    continue;
                }
                Matcher hrefMatcher = HREF.matcher(tag);
                if (!hrefMatcher.find()) {
                    continue;
                }
                String resolvedUrl = resolve(pageUrl, hrefMatcher.group(1));
                if (resolvedUrl == null) {
                    continue;
                }
                Matcher titleMatcher = TITLE_ATTR.matcher(tag);
                String title = titleMatcher.find() ? titleMatcher.group(1) : null;
                results.add(new Discovery(resolvedUrl, title));
            }
        } catch (IOException ignored) {
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return results;
    }

    private String readLimited(HttpURLConnection connection) throws IOException {
        StringBuilder builder = new StringBuilder();
        byte[] chunk = new byte[8192];
        int read;
        int total = 0;
        try (java.io.InputStream in = connection.getInputStream()) {
            while ((read = in.read(chunk)) != -1) {
                total += read;
                builder.append(new String(chunk, 0, read, StandardCharsets.UTF_8));
                if (total > MAX_HTML_BYTES) {
                    break;
                }
            }
        }
        return builder.toString();
    }

    private String resolve(String baseUrl, String href) {
        try {
            return new URL(new URL(baseUrl), href).toString();
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
