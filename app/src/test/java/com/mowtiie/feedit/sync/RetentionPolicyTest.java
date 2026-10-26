package com.mowtiie.feedit.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class RetentionPolicyTest {

    private static final long DAY = 24L * 60L * 60L * 1000L;

    @Test
    public void zeroOrNegativeDaysMeansKeepForever() {
        assertNull(RetentionPolicy.cutoffMillis(0L, 1_000_000L));
        assertNull(RetentionPolicy.cutoffMillis(-5L, 1_000_000L));
    }

    @Test
    public void cutoffIsNowMinusWholeDays() {
        long now = 400L * DAY;
        assertEquals(Long.valueOf(now - 90L * DAY), RetentionPolicy.cutoffMillis(90L, now));
        assertEquals(Long.valueOf(now - DAY), RetentionPolicy.cutoffMillis(1L, now));
    }
}
