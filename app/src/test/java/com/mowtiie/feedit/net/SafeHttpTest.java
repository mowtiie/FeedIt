package com.mowtiie.feedit.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SafeHttpTest {

    @Test
    public void upgradeToHttps_rewritesOnlyHttp() {
        assertEquals("https://example.com/feed", SafeHttp.upgradeToHttps("http://example.com/feed"));
        assertEquals("https://example.com/feed", SafeHttp.upgradeToHttps("HTTP://example.com/feed"));
        assertEquals("https://example.com/feed", SafeHttp.upgradeToHttps("https://example.com/feed"));
        assertNull(SafeHttp.upgradeToHttps(null));
    }

    @Test
    public void resolveRedirect_upgradesAbsoluteHttpTargets() throws Exception {
        assertEquals("https://feeds.feedburner.com/blogspot/MKuf",
                SafeHttp.resolveRedirect("https://googleblog.blogspot.com/feeds/posts/default",
                        "http://feeds.feedburner.com/blogspot/MKuf"));
    }

    @Test
    public void resolveRedirect_resolvesRelativeLocations() throws Exception {
        assertEquals("https://example.com/new/feed.xml",
                SafeHttp.resolveRedirect("https://example.com/old/feed.xml", "/new/feed.xml"));
        assertEquals("https://example.com/old/other.xml",
                SafeHttp.resolveRedirect("https://example.com/old/feed.xml", "other.xml"));
    }

    @Test
    public void isRedirect_coversAllRedirectStatuses() {
        for (int status : new int[]{301, 302, 303, 307, 308}) {
            assertTrue("status " + status, SafeHttp.isRedirect(status));
        }
        assertFalse(SafeHttp.isRedirect(200));
        assertFalse(SafeHttp.isRedirect(304));
        assertFalse(SafeHttp.isRedirect(404));
    }

    @Test
    public void describeHttpError_hintsAtBlockingForCommonStatuses() {
        assertTrue(FeedFetcher.describeHttpError(403, "https://x/f").contains("blocking"));
        assertTrue(FeedFetcher.describeHttpError(429, "https://x/f").contains("blocking"));
        assertEquals("HTTP 404 fetching https://x/f", FeedFetcher.describeHttpError(404, "https://x/f"));
    }
}
