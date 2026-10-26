package com.mowtiie.feedit.sync;

import android.content.Context;

import com.mowtiie.feedit.util.PrefsKeys;

public final class RetentionPolicy {

    public static final long KEEP_FOREVER = 0L;
    private static final long MILLIS_PER_DAY = 24L * 60L * 60L * 1000L;

    private RetentionPolicy() {
    }

    public static long getRetentionDays(Context context) {
        return context.getSharedPreferences(PrefsKeys.PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(PrefsKeys.RETENTION_DAYS, PrefsKeys.DEFAULT_RETENTION_DAYS);
    }

    /** Returns the oldest published time worth keeping, or null when articles are kept forever. */
    public static Long cutoffMillis(long retentionDays, long now) {
        if (retentionDays <= KEEP_FOREVER) {
            return null;
        }
        return now - retentionDays * MILLIS_PER_DAY;
    }

    public static Long cutoffMillis(Context context) {
        return cutoffMillis(getRetentionDays(context), System.currentTimeMillis());
    }
}
