package com.mowtiie.feedit.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

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
}
