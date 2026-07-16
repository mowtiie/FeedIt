package com.mowtiie.feedit.util;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;

public final class TagColorPicker {

    public static final String[] PRESET_COLORS = {
            "#2B6777", // teal
            "#C9A227", // amber
            "#C1666B", // coral
            "#7C6BA8", // purple
            "#4C9A6A", // green
            "#4472CA"  // blue
    };

    private TagColorPicker() {
    }

    public interface OnColorSelected {
        void onColorSelected(String hex);
    }

    public static void setup(View[] fillViews, View[] ringViews, String initialHex,
                              OnColorSelected callback) {
        int selectedIndex = 0;
        for (int i = 0; i < PRESET_COLORS.length; i++) {
            if (PRESET_COLORS[i].equalsIgnoreCase(initialHex)) {
                selectedIndex = i;
                break;
            }
        }

        for (int i = 0; i < PRESET_COLORS.length; i++) {
            int index = i;
            fillViews[i].setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(PRESET_COLORS[i])));
            ringViews[i].setVisibility(index == selectedIndex ? View.VISIBLE : View.GONE);

            View clickTarget = (View) fillViews[i].getParent();
            clickTarget.setOnClickListener(v -> {
                for (View ring : ringViews) {
                    ring.setVisibility(View.GONE);
                }
                ringViews[index].setVisibility(View.VISIBLE);
                callback.onColorSelected(PRESET_COLORS[index]);
            });
        }

        callback.onColorSelected(PRESET_COLORS[selectedIndex]);
    }
}
