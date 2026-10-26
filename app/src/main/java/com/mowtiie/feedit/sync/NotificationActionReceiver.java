package com.mowtiie.feedit.sync;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.app.NotificationManagerCompat;

import com.mowtiie.feedit.data.FeedRepository;
import com.mowtiie.feedit.model.Feed;
import com.mowtiie.feedit.ui.activities.ArticleDetailActivity;

public class NotificationActionReceiver extends BroadcastReceiver {

    public static final String ACTION_MARK_READ = "com.mowtiie.feedit.action.MARK_READ";
    public static final String ACTION_OPEN = "com.mowtiie.feedit.action.OPEN";

    public static final String EXTRA_ARTICLE_ID = "extra_article_id";
    public static final String EXTRA_ARTICLE_LINK = "extra_article_link";
    public static final String EXTRA_OPEN_MODE = "extra_open_mode";
    public static final String EXTRA_NOTIFICATION_ID = "extra_notification_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        long articleId = intent.getLongExtra(EXTRA_ARTICLE_ID, -1L);
        if (articleId == -1L) {
            return;
        }

        FeedRepository.getInstance(context).setArticleRead(articleId, true);

        int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1);
        if (notificationId != -1) {
            NotificationManagerCompat.from(context).cancel(notificationId);
        }

        if (ACTION_OPEN.equals(intent.getAction())) {
            openArticle(context, intent);
        }
    }

    private void openArticle(Context context, Intent sourceIntent) {
        int openMode = sourceIntent.getIntExtra(EXTRA_OPEN_MODE, Feed.OPEN_MODE_IN_APP);
        String link = sourceIntent.getStringExtra(EXTRA_ARTICLE_LINK);
        long articleId = sourceIntent.getLongExtra(EXTRA_ARTICLE_ID, -1L);

        Intent openIntent;
        if (openMode == Feed.OPEN_MODE_BROWSER && link != null) {
            openIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
            openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } else {
            openIntent = new Intent(context, ArticleDetailActivity.class);
            openIntent.putExtra(ArticleDetailActivity.EXTRA_ARTICLE_ID, articleId);
            openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }

        try {
            context.startActivity(openIntent);
        } catch (ActivityNotFoundException e) {
        }
    }
}