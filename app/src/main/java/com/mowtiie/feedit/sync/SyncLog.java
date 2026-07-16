package com.mowtiie.feedit.sync;

import android.util.Log;

public final class SyncLog {

    public static final String TAG = "FeedItSync";

    private SyncLog() {
    }

    public static void d(String message) {
        Log.d(TAG, message);
    }

    public static void w(String message) {
        Log.w(TAG, message);
    }

    public static void e(String message, Throwable t) {
        Log.e(TAG, message, t);
    }
}