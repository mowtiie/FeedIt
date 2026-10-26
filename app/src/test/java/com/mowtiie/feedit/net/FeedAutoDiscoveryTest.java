package com.mowtiie.feedit.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class FeedAutoDiscoveryTest {

    private static final String PAGE = "https://example.com/blog/";

    @Test
    public void findsRssAndAtomLinks_inAnyAttributeOrder() {
        String html = "<head>"
                + "<link rel=\"alternate\" type=\"application/rss+xml\" title=\"RSS\" href=\"/feed.xml\">"
                + "<link href='atom.xml' title='Atom' type='application/atom+xml' rel='alternate'>"
                + "<link rel=\"stylesheet\" href=\"/style.css\">"
                + "</head>";

        List<FeedAutoDiscovery.Discovery> found = FeedAutoDiscovery.parseFeedLinks(html, PAGE);

        assertEquals(2, found.size());
        assertEquals("https://example.com/feed.xml", found.get(0).feedUrl);
        assertEquals("RSS", found.get(0).title);
        assertEquals("https://example.com/blog/atom.xml", found.get(1).feedUrl);
    }

    @Test
    public void acceptsJsonFeedRdfAndGenericXmlTypes() {
        String html = "<link rel=\"alternate\" type=\"application/feed+json\" href=\"/feed.json\">"
                + "<link rel=\"alternate\" type=\"application/rdf+xml\" href=\"/rdf\">"
                + "<link rel=\"alternate\" type=\"text/xml\" href=\"/xml\">";

        assertEquals(3, FeedAutoDiscovery.parseFeedLinks(html, PAGE).size());
    }

    @Test
    public void acceptsRelWithMultipleTokens_andDecodesAmpersands() {
        String html = "<link rel=\"alternate feed\" type=\"application/rss+xml\" href=\"/f?a=1&amp;b=2\">";

        List<FeedAutoDiscovery.Discovery> found = FeedAutoDiscovery.parseFeedLinks(html, PAGE);

        assertEquals(1, found.size());
        assertEquals("https://example.com/f?a=1&b=2", found.get(0).feedUrl);
    }

    @Test
    public void ignoresNonFeedAlternatesAndHttpIsUpgraded() {
        String html = "<link rel=\"alternate\" hreflang=\"fr\" href=\"/fr/\">"
                + "<link rel=\"alternate\" type=\"application/rss+xml\" href=\"http://feeds.example.com/a\">";

        List<FeedAutoDiscovery.Discovery> found = FeedAutoDiscovery.parseFeedLinks(html, PAGE);

        assertEquals(1, found.size());
        assertEquals("https://feeds.example.com/a", found.get(0).feedUrl);
    }

    @Test
    public void looksLikeFeed_recognisesFeedFormatsAndRejectsHtml() {
        assertTrue(FeedAutoDiscovery.looksLikeFeed("<?xml version=\"1.0\"?><rss version=\"2.0\">"));
        assertTrue(FeedAutoDiscovery.looksLikeFeed("<feed xmlns=\"http://www.w3.org/2005/Atom\">"));
        assertTrue(FeedAutoDiscovery.looksLikeFeed("<rdf:RDF xmlns:rdf=\"...\">"));
        assertTrue(FeedAutoDiscovery.looksLikeFeed("{ \"version\": \"https://jsonfeed.org/version/1\" }"));
        assertFalse(FeedAutoDiscovery.looksLikeFeed("<!DOCTYPE html><html><body>about our <feed></body></html>"));
        assertFalse(FeedAutoDiscovery.looksLikeFeed("{ \"hello\": \"world\" }"));
    }
}
