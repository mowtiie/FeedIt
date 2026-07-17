package com.mowtiie.feedit;

import android.app.Application;

import com.mowtiie.feedit.sync.NotificationHelper;
import com.mowtiie.feedit.sync.SyncScheduler;
import com.mowtiie.feedit.util.ThemeManager;

public class FeedItApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ThemeManager.applyPersistedDarkMode(this);
        ThemeManager.applyDynamicColorsIfEnabled(this);
        NotificationHelper.ensureChannel(this);
        SyncScheduler.schedulePeriodicSync(this);
    }
}