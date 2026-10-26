package com.mowtiie.feedit.ui.activities;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.mowtiie.feedit.R;
import com.mowtiie.feedit.databinding.ActivityArticleDetailBinding;
import com.mowtiie.feedit.model.Article;
import com.mowtiie.feedit.ui.viewmodel.ArticleViewModel;
import com.mowtiie.feedit.util.ArticleNavigation;
import com.mowtiie.feedit.util.ArticleUiState;
import com.mowtiie.feedit.util.InsetsUtil;
import com.mowtiie.feedit.util.PrefsKeys;
import com.mowtiie.feedit.util.ReaderTextSize;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ArticleDetailActivity extends FeedItActivity {

    public static final String EXTRA_ARTICLE_ID = "extra_article_id";
    public static final String EXTRA_ARTICLE_IDS = "extra_article_ids";
    private static final String STATE_ARTICLE_ID = "state_article_id";

    private ActivityArticleDetailBinding binding;
    private ArticleViewModel viewModel;
    private long[] articleIds;
    private long currentArticleId;

    /** Intent that opens an article and lets the reader step through the list it came from. */
    public static Intent newIntent(Context context, List<ArticleUiState> items, long articleId) {
        List<Long> ids = new ArrayList<>(items.size());
        for (ArticleUiState item : items) {
            ids.add(item.getArticle().getId());
        }
        Intent intent = new Intent(context, ArticleDetailActivity.class);
        intent.putExtra(EXTRA_ARTICLE_ID, articleId);
        intent.putExtra(EXTRA_ARTICLE_IDS, ArticleNavigation.window(ids, articleId));
        return intent;
    }

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

        long articleId = savedInstanceState != null
                ? savedInstanceState.getLong(STATE_ARTICLE_ID, -1L)
                : getIntent().getLongExtra(EXTRA_ARTICLE_ID, -1L);
        if (articleId == -1L) {
            finish();
            return;
        }
        currentArticleId = articleId;
        articleIds = getIntent().getLongArrayExtra(EXTRA_ARTICLE_IDS);

        binding.textContent.setMovementMethod(LinkMovementMethod.getInstance());
        applyReaderTextSize();
        setupNavigationBar();
        viewModel.load(articleId);
        viewModel.getArticle().observe(this, this::renderArticle);
        viewModel.getRenderedContent().observe(this, binding.textContent::setText);
        viewModel.getImageLoadTick().observe(this, tick -> {
            if (tick > 0) {
                binding.textContent.setText(binding.textContent.getText());
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_ARTICLE_ID, currentArticleId);
    }

    private void applyReaderTextSize() {
        String size = getSharedPreferences(PrefsKeys.PREFS_NAME, MODE_PRIVATE)
                .getString(PrefsKeys.READER_TEXT_SIZE, PrefsKeys.TEXT_SIZE_DEFAULT);
        float scale = ReaderTextSize.scaleFor(size);
        if (scale != 1f) {
            binding.textContent.setTextSize(TypedValue.COMPLEX_UNIT_PX, binding.textContent.getTextSize() * scale);
        }
    }

    private void setupNavigationBar() {
        boolean canNavigate = articleIds != null && articleIds.length > 1;
        binding.barArticleNavigation.setVisibility(canNavigate ? View.VISIBLE : View.GONE);
        binding.buttonPreviousArticle.setOnClickListener(v -> step(-1));
        binding.buttonNextArticle.setOnClickListener(v -> step(1));
        ViewCompat.setOnApplyWindowInsetsListener(binding.barArticleNavigation, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), v.getPaddingTop() + bars.bottom);
            return windowInsets;
        });
        // Keep the last lines of text clear of the bar.
        binding.barArticleNavigation.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) binding.scrollContent.getLayoutParams();
            int height = v.getVisibility() == View.VISIBLE ? bottom - top : 0;
            if (params.bottomMargin != height) {
                params.bottomMargin = height;
                binding.scrollContent.setLayoutParams(params);
            }
        });
    }

    private void updateNavigationButtons() {
        int index = ArticleNavigation.indexOf(articleIds, currentArticleId);
        binding.buttonPreviousArticle.setEnabled(ArticleNavigation.neighbor(articleIds, index, -1) != -1L);
        binding.buttonNextArticle.setEnabled(ArticleNavigation.neighbor(articleIds, index, 1) != -1L);
    }

    private void step(int delta) {
        long target = ArticleNavigation.neighbor(articleIds, ArticleNavigation.indexOf(articleIds, currentArticleId), delta);
        if (target == -1L) {
            return;
        }
        currentArticleId = target;
        binding.scrollContent.scrollTo(0, 0);
        binding.appBarLayout.setExpanded(true, false);
        viewModel.load(target, true);
    }

    private void renderArticle(Article article) {
        if (article == null) {
            return;
        }

        currentArticleId = article.getId();
        updateNavigationButtons();
        if (article.getTitle() != null) {
            setTitle(article.getTitle());
            binding.collapsingToolbar.setTitle(article.getTitle());
        }

        bindHeroImage(article.getImageUrl());

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

        invalidateOptionsMenu();
    }

    private void bindHeroImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            binding.imageHero.setVisibility(View.GONE);
            return;
        }
        binding.imageHero.setVisibility(View.VISIBLE);
        Glide.with(this)
                .load(imageUrl)
                .centerCrop()
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        binding.imageHero.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        return false;
                    }
                })
                .into(binding.imageHero);
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
                    ? R.drawable.ic_star_filled
                    : R.drawable.ic_star_outlined);
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