package com.mowtiie.feedit.parser;

import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class OpmlWriter {

    public void write(OutputStream output, List<OpmlExportEntry> entries) throws IOException {
        XmlSerializer serializer = Xml.newSerializer();
        serializer.setOutput(output, "UTF-8");
        serializer.startDocument("UTF-8", true);

        serializer.startTag(null, "opml");
        serializer.attribute(null, "version", "2.0");

        serializer.startTag(null, "head");
        serializer.startTag(null, "title");
        serializer.text("FeedIt subscriptions");
        serializer.endTag(null, "title");
        serializer.endTag(null, "head");

        serializer.startTag(null, "body");
        for (OpmlExportEntry entry : entries) {
            serializer.startTag(null, "outline");
            serializer.attribute(null, "type", "rss");
            serializer.attribute(null, "text", nullToEmpty(entry.getTitle()));
            serializer.attribute(null, "title", nullToEmpty(entry.getTitle()));
            serializer.attribute(null, "xmlUrl", entry.getFeedUrl());
            if (entry.getSiteUrl() != null) {
                serializer.attribute(null, "htmlUrl", entry.getSiteUrl());
            }
            if (entry.getTagNames() != null && !entry.getTagNames().isEmpty()) {
                serializer.attribute(null, "category", String.join(",", entry.getTagNames()));
            }
            serializer.endTag(null, "outline");
        }
        serializer.endTag(null, "body");

        serializer.endTag(null, "opml");
        serializer.endDocument();
        serializer.flush();
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
