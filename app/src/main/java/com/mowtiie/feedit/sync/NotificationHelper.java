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

import java.util.List;

public final class NotificationHelper {

    private static final String CHANNEL_ID = "new_articles";
    private static final String GROUP_KEY = "com.mowtiie.feedit.NEW_ARTICLES";
    private static final int SUMMARY_NOTIFICATION_ID = 0;

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

    public static void notifyNewArticles(Context context, List<FeedSyncResult> results) {
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
                // TODO: swap for a proper monochrome notification icon asset before shipping —
                .setSmallIcon(android.R.drawable.ic_dialog_info)
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
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(body)
                .setStyle(new NotificationCompat.InboxStyle().setSummaryText(body))
                .setGroup(GROUP_KEY)
                .setGroupSummary(true)
                .setAutoCancel(true);
    }
}
