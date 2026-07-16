package com.mowtiie.feedit;

import android.app.Application;

import com.mowtiie.feedit.sync.NotificationHelper;
import com.mowtiie.feedit.sync.SyncScheduler;

public class FeedItApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.ensureChannel(this);
        SyncScheduler.schedulePeriodicSync(this);
    }
}
