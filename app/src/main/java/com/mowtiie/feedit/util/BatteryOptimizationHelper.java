package com.mowtiie.feedit.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.Settings;

public final class BatteryOptimizationHelper {

    private BatteryOptimizationHelper() {
    }

    public static boolean isIgnoringBatteryOptimizations(Context context) {
        PowerManager powerManager = context.getSystemService(PowerManager.class);
        return powerManager != null
                && powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
    }

    public static Intent buildRequestExemptionIntent(Context context) {
        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        return intent;
    }

    public static Intent buildAppSettingsIntent(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        return intent;
    }
}