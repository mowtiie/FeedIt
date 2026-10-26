package com.mowtiie.feedit.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class DateUtilsTest {

    private static final long EXPECTED_UTC = 1_792_588_949_000L; // 2026-10-21T13:22:29Z

    @Test
    public void rfc822_withLegacyUtZone() {
        assertEquals(Long.valueOf(EXPECTED_UTC), DateUtils.parse("Wed, 21 Oct 2026 13:22:29 UT"));
    }

    @Test
    public void rfc822_withGmtAndNumericOffsets() {
        assertEquals(Long.valueOf(EXPECTED_UTC), DateUtils.parse("Wed, 21 Oct 2026 13:22:29 GMT"));
        assertEquals(Long.valueOf(EXPECTED_UTC), DateUtils.parse("Wed, 21 Oct 2026 21:22:29 +0800"));
    }

    @Test
    public void iso8601_withZoneAndOffset() {
        assertEquals(Long.valueOf(EXPECTED_UTC), DateUtils.parse("2026-10-21T13:22:29Z"));
        assertEquals(Long.valueOf(EXPECTED_UTC), DateUtils.parse("2026-10-21T21:22:29+08:00"));
    }

    @Test
    public void rfc822_withUsZoneNames() {
        assertEquals(Long.valueOf(EXPECTED_UTC), DateUtils.parse("Wed, 21 Oct 2026 09:22:29 EDT"));
    }

    @Test
    public void blankOrGarbageIsNull() {
        assertNull(DateUtils.parse(null));
        assertNull(DateUtils.parse("  "));
        assertNull(DateUtils.parse("not a date"));
    }
}
