package com.mowtiie.feedit.util;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public final class InsetsUtil {

    private InsetsUtil() {
    }

    public static void applyEdgeToEdgeInsets(@NonNull View topBar, @Nullable View bottomContent) {
        applyEdgeToEdgeInsets(topBar, bottomContent, 0);
    }

    /** Like the two-argument form, with extra bottom padding (px) so content can scroll clear of a FAB. */
    public static void applyEdgeToEdgeInsets(@NonNull View topBar, @Nullable View bottomContent, int extraBottomPx) {
        if (bottomContent != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomContent, (v, windowInsets) -> {
                Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(bars.left, v.getPaddingTop(), bars.right, bars.bottom + extraBottomPx);
                return windowInsets;
            });
        }
    }

    /** Keeps a floating button above the system navigation bar. */
    public static void applyFabInsets(@NonNull View fab) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
        int baseBottom = params.bottomMargin;
        int baseRight = params.rightMargin;
        ViewCompat.setOnApplyWindowInsetsListener(fab, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            lp.bottomMargin = baseBottom + bars.bottom;
            lp.rightMargin = baseRight + bars.right;
            v.setLayoutParams(lp);
            return windowInsets;
        });
    }
}
