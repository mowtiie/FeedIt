package com.mowtiie.feedit.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ReaderTextSizeTest {

    @Test
    public void scalesIncreaseWithSize() {
        assertEquals(0.875f, ReaderTextSize.scaleFor(PrefsKeys.TEXT_SIZE_SMALL), 0.0001f);
        assertEquals(1f, ReaderTextSize.scaleFor(PrefsKeys.TEXT_SIZE_DEFAULT), 0.0001f);
        assertEquals(1.15f, ReaderTextSize.scaleFor(PrefsKeys.TEXT_SIZE_LARGE), 0.0001f);
        assertEquals(1.3f, ReaderTextSize.scaleFor(PrefsKeys.TEXT_SIZE_XLARGE), 0.0001f);
    }

    @Test
    public void unknownOrMissingFallsBackToDefault() {
        assertEquals(1f, ReaderTextSize.scaleFor(null), 0.0001f);
        assertEquals(1f, ReaderTextSize.scaleFor("huge"), 0.0001f);
    }
}
