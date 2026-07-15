package com.mowtiie.feedit.parser;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class OpmlParser {

    public List<OpmlEntry> parse(InputStream input) throws XmlPullParserException, IOException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(input, null);

        List<OpmlEntry> entries = new ArrayList<>();
        Deque<String> folderStack = new ArrayDeque<>();
        Deque<Boolean> outlineIsFolder = new ArrayDeque<>();

        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && "outline".equals(parser.getName())) {
                String xmlUrl = parser.getAttributeValue(null, "xmlUrl");
                String title = firstNonNull(
                        parser.getAttributeValue(null, "title"),
                        parser.getAttributeValue(null, "text"));

                if (xmlUrl != null) {
                    OpmlEntry entry = new OpmlEntry();
                    entry.setFeedUrl(xmlUrl);
                    entry.setTitle(title);
                    entry.setSiteUrl(parser.getAttributeValue(null, "htmlUrl"));
                    entry.getTagNames().addAll(folderStack);
                    addCategoryAttribute(parser, entry.getTagNames());
                    entries.add(entry);
                    outlineIsFolder.push(false);
                } else if (title != null) {
                    folderStack.push(title);
                    outlineIsFolder.push(true);
                } else {
                    outlineIsFolder.push(false);
                }
            } else if (event == XmlPullParser.END_TAG && "outline".equals(parser.getName())) {
                if (!outlineIsFolder.isEmpty() && Boolean.TRUE.equals(outlineIsFolder.pop())) {
                    folderStack.pop();
                }
            }
            event = parser.next();
        }
        return entries;
    }

    private void addCategoryAttribute(XmlPullParser parser, List<String> tagNames) {
        String category = parser.getAttributeValue(null, "category");
        if (category == null || category.trim().isEmpty()) {
            return;
        }
        for (String raw : category.split(",")) {
            String trimmed = raw.trim();
            if (!trimmed.isEmpty() && !tagNames.contains(trimmed)) {
                tagNames.add(trimmed);
            }
        }
    }

    private String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }
}
