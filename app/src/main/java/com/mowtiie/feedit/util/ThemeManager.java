package com.mowtiie.feedit.util;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.color.DynamicColors;
import com.mowtiie.feedit.R;

public final class ThemeManager {

    private ThemeManager() {
    }

    public static void applyPersistedDarkMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PrefsKeys.PREFS_NAME, Context.MODE_PRIVATE);
        applyDarkMode(prefs.getString(PrefsKeys.DARK_MODE, "system"));
    }

    public static void applyDarkMode(String mode) {
        int nightMode;
        if ("light".equals(mode)) {
            nightMode = AppCompatDelegate.MODE_NIGHT_NO;
        } else if ("dark".equals(mode)) {
            nightMode = AppCompatDelegate.MODE_NIGHT_YES;
        } else {
            nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }

    public static void applyDynamicColorsIfEnabled(Application application) {
        SharedPreferences prefs = application.getSharedPreferences(PrefsKeys.PREFS_NAME, Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(PrefsKeys.DYNAMIC_COLORS_ENABLED, false);
        if (enabled) {
            DynamicColors.applyToActivitiesIfAvailable(application);
        }
    }

    public static void applyPersistedContrast(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PrefsKeys.PREFS_NAME, Context.MODE_PRIVATE);
        String contrast = prefs.getString(PrefsKeys.CONTRAST_LEVEL, PrefsKeys.CONTRAST_STANDARD);
        if (PrefsKeys.CONTRAST_MEDIUM.equals(contrast)) {
            activity.setTheme(R.style.Theme_FeedIt_MediumContrast);
        } else if (PrefsKeys.CONTRAST_HIGH.equals(contrast)) {
            activity.setTheme(R.style.Theme_FeedIt_HighContrast);
        }
    }
}