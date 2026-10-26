package com.mowtiie.feedit.parser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

/** Parses JSON Feed (https://www.jsonfeed.org) documents into the same model the XML parser produces. */
final class JsonFeedParser {

    private JsonFeedParser() {
    }

    static ParsedFeedMeta parse(String json) throws XmlPullParserException {
        JSONObject root;
        try {
            root = new JSONObject(json);
        } catch (JSONException e) {
            throw new XmlPullParserException("Invalid JSON feed: " + e.getMessage());
        }
        JSONArray items = root.optJSONArray("items");
        if (items == null) {
            throw new XmlPullParserException("Unrecognized JSON document: no feed items");
        }

        ParsedFeedMeta meta = new ParsedFeedMeta();
        meta.setTitle(FeedParser.decodePlainText(optString(root, "title")));
        meta.setSiteUrl(optString(root, "home_page_url"));
        meta.setDescription(optString(root, "description"));
        meta.setImageUrl(optString(root, "icon"));

        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) {
                continue;
            }
            meta.getArticles().add(FeedParser.withFallbackTitle(parseItem(item)));
        }
        return meta;
    }

    private static ParsedArticle parseItem(JSONObject item) {
        ParsedArticle article = new ParsedArticle();
        String url = optString(item, "url");
        String externalUrl = optString(item, "external_url");

        article.setGuid(optString(item, "id"));
        article.setLink(url != null ? url : externalUrl);
        article.setTitle(FeedParser.decodePlainText(optString(item, "title")));
        article.setSummary(optString(item, "summary"));
        article.setContent(optString(item, "content_html"));
        if (article.getSummary() == null) {
            article.setSummary(optString(item, "content_text"));
        }
        article.setAuthor(authorOf(item));

        String published = optString(item, "date_published");
        if (published == null) {
            published = optString(item, "date_modified");
        }
        article.setPublishedAt(DateUtils.parse(published));

        String image = optString(item, "image");
        if (image == null) {
            image = optString(item, "banner_image");
        }
        if (image == null) {
            image = FeedParser.extractFirstImage(article.getContent());
        }
        article.setImageUrl(image);
        return article;
    }

    private static String authorOf(JSONObject item) {
        JSONArray authors = item.optJSONArray("authors");
        JSONObject author = authors != null && authors.length() > 0 ? authors.optJSONObject(0) : item.optJSONObject("author");
        return author != null ? optString(author, "name") : null;
    }

    private static String optString(JSONObject object, String key) {
        if (object.isNull(key)) {
            return null;
        }
        String value = object.optString(key, null);
        return value != null && !value.trim().isEmpty() ? value : null;
    }
}
