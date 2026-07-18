package com.mowtiie.feedit.util;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;
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

    public static void setupDynamicColors(Application application) {
        DynamicColorsOptions options = new DynamicColorsOptions.Builder()
                .setPrecondition((activity, theme) -> {
                    SharedPreferences prefs =
                            activity.getSharedPreferences(PrefsKeys.PREFS_NAME, Context.MODE_PRIVATE);
                    return prefs.getBoolean(PrefsKeys.DYNAMIC_COLORS_ENABLED, false);
                })
                .build();
        DynamicColors.applyToActivitiesIfAvailable(application, options);
    }

    public static void applyDynamicColorsToActivityIfEnabled(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PrefsKeys.PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(PrefsKeys.DYNAMIC_COLORS_ENABLED, false)) {
            DynamicColors.applyToActivityIfAvailable(activity);
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