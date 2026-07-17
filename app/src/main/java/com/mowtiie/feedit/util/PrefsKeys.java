package com.mowtiie.feedit.util;

public final class PrefsKeys {

    public static final String PREFS_NAME = "feedit_prefs";

    public static final String SORT_ORDER = "sort_order";           // used by MainViewModel
    public static final String SYNC_WIFI_ONLY = "sync_wifi_only";   // used by SyncScheduler
    public static final String NOTIFICATIONS_ENABLED = "notifications_enabled";
    public static final String DARK_MODE = "dark_mode"; // one of: "system", "light", "dark"

    public static final String STARTUP_PAGE = "startup_page"; // one of the STARTUP_PAGE_* values below
    public static final String STARTUP_PAGE_ALL = "all";
    public static final String STARTUP_PAGE_UNREAD = "unread";
    public static final String STARTUP_PAGE_STARRED = "starred";

    private PrefsKeys() {
    }
}