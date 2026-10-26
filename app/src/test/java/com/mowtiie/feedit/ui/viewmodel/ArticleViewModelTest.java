package com.mowtiie.feedit.ui.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.mowtiie.feedit.model.Article;

import org.junit.Test;

public class ArticleViewModelTest {

    @Test
    public void prefersContentOverSummary() {
        Article a = new Article();
        a.setContent("<p>full</p>");
        a.setSummary("short");
        assertEquals("<p>full</p>", ArticleViewModel.htmlOf(a));
    }

    @Test
    public void fallsBackToSummaryThenNull() {
        Article a = new Article();
        a.setSummary("short");
        assertEquals("short", ArticleViewModel.htmlOf(a));

        a.setSummary(null);
        assertNull(ArticleViewModel.htmlOf(a));
    }
}
