package com.mowtiie.feedit.sync;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.mowtiie.feedit.R;
import com.mowtiie.feedit.ui.activities.MainActivity;
import com.mowtiie.feedit.util.PrefsKeys;

import java.util.List;

public final class NotificationHelper {

    private static final String CHANNEL_ID = "new_articles";
    private static final String GROUP_KEY = "com.mowtiie.feedit.NEW_ARTICLES";
    private static final int SUMMARY_NOTIFICATION_ID = 0;

    private static final String SYNC_STATUS_CHANNEL_ID = "sync_status";
    private static final int SYNC_STATUS_NOTIFICATION_ID = -1;

    private NotificationHelper() {
    }

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(context.getString(R.string.notification_channel_description));
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void ensureSyncStatusChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(
                SYNC_STATUS_CHANNEL_ID,
                context.getString(R.string.notification_channel_sync_status_name),
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(context.getString(R.string.notification_channel_sync_status_description));
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    public static void notifyNewArticles(Context context, List<FeedSyncResult> results) {
        boolean notificationsEnabled = context
                .getSharedPreferences(PrefsKeys.PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(PrefsKeys.NOTIFICATIONS_ENABLED, true);
        if (!notificationsEnabled) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        ensureChannel(context);
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);

        int totalNew = 0;
        for (FeedSyncResult result : results) {
            totalNew += result.getNewArticleCount();
            manager.notify((int) result.getFeedId(), buildFeedNotification(context, result).build());
        }

        manager.notify(SUMMARY_NOTIFICATION_ID, buildSummaryNotification(context, totalNew).build());
    }

    public static void notifySyncStatus(Context context, int totalNewArticles, int failedFeedCount) {
        boolean notificationsEnabled = context
                .getSharedPreferences(PrefsKeys.PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(PrefsKeys.NOTIFICATIONS_ENABLED, true);
        if (!notificationsEnabled) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        ensureSyncStatusChannel(context);

        StringBuilder body = new StringBuilder();
        if (totalNewArticles > 0) {
            body.append(context.getResources().getQuantityString(
                    R.plurals.new_articles_count, totalNewArticles, totalNewArticles));
        } else {
            body.append(context.getString(R.string.sync_status_no_new_articles));
        }
        if (failedFeedCount > 0) {
            body.append(" · ").append(context.getResources().getQuantityString(
                    R.plurals.sync_status_failed_count, failedFeedCount, failedFeedCount));
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, SYNC_STATUS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_feedit)
                .setContentTitle(context.getString(R.string.sync_status_title))
                .setContentText(body.toString())
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        NotificationManagerCompat.from(context).notify(SYNC_STATUS_NOTIFICATION_ID, builder.build());
    }

    private static NotificationCompat.Builder buildFeedNotification(Context context, FeedSyncResult result) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_OPEN_FEED_ID, result.getFeedId());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, (int) result.getFeedId(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = result.getFeedTitle() != null ? result.getFeedTitle() : context.getString(R.string.app_name);
        String body = context.getResources().getQuantityString(
                R.plurals.new_articles_count, result.getNewArticleCount(), result.getNewArticleCount());

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_feedit)
                .setContentTitle(title)
                .setContentText(body)
                .setGroup(GROUP_KEY)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
    }

    private static NotificationCompat.Builder buildSummaryNotification(Context context, int totalNew) {
        String body = context.getResources().getQuantityString(R.plurals.new_articles_count, totalNew, totalNew);
        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_feedit)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(body)
                .setStyle(new NotificationCompat.InboxStyle().setSummaryText(body))
                .setGroup(GROUP_KEY)
                .setGroupSummary(true)
                .setAutoCancel(true);
    }
}