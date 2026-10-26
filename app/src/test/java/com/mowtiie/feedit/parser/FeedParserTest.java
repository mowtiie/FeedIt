package com.mowtiie.feedit.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@RunWith(RobolectricTestRunner.class)
@Config(application = android.app.Application.class)
public class FeedParserTest {

    private static ParsedFeedMeta parse(String xml) throws Exception {
        return new FeedParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void rss_parsesChannelAndItems() throws Exception {
        ParsedFeedMeta meta = parse("<?xml version=\"1.0\"?><rss version=\"2.0\"><channel>"
                + "<title>Blog</title><link>https://example.com</link>"
                + "<item><title>First</title><link>https://example.com/1</link><guid>g1</guid></item>"
                + "<item><title>Second</title><link>https://example.com/2</link><guid>g2</guid></item>"
                + "</channel></rss>");

        assertEquals("Blog", meta.getTitle());
        assertEquals(2, meta.getArticles().size());
        assertEquals("First", meta.getArticles().get(0).getTitle());
        assertEquals("g2", meta.getArticles().get(1).getGuid());
    }

    @Test
    public void rss_titleEntitiesDecoded_summaryStaysRawHtml() throws Exception {
        ParsedFeedMeta meta = parse("<rss version=\"2.0\"><channel><title>T</title>"
                + "<item><title>Tom &amp;amp; Jerry</title><description>&lt;p&gt;Hi&lt;/p&gt;</description></item>"
                + "</channel></rss>");

        ParsedArticle article = meta.getArticles().get(0);
        assertEquals("Tom & Jerry", article.getTitle());
        assertEquals("<p>Hi</p>", article.getSummary());
    }

    @Test
    public void atom_youtubeThumbnailInsideMediaGroup() throws Exception {
        ParsedFeedMeta meta = parse("<feed xmlns=\"http://www.w3.org/2005/Atom\" "
                + "xmlns:media=\"http://search.yahoo.com/mrss/\"><title>Channel</title>"
                + "<entry><title>Video</title><id>yt:1</id>"
                + "<link rel=\"alternate\" href=\"https://youtube.com/watch?v=1\"/>"
                + "<media:group><media:title>Video</media:title>"
                + "<media:thumbnail url=\"https://i.ytimg.com/vi/1/hq.jpg\" width=\"480\" height=\"360\"/>"
                + "</media:group></entry></feed>");

        assertEquals(1, meta.getArticles().size());
        assertEquals("https://i.ytimg.com/vi/1/hq.jpg", meta.getArticles().get(0).getImageUrl());
    }

    @Test
    public void atom_elementsAfterReadTextStayInSync() throws Exception {
        ParsedFeedMeta meta = parse("<feed xmlns=\"http://www.w3.org/2005/Atom\"><title>F</title>"
                + "<entry><title>A</title><id>1</id><summary>s</summary><content>c</content></entry>"
                + "<entry><title>B</title><id>2</id></entry></feed>");

        assertEquals(2, meta.getArticles().size());
        assertNotNull(meta.getArticles().get(1).getGuid());
        assertEquals("B", meta.getArticles().get(1).getTitle());
    }

    @Test
    public void truncatedXml_throwsInsteadOfLooping() {
        assertThrows(XmlPullParserException.class,
                () -> parse("<rss version=\"2.0\"><channel><title>T</title><item><title>cut"));
    }

    @Test
    public void unknownRoot_throws() {
        assertThrows(XmlPullParserException.class, () -> parse("<html><body/></html>"));
    }

    @Test
    public void rss1Rdf_itemsAfterChannelAreParsed() throws Exception {
        ParsedFeedMeta meta = parse("<?xml version=\"1.0\"?><rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "
                + "xmlns=\"http://purl.org/rss/1.0/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">"
                + "<channel rdf:about=\"http://example.com/\"><title>Old School</title><link>http://example.com/</link>"
                + "<items><rdf:Seq><rdf:li rdf:resource=\"http://example.com/1\"/></rdf:Seq></items></channel>"
                + "<item rdf:about=\"http://example.com/1\"><title>One</title><link>http://example.com/1</link>"
                + "<dc:date>2026-10-20T10:00:00Z</dc:date></item>"
                + "<item rdf:about=\"http://example.com/2\"><title>Two</title><link>http://example.com/2</link></item>"
                + "</rdf:RDF>");

        assertEquals("Old School", meta.getTitle());
        assertEquals("http://example.com/", meta.getSiteUrl());
        assertEquals(2, meta.getArticles().size());
        assertEquals("One", meta.getArticles().get(0).getTitle());
        assertEquals("http://example.com/1", meta.getArticles().get(0).resolveDedupKey());
        assertNotNull(meta.getArticles().get(0).getPublishedAt());
    }

    @Test
    public void rss_emptyAtomLinkDoesNotClobberRealLink() throws Exception {
        ParsedFeedMeta meta = parse("<rss version=\"2.0\" xmlns:atom=\"http://www.w3.org/2005/Atom\"><channel>"
                + "<atom:link href=\"https://example.com/feed\" rel=\"self\"/><title>T</title><link>https://example.com</link>"
                + "<item><title>A</title><link>https://example.com/a</link>"
                + "<atom:link href=\"https://example.com/a\" rel=\"standout\"></atom:link></item></channel></rss>");

        assertEquals("https://example.com", meta.getSiteUrl());
        assertEquals("https://example.com/a", meta.getArticles().get(0).getLink());
    }

    @Test
    public void rss_atomLinkIsUsedWhenItemHasNoPlainLink() throws Exception {
        ParsedFeedMeta meta = parse("<rss version=\"2.0\" xmlns:atom=\"http://www.w3.org/2005/Atom\"><channel><title>T</title>"
                + "<item><title>A</title><atom:link href=\"https://example.com/a\"/></item></channel></rss>");

        assertEquals("https://example.com/a", meta.getArticles().get(0).getLink());
    }

    @Test
    public void rss_foreignNamespacedTitleAndDescriptionAreIgnored() throws Exception {
        ParsedFeedMeta meta = parse("<rss version=\"2.0\" xmlns:itunes=\"http://www.itunes.com/dtds/podcast-1.0.dtd\" "
                + "xmlns:media=\"http://search.yahoo.com/mrss/\"><channel><title>T</title>"
                + "<item><title>Real</title><itunes:title>Other</itunes:title><description>real summary</description>"
                + "<media:description>media text</media:description></item></channel></rss>");

        ParsedArticle article = meta.getArticles().get(0);
        assertEquals("Real", article.getTitle());
        assertEquals("real summary", article.getSummary());
    }

    @Test
    public void rss_titleLessItemGetsTitleFromText() throws Exception {
        ParsedFeedMeta meta = parse("<rss version=\"2.0\"><channel><title>Fediverse</title>"
                + "<item><link>https://example.com/p/1</link><description>&lt;p&gt;Hello &lt;b&gt;world&lt;/b&gt;, this is a post&lt;/p&gt;</description></item>"
                + "</channel></rss>");

        assertEquals("Hello world, this is a post", meta.getArticles().get(0).getTitle());
    }

    @Test
    public void rss_longTitleLessItemIsTruncated() throws Exception {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            text.append("word ");
        }
        ParsedFeedMeta meta = parse("<rss version=\"2.0\"><channel><title>T</title>"
                + "<item><description>" + text + "</description></item></channel></rss>");

        String title = meta.getArticles().get(0).getTitle();
        assertTrue(title.endsWith("…"));
        assertTrue(title.length() <= 92);
    }

    @Test
    public void rss_dcDateIsUsedWhenPubDateMissing() throws Exception {
        ParsedFeedMeta meta = parse("<rss version=\"2.0\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\"><channel><title>T</title>"
                + "<item><title>A</title><dc:date>2026-10-20T10:00:00Z</dc:date></item></channel></rss>");

        assertNotNull(meta.getArticles().get(0).getPublishedAt());
    }

    @Test
    public void jsonFeed_isParsed() throws Exception {
        ParsedFeedMeta meta = parse("{\"version\":\"https://jsonfeed.org/version/1.1\",\"title\":\"JSON Blog\","
                + "\"home_page_url\":\"https://example.com/\",\"items\":["
                + "{\"id\":\"1\",\"url\":\"https://example.com/1\",\"title\":\"First\",\"content_html\":\"<p>Hi <img src=\\\"https://example.com/i.png\\\"></p>\","
                + "\"date_published\":\"2026-10-20T10:00:00Z\",\"authors\":[{\"name\":\"Ann\"}]},"
                + "{\"id\":\"2\",\"url\":\"https://example.com/2\",\"content_text\":\"No title here\"}]}");

        assertEquals("JSON Blog", meta.getTitle());
        assertEquals("https://example.com/", meta.getSiteUrl());
        assertEquals(2, meta.getArticles().size());
        ParsedArticle first = meta.getArticles().get(0);
        assertEquals("First", first.getTitle());
        assertEquals("Ann", first.getAuthor());
        assertEquals("https://example.com/i.png", first.getImageUrl());
        assertNotNull(first.getPublishedAt());
        assertEquals("No title here", meta.getArticles().get(1).getTitle());
    }

    @Test
    public void json_withoutItemsIsRejected() {
        assertThrows(XmlPullParserException.class, () -> parse("{\"hello\":\"world\"}"));
    }
}
