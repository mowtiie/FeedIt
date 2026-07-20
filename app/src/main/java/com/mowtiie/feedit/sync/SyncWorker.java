package com.mowtiie.feedit.sync;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SyncWorker extends Worker {

    public static final String KEY_IS_MANUAL = "is_manual";

    private static final int OUTCOME_NOT_MODIFIED = -2;
    private static final int OUTCOME_FETCH_FAILED = -1;

    private static final int MAX_CONCURRENT_FEEDS = 4;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    private static final class FeedOutcome {
        final int outcome;
        @Nullable final FeedSyncResult notifyResult;
        @Nullable final Exception error;

        FeedOutcome(int outcome, @Nullable FeedSyncResult notifyResult, @Nullable Exception error) {
            this.outcome = outcome;
            this.notifyResult = notifyResult;
            this.error = error;
        }
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
        if (feeds.isEmpty()) {
            SyncLog.d("=== sync run START — no feeds, nothing to do ===");
            return Result.success();
        }

        int threadCount = Math.min(MAX_CONCURRENT_FEEDS, feeds.size());
        SyncLog.d("=== sync run START — " + feeds.size() + " feeds, " + threadCount + " at a time ===");

        List<FeedSyncResult> notifyResults;
        int ok;
        int notModified;
        int failed;
        int totalNewArticles;
        try (ExecutorService pool = Executors.newFixedThreadPool(threadCount)) {
            notifyResults = new ArrayList<>();
            ok = 0;
            notModified = 0;
            failed = 0;
            totalNewArticles = 0;

            try {
                List<Callable<FeedOutcome>> tasks = new ArrayList<>();
                for (int i = 0; i < feeds.size(); i++) {
                    Feed feed = feeds.get(i);
                    String label = "[" + (i + 1) + "/" + feeds.size() + "] " + feed.getUrl();
                    tasks.add(() -> runOneFeed(feed, label, feedDao, articleDao, fetcher, parser));
                }

                List<Future<FeedOutcome>> futures = pool.invokeAll(tasks);

                for (Future<FeedOutcome> future : futures) {
                    FeedOutcome result;
                    try {
                        result = future.get();
                    } catch (ExecutionException e) {
                        failed++;
                        SyncLog.e("task failed unexpectedly", e);
                        continue;
                    }

                    if (result.error != null || result.outcome == OUTCOME_FETCH_FAILED) {
                        failed++;
                    } else if (result.outcome == OUTCOME_NOT_MODIFIED) {
                        notModified++;
                    } else {
                        ok++;
                        totalNewArticles += result.outcome;
                    }
                    if (result.notifyResult != null) {
                        notifyResults.add(result.notifyResult);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                SyncLog.w("sync interrupted — requesting retry");
                return Result.retry();
            } finally {
                pool.shutdownNow();
            }
        }

        if (!notifyResults.isEmpty()) {
            NotificationHelper.notifyNewArticles(getApplicationContext(), notifyResults);
        }

        boolean isManual = getInputData().getBoolean(KEY_IS_MANUAL, false);
        if (isManual) {
            NotificationHelper.notifySyncStatus(getApplicationContext(), totalNewArticles, failed);
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

    private FeedOutcome runOneFeed(Feed feed, String label, FeedDao feedDao, ArticleDao articleDao,
                                   FeedFetcher fetcher, FeedParser parser) {
        long feedStart = System.currentTimeMillis();
        SyncLog.d("--> " + label);
        List<FeedSyncResult> collected = new ArrayList<>(1);
        try {
            int outcome = syncOneFeed(feed, label, feedDao, articleDao, fetcher, parser, collected);
            long elapsed = System.currentTimeMillis() - feedStart;
            if (outcome == OUTCOME_NOT_MODIFIED) {
                SyncLog.d("<-- " + label + " : 304 not-modified (" + elapsed + "ms)");
            } else if (outcome == OUTCOME_FETCH_FAILED) {
                SyncLog.w("<-- " + label + " : FETCH FAILED (" + elapsed + "ms)");
            } else {
                SyncLog.d("<-- " + label + " : ok, " + outcome + " new (" + elapsed + "ms)");
            }
            return new FeedOutcome(outcome, collected.isEmpty() ? null : collected.get(0), null);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - feedStart;
            SyncLog.e("<-- " + label + " : EXCEPTION after " + elapsed + "ms", e);
            return new FeedOutcome(OUTCOME_FETCH_FAILED, null, e);
        }
    }

    private int syncOneFeed(Feed feed, String label, FeedDao feedDao, ArticleDao articleDao, FeedFetcher fetcher, FeedParser parser, List<FeedSyncResult> notifyResults) throws Exception {
        long t0 = System.currentTimeMillis();
        FetchResult fetchResult = fetcher.fetch(feed.getUrl(), feed.getEtag(), feed.getLastModified());
        SyncLog.d("    " + label + " fetch() returned in " + (System.currentTimeMillis() - t0) + "ms"
                + " (hadEtag=" + (feed.getEtag() != null) + ")");

        if (fetchResult.isNotModified()) {
            feed.setLastFetched(System.currentTimeMillis());
            feedDao.updateFeed(feed);
            return OUTCOME_NOT_MODIFIED;
        }
        if (fetchResult.isError()) {
            SyncLog.w("    " + label + " fetch error: " + fetchResult.getErrorMessage());
            return OUTCOME_FETCH_FAILED;
        }

        long parseStart = System.currentTimeMillis();
        ParsedFeedMeta meta = parser.parse(new ByteArrayInputStream(fetchResult.getBody()));
        SyncLog.d("    " + label + " parsed " + meta.getArticles().size() + " items in "
                + (System.currentTimeMillis() - parseStart) + "ms");

        feed.setEtag(fetchResult.getNewEtag());
        feed.setLastModified(fetchResult.getNewLastModified());
        feed.setLastFetched(System.currentTimeMillis());
        if (feed.getImageUrl() == null && meta.getImageUrl() != null) {
            feed.setImageUrl(meta.getImageUrl());
        }
        feedDao.updateFeed(feed);

        List<Article> parsedArticles = new ArrayList<>();
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
            parsedArticles.add(article);
        }

        long insertStart = System.currentTimeMillis();
        int newCount = articleDao.insertAllOrIgnore(parsedArticles);
        SyncLog.d("    " + label + " inserted " + newCount + " new of " + parsedArticles.size()
                + " in " + (System.currentTimeMillis() - insertStart) + "ms");

        if (newCount > 0 && feed.isNotifyNew()) {
            notifyResults.add(new FeedSyncResult(feed.getId(), feed.getTitle(), newCount));
        }
        return newCount;
    }
}