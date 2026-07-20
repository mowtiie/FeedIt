package com.mowtiie.feedit.parser;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FeedParser {

    private static final String NS_MEDIA = "http://search.yahoo.com/mrss/";
    private static final String NS_CONTENT = "http://purl.org/rss/1.0/modules/content/";
    private static final String NS_ATOM = "http://www.w3.org/2005/Atom";

    private static final int MAX_ARTICLES = 15;

    private static final Pattern FIRST_IMG = Pattern.compile(
            "<img[^>]+src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    public ParsedFeedMeta parse(InputStream input) throws XmlPullParserException, IOException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(input, null);

        do {
            parser.next();
        } while (parser.getEventType() != XmlPullParser.START_TAG);

        String rootName = parser.getName();
        if ("rss".equalsIgnoreCase(rootName)) {
            return parseRss(parser);
        } else if ("feed".equalsIgnoreCase(rootName)) {
            return parseAtom(parser);
        } else {
            throw new XmlPullParserException("Unrecognized feed root element: " + rootName);
        }
    }

    private ParsedFeedMeta parseRss(XmlPullParser parser) throws XmlPullParserException, IOException {
        ParsedFeedMeta meta = new ParsedFeedMeta();
        int depth = 1;
        while (depth != 0) {
            int event = parser.next();
            if (event == XmlPullParser.END_DOCUMENT) {
                break;
            }
            if (event == XmlPullParser.END_TAG) {
                depth--;
                continue;
            }
            if (event != XmlPullParser.START_TAG) {
                continue;
            }
            depth++;
            String name = parser.getName();
            if ("channel".equals(name)) {
                depth--;
            } else if ("title".equals(name) && meta.getTitle() == null) {
                meta.setTitle(readText(parser));
                depth--;
            } else if ("link".equals(name) && meta.getSiteUrl() == null) {
                meta.setSiteUrl(readText(parser));
                depth--;
            } else if ("description".equals(name) && meta.getDescription() == null) {
                meta.setDescription(readText(parser));
                depth--;
            } else if ("image".equals(name)) {
                meta.setImageUrl(readChannelImageUrl(parser));
                depth--;
            } else if ("item".equals(name)) {
                ParsedArticle item = parseRssItem(parser);
                if (meta.getArticles().size() < MAX_ARTICLES) {
                    meta.getArticles().add(item);
                }
                depth--;
            } else {
                skip(parser);
                depth--;
            }
        }
        return meta;
    }

    private String readChannelImageUrl(XmlPullParser parser) throws XmlPullParserException, IOException {
        String url = null;
        int depth = 1;
        while (depth != 0) {
            int event = parser.next();
            if (event == XmlPullParser.END_DOCUMENT) {
                break;
            }
            if (event == XmlPullParser.END_TAG) {
                depth--;
                continue;
            }
            if (event != XmlPullParser.START_TAG) {
                continue;
            }
            depth++;
            if ("url".equals(parser.getName())) {
                url = readText(parser);
            } else {
                skip(parser);
            }
            depth--;
        }
        return url;
    }

    private ParsedArticle parseRssItem(XmlPullParser parser) throws XmlPullParserException, IOException {
        ParsedArticle article = new ParsedArticle();
        String enclosureImageUrl = null;
        String mediaImageUrl = null;

        int depth = 1;
        while (depth != 0) {
            int event = parser.next();
            if (event == XmlPullParser.END_DOCUMENT) {
                break;
            }
            if (event == XmlPullParser.END_TAG) {
                depth--;
                continue;
            }
            if (event != XmlPullParser.START_TAG) {
                continue;
            }
            depth++;
            String name = parser.getName();
            String namespace = parser.getNamespace();

            if ("title".equals(name)) {
                article.setTitle(readText(parser));
                depth--;
            } else if ("link".equals(name)) {
                article.setLink(readText(parser));
                depth--;
            } else if ("guid".equals(name)) {
                article.setGuid(readText(parser));
                depth--;
            } else if ("author".equals(name) || "creator".equals(name)) {
                article.setAuthor(readText(parser));
                depth--;
            } else if ("description".equals(name)) {
                article.setSummary(readText(parser));
                depth--;
            } else if ("encoded".equals(name) && NS_CONTENT.equals(namespace)) {
                article.setContent(readText(parser));
                depth--;
            } else if ("pubDate".equals(name)) {
                article.setPublishedAt(DateUtils.parse(readText(parser)));
                depth--;
            } else if ("enclosure".equals(name)) {
                String type = parser.getAttributeValue(null, "type");
                String url = parser.getAttributeValue(null, "url");
                if (type != null && type.startsWith("image/")) {
                    enclosureImageUrl = url;
                }
                skip(parser);
                depth--;
            } else if ("group".equals(name) && NS_MEDIA.equals(namespace)) {
                // Do nothing
            } else if (("thumbnail".equals(name) || "content".equals(name)) && NS_MEDIA.equals(namespace)) {
                String url = parser.getAttributeValue(null, "url");
                String medium = parser.getAttributeValue(null, "medium");
                String type = parser.getAttributeValue(null, "type");
                boolean looksLikeImage = "image".equals(medium)
                        || (type != null && type.startsWith("image/"))
                        || "thumbnail".equals(name);
                if (mediaImageUrl == null && url != null && looksLikeImage) {
                    mediaImageUrl = url;
                }
                skip(parser);
                depth--;
            } else {
                skip(parser);
                depth--;
            }
        }

        article.setImageUrl(resolveArticleImage(mediaImageUrl, enclosureImageUrl, article.getContent(), article.getSummary()));
        return article;
    }

    private ParsedFeedMeta parseAtom(XmlPullParser parser) throws XmlPullParserException, IOException {
        ParsedFeedMeta meta = new ParsedFeedMeta();
        int depth = 1;
        while (depth != 0) {
            int event = parser.next();
            if (event == XmlPullParser.END_DOCUMENT) {
                break;
            }
            if (event == XmlPullParser.END_TAG) {
                depth--;
                continue;
            }
            if (event != XmlPullParser.START_TAG) {
                continue;
            }
            depth++;
            String name = parser.getName();
            if ("title".equals(name) && meta.getTitle() == null) {
                meta.setTitle(readText(parser));
                depth--;
            } else if ("subtitle".equals(name) && meta.getDescription() == null) {
                meta.setDescription(readText(parser));
                depth--;
            } else if ("logo".equals(name) || "icon".equals(name)) {
                if (meta.getImageUrl() == null) {
                    meta.setImageUrl(readText(parser));
                } else {
                    skip(parser);
                }
                depth--;
            } else if ("link".equals(name)) {
                String rel = parser.getAttributeValue(null, "rel");
                String href = parser.getAttributeValue(null, "href");
                if ((rel == null || "alternate".equals(rel)) && meta.getSiteUrl() == null) {
                    meta.setSiteUrl(href);
                }
                skip(parser);
                depth--;
            } else if ("entry".equals(name)) {
                ParsedArticle entry = parseAtomEntry(parser);
                if (meta.getArticles().size() < MAX_ARTICLES) {
                    meta.getArticles().add(entry);
                }
                depth--;
            } else {
                skip(parser);
                depth--;
            }
        }
        return meta;
    }

    private ParsedArticle parseAtomEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
        ParsedArticle article = new ParsedArticle();
        String mediaImageUrl = null;
        String enclosureImageUrl = null;

        int depth = 1;
        while (depth != 0) {
            int event = parser.next();
            if (event == XmlPullParser.END_DOCUMENT) {
                break;
            }
            if (event == XmlPullParser.END_TAG) {
                depth--;
                continue;
            }
            if (event != XmlPullParser.START_TAG) {
                continue;
            }
            depth++;
            String name = parser.getName();
            String namespace = parser.getNamespace();

            if ("title".equals(name)) {
                article.setTitle(readText(parser));
                depth--;
            } else if ("id".equals(name)) {
                article.setGuid(readText(parser));
                depth--;
            } else if ("link".equals(name)) {
                String rel = parser.getAttributeValue(null, "rel");
                String type = parser.getAttributeValue(null, "type");
                String href = parser.getAttributeValue(null, "href");
                if (rel == null || "alternate".equals(rel)) {
                    article.setLink(href);
                } else if ("enclosure".equals(rel) && type != null && type.startsWith("image/")) {
                    enclosureImageUrl = href;
                }
                skip(parser);
                depth--;
            } else if ("name".equals(name)) {
                if (article.getAuthor() == null) {
                    article.setAuthor(readText(parser));
                } else {
                    skip(parser);
                }
                depth--;
            } else if ("summary".equals(name)) {
                article.setSummary(readText(parser));
                depth--;
            } else if ("content".equals(name) && NS_ATOM.equals(namespace)) {
                article.setContent(readText(parser));
                depth--;
            } else if ("published".equals(name) || "updated".equals(name)) {
                Long parsed = DateUtils.parse(readText(parser));
                depth--;
                if (article.getPublishedAt() == null || "published".equals(name)) {
                    article.setPublishedAt(parsed);
                }
            } else if ("group".equals(name) && NS_MEDIA.equals(namespace)) {
                // Do nothing
            } else if (("thumbnail".equals(name) || "content".equals(name)) && NS_MEDIA.equals(namespace)) {
                String url = parser.getAttributeValue(null, "url");
                String medium = parser.getAttributeValue(null, "medium");
                String type = parser.getAttributeValue(null, "type");
                boolean isImage = "thumbnail".equals(name)
                        || "image".equals(medium)
                        || (type != null && type.startsWith("image/"));
                if (url != null && isImage && (mediaImageUrl == null || "thumbnail".equals(name))) {
                    mediaImageUrl = url;
                }
                skip(parser);
                depth--;
            } else {
                skip(parser);
                depth--;
            }
        }

        article.setImageUrl(resolveArticleImage(mediaImageUrl, enclosureImageUrl, article.getContent(), article.getSummary()));
        return article;
    }

    private String resolveArticleImage(String mediaImageUrl, String enclosureImageUrl,
                                       String content, String summary) {
        if (mediaImageUrl != null) {
            return mediaImageUrl;
        }
        if (enclosureImageUrl != null) {
            return enclosureImageUrl;
        }
        String fromContent = extractFirstImage(content);
        if (fromContent != null) {
            return fromContent;
        }
        return extractFirstImage(summary);
    }

    private String extractFirstImage(String html) {
        if (html == null) {
            return null;
        }
        Matcher matcher = FIRST_IMG.matcher(html);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String readText(XmlPullParser parser) throws XmlPullParserException, IOException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            int event = parser.next();
            if (event == XmlPullParser.TEXT) {
                sb.append(parser.getText());
            } else if (event == XmlPullParser.START_TAG) {
                skip(parser);
            } else {
                break;
            }
        }
        return sb.toString();
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException("skip() called from a non-START_TAG event");
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
                default:
                    break;
            }
        }
    }
}