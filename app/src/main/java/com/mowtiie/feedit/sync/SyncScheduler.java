package com.mowtiie.feedit.sync;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.mowtiie.feedit.util.PrefsKeys;

import java.util.concurrent.TimeUnit;

public final class SyncScheduler {

    private static final String PERIODIC_WORK_NAME = "feedit_periodic_sync";
    public static final String MANUAL_SYNC_WORK_NAME = "feedit_manual_sync";

    private static final String PREFS_NAME = "feedit_prefs";
    private static final String KEY_SYNC_WIFI_ONLY = "sync_wifi_only";

    private static final long DEFAULT_INTERVAL_MINUTES = 60;

    private SyncScheduler() {
    }

    public static void schedulePeriodicSync(Context context) {
        long intervalMinutes = context.getSharedPreferences(PrefsKeys.PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(PrefsKeys.SYNC_INTERVAL_MINUTES, DEFAULT_INTERVAL_MINUTES);
        long clampedMinutes = Math.max(intervalMinutes, PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS / 60000L);

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                SyncWorker.class, clampedMinutes, TimeUnit.MINUTES)
                .setConstraints(buildConstraints(context))
                .build();

        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request);
    }

    public static void triggerManualSync(Context context) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(MANUAL_SYNC_WORK_NAME, ExistingWorkPolicy.REPLACE, request);
    }

    private static Constraints buildConstraints(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean wifiOnly = prefs.getBoolean(KEY_SYNC_WIFI_ONLY, false);
        NetworkType networkType = wifiOnly ? NetworkType.UNMETERED : NetworkType.CONNECTED;
        return new Constraints.Builder().setRequiredNetworkType(networkType).build();
    }
}