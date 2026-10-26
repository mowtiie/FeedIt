package com.mowtiie.feedit.util;

public final class ReaderTextSize {

    private ReaderTextSize() {
    }

    public static float scaleFor(String size) {
        if (PrefsKeys.TEXT_SIZE_SMALL.equals(size)) {
            return 0.875f;
        } else if (PrefsKeys.TEXT_SIZE_LARGE.equals(size)) {
            return 1.15f;
        } else if (PrefsKeys.TEXT_SIZE_XLARGE.equals(size)) {
            return 1.3f;
        }
        return 1f;
    }
}
