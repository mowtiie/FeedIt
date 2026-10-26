package com.mowtiie.feedit.sync;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Data;
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
import com.mowtiie.feedit.parser.ParsedFeedMeta;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SyncWorker extends Worker {

    public static final String KEY_FEED_ID = "feed_id";
    public static final String KEY_FAILED_COUNT = "failed_count";

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

        Long retentionCutoff = RetentionPolicy.cutoffMillis(getApplicationContext());
        long scopedFeedId = getInputData().getLong(KEY_FEED_ID, -1L);
        List<Feed> feeds;
        if (scopedFeedId != -1L) {
            Feed single = feedDao.getFeedById(scopedFeedId);
            feeds = single != null ? Collections.singletonList(single) : new ArrayList<>();
        } else {
            feeds = feedDao.getAllFeeds();
        }
        if (feeds.isEmpty()) {
            SyncLog.d("=== sync run START — no feeds, nothing to do ===");
            return Result.success();
        }

        int threadCount = Math.min(MAX_CONCURRENT_FEEDS, feeds.size());
        SyncLog.d("=== sync run START — " + feeds.size() + " feeds, "
                + threadCount + " at a time ===");

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        List<FeedSyncResult> notifyResults = new ArrayList<>();
        int ok = 0, notModified = 0, failed = 0;
        int totalNewArticles = 0;

        try {
            List<Callable<FeedOutcome>> tasks = new ArrayList<>();
            for (int i = 0; i < feeds.size(); i++) {
                Feed feed = feeds.get(i);
                String label = "[" + (i + 1) + "/" + feeds.size() + "] " + feed.getUrl();
                tasks.add(() -> runOneFeed(feed, label, feedDao, articleDao, fetcher, parser, retentionCutoff));
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

        if (retentionCutoff != null) {
            int pruned = articleDao.deleteOlderThan(retentionCutoff);
            if (pruned > 0) {
                SyncLog.d("pruned " + pruned + " articles older than the retention window");
            }
        }

        if (!notifyResults.isEmpty()) {
            NotificationHelper.notifyNewArticles(getApplicationContext(), notifyResults);
        }

        long totalElapsed = System.currentTimeMillis() - runStart;
        SyncLog.d("=== sync run END — ok=" + ok + " notModified=" + notModified
                + " failed=" + failed + " newArticles=" + totalNewArticles
                + " totalTime=" + totalElapsed + "ms ===");
        if (totalElapsed > 9 * 60 * 1000) {
            SyncLog.w("Run took over 9 min — approaching WorkManager's 10-min limit; "
                    + "the OS may kill long runs before they finish.");
        }
        return Result.success(new Data.Builder().putInt(KEY_FAILED_COUNT, failed).build());
    }

    private FeedOutcome runOneFeed(Feed feed, String label, FeedDao feedDao, ArticleDao articleDao,
                                   FeedFetcher fetcher, FeedParser parser, Long retentionCutoff) {
        long feedStart = System.currentTimeMillis();
        SyncLog.d("--> " + label);
        List<FeedSyncResult> collected = new ArrayList<>(1);
        try {
            int outcome = syncOneFeed(feed, label, feedDao, articleDao, fetcher, parser, retentionCutoff, collected);
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
            feedDao.setLastError(feed.getId(), e.getClass().getSimpleName()
                    + (e.getMessage() != null ? ": " + e.getMessage() : ""));
            return new FeedOutcome(OUTCOME_FETCH_FAILED, null, e);
        }
    }

    private int syncOneFeed(Feed feed, String label, FeedDao feedDao, ArticleDao articleDao,
                            FeedFetcher fetcher, FeedParser parser,
                            Long retentionCutoff, List<FeedSyncResult> notifyResults) throws Exception {
        long t0 = System.currentTimeMillis();
        FetchResult fetchResult = fetcher.fetch(feed.getUrl(), feed.getEtag(), feed.getLastModified());
        SyncLog.d("    " + label + " fetch() returned in " + (System.currentTimeMillis() - t0) + "ms"
                + " (hadEtag=" + (feed.getEtag() != null) + ")");

        if (fetchResult.isNotModified()) {
            feed.setLastError(null);
            feed.setLastFetched(System.currentTimeMillis());
            feedDao.updateFeed(feed);
            return OUTCOME_NOT_MODIFIED;
        }
        if (fetchResult.isError()) {
            SyncLog.w("    " + label + " fetch error: " + fetchResult.getErrorMessage());
            feedDao.setLastError(feed.getId(), fetchResult.getErrorMessage());
            return OUTCOME_FETCH_FAILED;
        }

        long parseStart = System.currentTimeMillis();
        ParsedFeedMeta meta = parser.parse(new ByteArrayInputStream(fetchResult.getBody()));
        SyncLog.d("    " + label + " parsed " + meta.getArticles().size() + " items in "
                + (System.currentTimeMillis() - parseStart) + "ms");

        feed.setLastError(null);
        feed.setEtag(fetchResult.getNewEtag());
        feed.setLastModified(fetchResult.getNewLastModified());
        feed.setLastFetched(System.currentTimeMillis());
        if (feed.getImageUrl() == null && meta.getImageUrl() != null) {
            feed.setImageUrl(meta.getImageUrl());
        }
        feedDao.updateFeed(feed);

        List<Article> parsedArticles = ArticleMapper.fromParsed(
                feed.getId(), meta.getArticles(), System.currentTimeMillis(), retentionCutoff);

        long insertStart = System.currentTimeMillis();
        List<Article> newArticles = articleDao.insertAllOrIgnore(parsedArticles);
        SyncLog.d("    " + label + " inserted " + newArticles.size() + " new of " + parsedArticles.size()
                + " in " + (System.currentTimeMillis() - insertStart) + "ms");

        if (!newArticles.isEmpty() && feed.isNotifyNew()) {
            notifyResults.add(new FeedSyncResult(feed.getId(), feed.getTitle(), feed.getOpenMode(), newArticles));
        }
        return newArticles.size();
    }
}