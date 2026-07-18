package com.mowtiie.feedit;

import android.app.Application;

import com.mowtiie.feedit.crash.CrashHandler;
import com.mowtiie.feedit.sync.NotificationHelper;
import com.mowtiie.feedit.sync.SyncScheduler;
import com.mowtiie.feedit.util.ThemeManager;

public class FeedItApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler.install(this);
        ThemeManager.applyPersistedDarkMode(this);
        ThemeManager.setupDynamicColors(this);
        NotificationHelper.ensureChannel(this);
        NotificationHelper.ensureSyncStatusChannel(this);
        SyncScheduler.schedulePeriodicSync(this);
    }
}