package com.mowtiie.feedit.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ArticleNavigationTest {

    private static List<Long> range(int count) {
        List<Long> ids = new ArrayList<>();
        for (long i = 0; i < count; i++) {
            ids.add(i);
        }
        return ids;
    }

    @Test
    public void window_smallListIsReturnedWhole() {
        assertArrayEquals(new long[]{0, 1, 2, 3}, ArticleNavigation.window(range(4), 2L));
    }

    @Test
    public void window_unknownCurrentIdReturnsOnlyThatId() {
        assertArrayEquals(new long[]{99L}, ArticleNavigation.window(range(4), 99L));
    }

    @Test
    public void window_largeListIsCappedAndContainsCurrent() {
        long[] window = ArticleNavigation.window(range(5000), 2500L);

        assertEquals(ArticleNavigation.MAX_WINDOW, window.length);
        assertEquals(2500L, window[ArticleNavigation.indexOf(window, 2500L)]);
        assertEquals(250L, ArticleNavigation.indexOf(window, 2500L));
    }

    @Test
    public void window_atListEdgesStaysFullSize() {
        long[] start = ArticleNavigation.window(range(5000), 0L);
        long[] end = ArticleNavigation.window(range(5000), 4999L);

        assertEquals(ArticleNavigation.MAX_WINDOW, start.length);
        assertEquals(0L, start[0]);
        assertEquals(ArticleNavigation.MAX_WINDOW, end.length);
        assertEquals(4999L, end[end.length - 1]);
    }

    @Test
    public void indexOf_findsOrReturnsMinusOne() {
        long[] ids = {5, 6, 7};
        assertEquals(1, ArticleNavigation.indexOf(ids, 6L));
        assertEquals(-1, ArticleNavigation.indexOf(ids, 9L));
        assertEquals(-1, ArticleNavigation.indexOf(null, 5L));
    }

    @Test
    public void neighbor_stepsAndStopsAtBoundaries() {
        long[] ids = {10, 20, 30};
        assertEquals(20L, ArticleNavigation.neighbor(ids, 0, 1));
        assertEquals(10L, ArticleNavigation.neighbor(ids, 1, -1));
        assertEquals(-1L, ArticleNavigation.neighbor(ids, 0, -1));
        assertEquals(-1L, ArticleNavigation.neighbor(ids, 2, 1));
        assertEquals(-1L, ArticleNavigation.neighbor(ids, -1, 1));
        assertEquals(-1L, ArticleNavigation.neighbor(null, 0, 1));
    }
}
