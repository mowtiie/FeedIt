package com.mowtiie.feedit.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mowtiie.feedit.model.Article;
import com.mowtiie.feedit.model.Feed;
import com.mowtiie.feedit.model.FeedTags;
import com.mowtiie.feedit.model.Tag;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FeedRepository {

    private static volatile FeedRepository instance;

    private final FeedDao feedDao;
    private final TagDao tagDao;
    private final ArticleDao articleDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<List<Article>> articles = new MutableLiveData<>();
    private final MutableLiveData<List<FeedTags>> feedsWithTags = new MutableLiveData<>();
    private final MutableLiveData<List<Tag>> tags = new MutableLiveData<>();

    private FeedRepository(Context context) {
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        this.feedDao = new FeedDao(dbHelper);
        this.tagDao = new TagDao(dbHelper);
        this.articleDao = new ArticleDao(dbHelper);
    }

    public static synchronized FeedRepository getInstance(Context context) {
        if (instance == null) {
            instance = new FeedRepository(context.getApplicationContext());
        }
        return instance;
    }

    public LiveData<List<Article>> getArticles() {
        return articles;
    }

    public void loadArticles(Long feedId, Long tagId, boolean unreadOnly, boolean starredOnly,
                             String searchQuery, ArticleDao.SortOrder sortOrder) {
        executor.execute(() -> {
            List<Article> result = articleDao.getArticles(
                    feedId, tagId, unreadOnly, starredOnly, searchQuery, sortOrder);
            articles.postValue(result);
        });
    }

    public void setArticleRead(long articleId, boolean read) {
        executor.execute(() -> articleDao.setRead(articleId, read));
    }

    public void setArticleStarred(long articleId, boolean starred) {
        executor.execute(() -> articleDao.setStarred(articleId, starred));
    }

    public void markAllRead(Long feedId, Long tagId) {
        executor.execute(() -> articleDao.markAllRead(feedId, tagId));
    }

    public void getArticleById(long articleId, RepositoryCallback<Article> callback) {
        executor.execute(() -> runCallback(() -> articleDao.getArticleById(articleId), callback));
    }

    public LiveData<List<FeedTags>> getFeedsWithTags() {
        return feedsWithTags;
    }

    public void loadFeedsWithTags() {
        executor.execute(() -> feedsWithTags.postValue(feedDao.getAllFeedsWithTags()));
    }

    public void getFeedById(long feedId, RepositoryCallback<Feed> callback) {
        executor.execute(() -> runCallback(() -> feedDao.getFeedById(feedId), callback));
    }

    public void saveFeed(Feed feed, List<Long> tagIds, RepositoryCallback<Long> callback) {
        executor.execute(() -> runCallback(() -> {
            long feedId = feed.getId() != 0 ? feed.getId() : feedDao.insertFeed(feed);
            if (feed.getId() != 0) {
                feedDao.updateFeed(feed);
            }
            feedDao.setTagsForFeed(feedId, tagIds);
            return feedId;
        }, callback, this::loadFeedsWithTags));
    }

    public void deleteFeed(long feedId, RepositoryCallback<Void> callback) {
        executor.execute(() -> runCallback(() -> {
            feedDao.deleteFeed(feedId);
            return null;
        }, callback, this::loadFeedsWithTags));
    }

    public LiveData<List<Tag>> getTags() {
        return tags;
    }

    public void loadTags() {
        executor.execute(() -> tags.postValue(tagDao.getAllTags()));
    }

    public void saveTag(Tag tag, RepositoryCallback<Long> callback) {
        executor.execute(() -> runCallback(() -> {
            if (tag.getId() != 0) {
                tagDao.updateTag(tag);
                return tag.getId();
            }
            return tagDao.insertTag(tag);
        }, callback, this::loadTags));
    }

    public void deleteTag(long tagId, RepositoryCallback<Void> callback) {
        executor.execute(() -> runCallback(() -> {
            tagDao.deleteTag(tagId);
            return null;
        }, callback, this::loadTags));
    }

    public void getUnreadCountForTag(long tagId, RepositoryCallback<Integer> callback) {
        executor.execute(() -> runCallback(() -> tagDao.getUnreadCountForTag(tagId), callback));
    }

    private interface DbAction<T> {
        T run();
    }

    private <T> void runCallback(DbAction<T> action, RepositoryCallback<T> callback) {
        runCallback(action, callback, null);
    }

    private <T> void runCallback(DbAction<T> action, RepositoryCallback<T> callback,
                                 Runnable afterSuccessOnBgThread) {
        try {
            T result = action.run();
            if (afterSuccessOnBgThread != null) {
                afterSuccessOnBgThread.run();
            }
            if (callback != null) {
                mainHandler.post(() -> callback.onComplete(result));
            }
        } catch (Exception e) {
            if (callback != null) {
                mainHandler.post(() -> callback.onError(e));
            }
        }
    }

    public void insertArticles(List<Article> newArticles, RepositoryCallback<Integer> callback) {
        executor.execute(() -> runCallback(() -> articleDao.insertAllOrIgnore(newArticles), callback));
    }
}