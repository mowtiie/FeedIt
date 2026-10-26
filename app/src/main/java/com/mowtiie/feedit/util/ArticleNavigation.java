package com.mowtiie.feedit.util;

import java.util.List;

/** Helpers for stepping through the list of articles the reader opened an article from. */
public final class ArticleNavigation {

    public static final int MAX_WINDOW = 500;

    private ArticleNavigation() {
    }

    /**
     * Returns at most {@link #MAX_WINDOW} ids from the list, centred on {@code currentId} so the
     * Intent extra stays small. Returns just the current id when it is not in the list.
     */
    public static long[] window(List<Long> ids, long currentId) {
        int index = ids.indexOf(currentId);
        if (index < 0) {
            return new long[]{currentId};
        }
        int start = Math.max(0, index - MAX_WINDOW / 2);
        int end = Math.min(ids.size(), start + MAX_WINDOW);
        start = Math.max(0, end - MAX_WINDOW);
        long[] result = new long[end - start];
        for (int i = start; i < end; i++) {
            result[i - start] = ids.get(i);
        }
        return result;
    }

    public static int indexOf(long[] ids, long id) {
        if (ids == null) {
            return -1;
        }
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] == id) {
                return i;
            }
        }
        return -1;
    }

    /** Returns the id {@code delta} steps from {@code index}, or -1 when that falls outside the list. */
    public static long neighbor(long[] ids, int index, int delta) {
        if (ids == null || index < 0) {
            return -1L;
        }
        int target = index + delta;
        if (target < 0 || target >= ids.length) {
            return -1L;
        }
        return ids[target];
    }
}
