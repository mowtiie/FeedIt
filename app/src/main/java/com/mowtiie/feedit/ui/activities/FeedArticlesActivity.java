package com.mowtiie.feedit.ui.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.mowtiie.feedit.R;
import com.mowtiie.feedit.databinding.ActivityFeedArticlesBinding;
import com.mowtiie.feedit.model.Feed;
import com.mowtiie.feedit.ui.adapters.ArticleAdapter;
import com.mowtiie.feedit.ui.viewmodel.MainViewModel;
import com.mowtiie.feedit.util.ArticleUiState;
import com.mowtiie.feedit.util.InsetsUtil;
import com.mowtiie.feedit.util.PrefsKeys;

public class FeedArticlesActivity extends FeedItActivity implements ArticleAdapter.Listener {

    public static final String EXTRA_FEED_ID = "extra_feed_id";
    public static final String EXTRA_FEED_TITLE = "extra_feed_title";

    private ActivityFeedArticlesBinding binding;
    private MainViewModel viewModel;
    private ArticleAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityFeedArticlesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        InsetsUtil.applyEdgeToEdgeInsets(binding.appBarLayout, binding.recyclerArticles);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        String feedTitle = getIntent().getStringExtra(EXTRA_FEED_TITLE);
        setTitle(feedTitle != null ? feedTitle : getString(R.string.app_name));

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        long feedId = getIntent().getLongExtra(EXTRA_FEED_ID, -1L);
        viewModel.selectFeed(feedId);

        binding.emptyState.textEmptyKaomoji.setText(R.string.empty_state_no_articles_for_feed_kaomoji);
        binding.emptyState.textEmptyCaption.setText(R.string.empty_state_no_articles_for_feed);

        setupBackNavigation();
        setupRecyclerView();
        observeViewModel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String layoutStyle = getSharedPreferences(PrefsKeys.PREFS_NAME, MODE_PRIVATE).getString(PrefsKeys.ARTICLE_LAYOUT_STYLE, PrefsKeys.LAYOUT_CARD);
        adapter.setLayoutStyle(layoutStyle);
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new ArticleAdapter(this);
        binding.recyclerArticles.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerArticles.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.getArticleUiStates().observe(this, items -> {
            adapter.submitList(items);
            binding.emptyState.getRoot().setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onArticleClicked(ArticleUiState item) {
        viewModel.markRead(item.getArticle());

        if (item.getFeedOpenMode() == Feed.OPEN_MODE_BROWSER) {
            openInBrowser(item.getArticle().getLink());
        } else {
            Intent intent = new Intent(this, ArticleDetailActivity.class);
            intent.putExtra(ArticleDetailActivity.EXTRA_ARTICLE_ID, item.getArticle().getId());
            startActivity(intent);
        }
    }

    private void openInBrowser(String url) {
        if (url == null) {
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No browser app found", Toast.LENGTH_SHORT).show();
        }
    }
}