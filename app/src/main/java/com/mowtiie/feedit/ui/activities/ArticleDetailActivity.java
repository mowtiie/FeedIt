package com.mowtiie.feedit.ui.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.text.HtmlCompat;
import androidx.lifecycle.ViewModelProvider;

import com.mowtiie.feedit.R;
import com.mowtiie.feedit.databinding.ActivityArticleDetailBinding;
import com.mowtiie.feedit.model.Article;
import com.mowtiie.feedit.ui.viewmodel.ArticleViewModel;
import com.mowtiie.feedit.util.InsetsUtil;

import java.text.DateFormat;
import java.util.Date;

public class ArticleDetailActivity extends FeedItActivity {

    public static final String EXTRA_ARTICLE_ID = "extra_article_id";

    private ActivityArticleDetailBinding binding;
    private ArticleViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityArticleDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        InsetsUtil.applyEdgeToEdgeInsets(binding.appBarLayout, binding.scrollContent);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        viewModel = new ViewModelProvider(this).get(ArticleViewModel.class);

        long articleId = getIntent().getLongExtra(EXTRA_ARTICLE_ID, -1L);
        if (articleId == -1L) {
            finish();
            return;
        }

        binding.textContent.setMovementMethod(LinkMovementMethod.getInstance());
        viewModel.load(articleId);
        viewModel.getArticle().observe(this, this::renderArticle);
    }

    private void renderArticle(Article article) {
        if (article == null) {
            return;
        }

        if (article.getTitle() != null) {
            setTitle(article.getTitle());
            binding.collapsingToolbar.setTitle(article.getTitle());
        }

        StringBuilder meta = new StringBuilder();
        if (article.getAuthor() != null && !article.getAuthor().isEmpty()) {
            meta.append(article.getAuthor());
        }
        if (article.getPublishedAt() != null) {
            String date = DateFormat.getDateInstance(DateFormat.MEDIUM)
                    .format(new Date(article.getPublishedAt()));
            if (meta.length() > 0) {
                meta.append(" · ");
            }
            meta.append(date);
        }
        binding.textMeta.setText(meta.toString());

        String html = article.getContent() != null ? article.getContent() : article.getSummary();
        if (html != null) {
            binding.textContent.setText(HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT));
        }

        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_article_detail, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Article article = viewModel.getArticle().getValue();
        if (article != null) {
            menu.findItem(R.id.action_star).setIcon(article.isStarred()
                    ? android.R.drawable.btn_star_big_on
                    : android.R.drawable.btn_star_big_off);
            menu.findItem(R.id.action_toggle_read).setTitle(article.isRead()
                    ? R.string.action_mark_unread
                    : R.string.action_mark_read);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_star) {
            viewModel.toggleStarred();
            return true;
        } else if (id == R.id.action_toggle_read) {
            viewModel.toggleRead();
            return true;
        } else if (id == R.id.action_share) {
            shareArticle();
            return true;
        } else if (id == R.id.action_open_browser) {
            openInBrowser();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void shareArticle() {
        Article article = viewModel.getArticle().getValue();
        if (article == null || article.getLink() == null) {
            return;
        }
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, article.getLink());
        if (article.getTitle() != null) {
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, article.getTitle());
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.action_share)));
    }

    private void openInBrowser() {
        Article article = viewModel.getArticle().getValue();
        if (article == null || article.getLink() == null) {
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(article.getLink())));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No browser app found", Toast.LENGTH_SHORT).show();
        }
    }
}