package com.mowtiie.feedit.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class FeedAutoDiscovery {

    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 15_000;
    private static final int PROBE_TIMEOUT_MS = 4_000;
    private static final long PROBE_BUDGET_MS = 8_000;
    private static final int MAX_HTML_BYTES = 500 * 1024;
    private static final int SNIFF_BYTES = 2048;

    /** Paths tried when a page advertises no feed, in order of how common they are. */
    private static final String[] COMMON_FEED_PATHS = {
            "/feed", "/rss", "/rss.xml", "/atom.xml", "/feed.xml", "/index.xml", "/feed/", "/rss/"
    };

    private static final Pattern LINK_TAG = Pattern.compile(
            "<link\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern REL_ALTERNATE = Pattern.compile(
            "rel=[\"'][^\"']*\\balternate\\b[^\"']*[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern TYPE_FEED = Pattern.compile(
            "type=[\"'](application/(rss|atom|rdf)\\+xml|application/feed\\+json|text/xml|application/xml)[\"']",
            Pattern.CASE_INSENSITIVE);
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
            connection = SafeHttp.open(pageUrl, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS, requestHeaders());
            if (connection.getResponseCode() / 100 == 2) {
                String html = readLimited(connection, MAX_HTML_BYTES);
                if (looksLikeFeed(html)) {
                    return results;
                }
                results.addAll(parseFeedLinks(html, connection.getURL().toString()));
            }
        } catch (IOException ignored) {
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        if (results.isEmpty()) {
            results.addAll(probeCommonPaths(pageUrl));
        }
        return results;
    }

    /** Finds {@code <link rel="alternate">} feed declarations in an HTML page. */
    static List<Discovery> parseFeedLinks(String html, String pageUrl) {
        List<Discovery> results = new ArrayList<>();
        Matcher linkMatcher = LINK_TAG.matcher(html);
        while (linkMatcher.find()) {
            String tag = linkMatcher.group();
            if (!REL_ALTERNATE.matcher(tag).find() || !TYPE_FEED.matcher(tag).find()) {
                continue;
            }
            Matcher hrefMatcher = HREF.matcher(tag);
            if (!hrefMatcher.find()) {
                continue;
            }
            String resolvedUrl = resolve(pageUrl, hrefMatcher.group(1).replace("&amp;", "&"));
            if (resolvedUrl == null) {
                continue;
            }
            Matcher titleMatcher = TITLE_ATTR.matcher(tag);
            results.add(new Discovery(resolvedUrl, titleMatcher.find() ? titleMatcher.group(1) : null));
        }
        return results;
    }

    /** True when the start of a document is RSS, Atom, RDF or JSON Feed rather than HTML. */
    static boolean looksLikeFeed(String head) {
        String start = head.length() > SNIFF_BYTES ? head.substring(0, SNIFF_BYTES) : head;
        String lower = start.toLowerCase(Locale.ROOT);
        if (lower.contains("<html") || lower.contains("<!doctype html")) {
            return false;
        }
        return lower.contains("<rss") || lower.contains("<feed") || lower.contains("<rdf:rdf")
                || (lower.trim().startsWith("{") && lower.contains("jsonfeed.org"));
    }

    private List<Discovery> probeCommonPaths(String pageUrl) {
        List<Discovery> found = new ArrayList<>();
        String origin;
        try {
            URL url = new URL(SafeHttp.upgradeToHttps(pageUrl));
            origin = url.getProtocol() + "://" + url.getAuthority();
        } catch (MalformedURLException e) {
            return found;
        }
        long deadline = System.currentTimeMillis() + PROBE_BUDGET_MS;
        for (String path : COMMON_FEED_PATHS) {
            if (System.currentTimeMillis() > deadline) {
                break;
            }
            String candidate = origin + path;
            HttpURLConnection connection = null;
            try {
                connection = SafeHttp.open(candidate, PROBE_TIMEOUT_MS, PROBE_TIMEOUT_MS, requestHeaders());
                if (connection.getResponseCode() / 100 == 2 && looksLikeFeed(readLimited(connection, SNIFF_BYTES))) {
                    found.add(new Discovery(candidate, null));
                    break;
                }
            } catch (IOException ignored) {
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        return found;
    }

    private static Map<String, String> requestHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("User-Agent", FeedFetcher.USER_AGENT);
        headers.put("Accept", "text/html, application/xhtml+xml, " + FeedFetcher.ACCEPT);
        headers.put("Accept-Encoding", "gzip");
        return headers;
    }

    private String readLimited(HttpURLConnection connection, int maxBytes) throws IOException {
        StringBuilder builder = new StringBuilder();
        byte[] chunk = new byte[8192];
        int read;
        int total = 0;
        InputStream raw = connection.getInputStream();
        try (InputStream in = "gzip".equalsIgnoreCase(connection.getContentEncoding()) ? new GZIPInputStream(raw) : raw) {
            while ((read = in.read(chunk)) != -1) {
                total += read;
                builder.append(new String(chunk, 0, read, StandardCharsets.UTF_8));
                if (total > maxBytes) {
                    break;
                }
            }
        }
        return builder.toString();
    }

    private static String resolve(String baseUrl, String href) {
        try {
            return SafeHttp.upgradeToHttps(new URL(new URL(baseUrl), href).toString());
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
