package com.mowtiie.feedit.sync;

import com.mowtiie.feedit.model.Article;
import com.mowtiie.feedit.parser.ParsedArticle;

import java.util.ArrayList;
import java.util.List;

public final class ArticleMapper {

    private ArticleMapper() {
    }

    /**
     * Maps parsed items to storable articles. Items dated before {@code minPublishedAt} are
     * dropped so retention pruning is not undone by the next sync; undated items are kept.
     */
    public static List<Article> fromParsed(long feedId, List<ParsedArticle> parsed, long now, Long minPublishedAt) {
        List<Article> result = new ArrayList<>(parsed.size());
        for (ParsedArticle p : parsed) {
            Long publishedAt = p.getPublishedAt();
            if (minPublishedAt != null && publishedAt != null && publishedAt < minPublishedAt) {
                continue;
            }
            Article article = new Article();
            article.setFeedId(feedId);
            article.setGuid(p.resolveDedupKey());
            article.setTitle(p.getTitle());
            article.setLink(p.getLink());
            article.setAuthor(p.getAuthor());
            article.setSummary(p.getSummary());
            article.setContent(p.getContent());
            article.setImageUrl(p.getImageUrl());
            article.setPublishedAt(publishedAt);
            article.setFetchedAt(now);
            result.add(article);
        }
        return result;
    }
}
