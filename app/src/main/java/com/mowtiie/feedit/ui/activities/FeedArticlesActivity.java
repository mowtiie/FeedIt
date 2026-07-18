package com.mowtiie.feedit.ui.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.view.ActionMode;
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

import java.util.HashSet;
import java.util.Set;

public class FeedArticlesActivity extends FeedItActivity implements ArticleAdapter.Listener {

    public static final String EXTRA_FEED_ID = "extra_feed_id";
    public static final String EXTRA_FEED_TITLE = "extra_feed_title";

    private ActivityFeedArticlesBinding binding;
    private MainViewModel viewModel;
    private ArticleAdapter adapter;

    private final Set<Long> selectedArticleIds = new HashSet<>();
    private ActionMode actionMode;

    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            getMenuInflater().inflate(R.menu.menu_article_selection, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int id = item.getItemId();
            if (id == R.id.action_mark_read) {
                viewModel.markSelectedRead(selectedArticleIds, true);
            } else if (id == R.id.action_mark_unread) {
                viewModel.markSelectedRead(selectedArticleIds, false);
            } else if (id == R.id.action_star_selected) {
                viewModel.starSelected(selectedArticleIds, true);
            } else if (id == R.id.action_unstar_selected) {
                viewModel.starSelected(selectedArticleIds, false);
            } else {
                return false;
            }
            mode.finish();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            selectedArticleIds.clear();
            adapter.updateSelection(selectedArticleIds, false);
        }
    };

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
                if (actionMode != null) {
                    actionMode.finish();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
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
        if (actionMode != null) {
            toggleSelection(item);
            return;
        }

        viewModel.markRead(item.getArticle());

        if (item.getFeedOpenMode() == Feed.OPEN_MODE_BROWSER) {
            openInBrowser(item.getArticle().getLink());
        } else {
            Intent intent = new Intent(this, ArticleDetailActivity.class);
            intent.putExtra(ArticleDetailActivity.EXTRA_ARTICLE_ID, item.getArticle().getId());
            startActivity(intent);
        }
    }

    @Override
    public void onArticleLongClicked(ArticleUiState item) {
        if (actionMode == null) {
            actionMode = startSupportActionMode(actionModeCallback);
        }
        toggleSelection(item);
    }

    private void toggleSelection(ArticleUiState item) {
        long id = item.getArticle().getId();
        if (selectedArticleIds.contains(id)) {
            selectedArticleIds.remove(id);
        } else {
            selectedArticleIds.add(id);
        }

        if (selectedArticleIds.isEmpty()) {
            if (actionMode != null) {
                actionMode.finish();
            }
            return;
        }

        if (actionMode != null) {
            actionMode.setTitle(getString(R.string.selection_count_format, selectedArticleIds.size()));
        }
        adapter.updateSelection(selectedArticleIds, true);
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