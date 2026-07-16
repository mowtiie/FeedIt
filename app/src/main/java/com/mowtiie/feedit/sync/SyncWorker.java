package com.mowtiie.feedit.sync;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import android.content.Context;

import com.mowtiie.feedit.data.ArticleDao;
import com.mowtiie.feedit.data.DatabaseHelper;
import com.mowtiie.feedit.data.FeedDao;
import com.mowtiie.feedit.model.Article;
import com.mowtiie.feedit.model.Feed;
import com.mowtiie.feedit.net.FeedFetcher;
import com.mowtiie.feedit.net.FetchResult;
import com.mowtiie.feedit.parser.FeedParser;
import com.mowtiie.feedit.parser.ParsedArticle;
import com.mowtiie.feedit.parser.ParsedFeedMeta;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public class SyncWorker extends Worker {

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(getApplicationContext());
        FeedDao feedDao = new FeedDao(dbHelper);
        ArticleDao articleDao = new ArticleDao(dbHelper);
        FeedFetcher fetcher = new FeedFetcher();
        FeedParser parser = new FeedParser();

        List<FeedSyncResult> notifyResults = new ArrayList<>();

        for (Feed feed : feedDao.getAllFeeds()) {
            try {
                syncOneFeed(feed, feedDao, articleDao, fetcher, parser, notifyResults);
            } catch (Exception e) {
            }
        }

        if (!notifyResults.isEmpty()) {
            NotificationHelper.notifyNewArticles(getApplicationContext(), notifyResults);
        }

        return Result.success();
    }

    private void syncOneFeed(Feed feed, FeedDao feedDao, ArticleDao articleDao,
                              FeedFetcher fetcher, FeedParser parser,
                              List<FeedSyncResult> notifyResults) throws Exception {
        FetchResult fetchResult = fetcher.fetch(feed.getUrl(), feed.getEtag(), feed.getLastModified());

        if (fetchResult.isNotModified()) {
            feed.setLastFetched(System.currentTimeMillis());
            feedDao.updateFeed(feed);
            return;
        }
        if (fetchResult.isError()) {
            return;
        }

        ParsedFeedMeta meta = parser.parse(new ByteArrayInputStream(fetchResult.getBody()));

        feed.setEtag(fetchResult.getNewEtag());
        feed.setLastModified(fetchResult.getNewLastModified());
        feed.setLastFetched(System.currentTimeMillis());
        if (feed.getImageUrl() == null && meta.getImageUrl() != null) {
            feed.setImageUrl(meta.getImageUrl());
        }
        feedDao.updateFeed(feed);

        int newCount = 0;
        long now = System.currentTimeMillis();
        for (ParsedArticle parsedArticle : meta.getArticles()) {
            Article article = new Article();
            article.setFeedId(feed.getId());
            article.setGuid(parsedArticle.resolveDedupKey());
            article.setTitle(parsedArticle.getTitle());
            article.setLink(parsedArticle.getLink());
            article.setAuthor(parsedArticle.getAuthor());
            article.setSummary(parsedArticle.getSummary());
            article.setContent(parsedArticle.getContent());
            article.setImageUrl(parsedArticle.getImageUrl());
            article.setPublishedAt(parsedArticle.getPublishedAt());
            article.setFetchedAt(now);

            if (articleDao.insertOrIgnore(article) != -1) {
                newCount++;
            }
        }

        if (newCount > 0 && feed.isNotifyNew()) {
            notifyResults.add(new FeedSyncResult(feed.getId(), feed.getTitle(), newCount));
        }
    }
}
