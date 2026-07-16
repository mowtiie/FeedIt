package com.mowtiie.feedit.util;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public final class InsetsUtil {

    private InsetsUtil() {
    }

    public static void applyEdgeToEdgeInsets(@NonNull View topBar, @Nullable View bottomContent) {
        ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, v.getPaddingBottom());
            return windowInsets;
        });

        if (bottomContent != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomContent, (v, windowInsets) -> {
                Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(bars.left, v.getPaddingTop(), bars.right, bars.bottom);
                return windowInsets;
            });
        }
    }
}