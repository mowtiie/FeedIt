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

    private static final int OUTCOME_NOT_MODIFIED = -2;
    private static final int OUTCOME_FETCH_FAILED = -1;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        long runStart = System.currentTimeMillis();
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(getApplicationContext());
        FeedDao feedDao = new FeedDao(dbHelper);
        ArticleDao articleDao = new ArticleDao(dbHelper);
        FeedFetcher fetcher = new FeedFetcher();
        FeedParser parser = new FeedParser();

        List<Feed> feeds = feedDao.getAllFeeds();
        SyncLog.d("=== sync run START — " + feeds.size() + " feeds to process ===");

        List<FeedSyncResult> notifyResults = new ArrayList<>();
        int ok = 0, notModified = 0, failed = 0;

        for (int i = 0; i < feeds.size(); i++) {
            Feed feed = feeds.get(i);
            long feedStart = System.currentTimeMillis();
            String label = "[" + (i + 1) + "/" + feeds.size() + "] " + feed.getUrl();
            SyncLog.d("--> " + label);
            try {
                int outcome = syncOneFeed(feed, feedDao, articleDao, fetcher, parser, notifyResults);
                long elapsed = System.currentTimeMillis() - feedStart;
                if (outcome == OUTCOME_NOT_MODIFIED) {
                    notModified++;
                    SyncLog.d("<-- " + label + " : 304 not-modified (" + elapsed + "ms)");
                } else if (outcome == OUTCOME_FETCH_FAILED) {
                    failed++;
                    SyncLog.w("<-- " + label + " : FETCH FAILED (" + elapsed + "ms)");
                } else {
                    ok++;
                    SyncLog.d("<-- " + label + " : ok, " + outcome + " new (" + elapsed + "ms)");
                }
            } catch (Exception e) {
                failed++;
                long elapsed = System.currentTimeMillis() - feedStart;
                SyncLog.e("<-- " + label + " : EXCEPTION after " + elapsed + "ms", e);
            }
        }

        if (!notifyResults.isEmpty()) {
            NotificationHelper.notifyNewArticles(getApplicationContext(), notifyResults);
        }

        long totalElapsed = System.currentTimeMillis() - runStart;
        SyncLog.d("=== sync run END — ok=" + ok + " notModified=" + notModified
                + " failed=" + failed + " totalTime=" + totalElapsed + "ms ===");
        if (totalElapsed > 9 * 60 * 1000) {
            SyncLog.w("Run took over 9 min — approaching WorkManager's 10-min limit; "
                    + "the OS may kill long runs before they finish.");
        }
        return Result.success();
    }

    private int syncOneFeed(Feed feed, FeedDao feedDao, ArticleDao articleDao,
                            FeedFetcher fetcher, FeedParser parser,
                            List<FeedSyncResult> notifyResults) throws Exception {
        long t0 = System.currentTimeMillis();
        FetchResult fetchResult = fetcher.fetch(feed.getUrl(), feed.getEtag(), feed.getLastModified());
        SyncLog.d("    fetch() returned in " + (System.currentTimeMillis() - t0) + "ms"
                + " (hadEtag=" + (feed.getEtag() != null) + ")");

        if (fetchResult.isNotModified()) {
            feed.setLastFetched(System.currentTimeMillis());
            feedDao.updateFeed(feed);
            return OUTCOME_NOT_MODIFIED;
        }
        if (fetchResult.isError()) {
            SyncLog.w("    fetch error: " + fetchResult.getErrorMessage());
            return OUTCOME_FETCH_FAILED;
        }

        long parseStart = System.currentTimeMillis();
        ParsedFeedMeta meta = parser.parse(new ByteArrayInputStream(fetchResult.getBody()));
        SyncLog.d("    parsed " + meta.getArticles().size() + " items in "
                + (System.currentTimeMillis() - parseStart) + "ms");

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
        return newCount;
    }
}