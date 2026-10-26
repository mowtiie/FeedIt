package com.mowtiie.feedit.ui.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.mowtiie.feedit.R;
import com.mowtiie.feedit.databinding.ActivityFeedArticlesBinding;
import com.mowtiie.feedit.model.Feed;
import com.mowtiie.feedit.sync.SyncLog;
import com.mowtiie.feedit.sync.SyncScheduler;
import com.mowtiie.feedit.ui.adapters.ArticleAdapter;
import com.mowtiie.feedit.ui.viewmodel.MainViewModel;
import com.mowtiie.feedit.util.ArticleSwipeCallback;
import com.mowtiie.feedit.util.BrowserUtil;
import com.mowtiie.feedit.util.ArticleActionsBottomSheet;
import com.mowtiie.feedit.util.ArticleUiState;
import com.mowtiie.feedit.util.InsetsUtil;
import com.mowtiie.feedit.util.PrefsKeys;

import java.util.List;

public class FeedArticlesActivity extends FeedItActivity implements ArticleAdapter.Listener {

    public static final String EXTRA_FEED_ID = "extra_feed_id";
    public static final String EXTRA_FEED_TITLE = "extra_feed_title";

    private static final long SEARCH_DEBOUNCE_MS = 250L;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;

    private long feedId;
    private boolean feedSyncWasRunning;

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

        feedId = getIntent().getLongExtra(EXTRA_FEED_ID, -1L);
        viewModel.selectFeed(feedId);

        binding.swipeRefresh.setOnRefreshListener(() -> {
            SyncLog.d("feed refresh triggered by pull-to-refresh, feedId=" + feedId);
            feedSyncWasRunning = true;
            SyncScheduler.triggerFeedSync(this, feedId);
        });

        WorkManager.getInstance(this)
                .getWorkInfosForUniqueWorkLiveData(SyncScheduler.feedSyncWorkName(feedId))
                .observe(this, workInfos -> {
                    if (workInfos == null || workInfos.isEmpty()) {
                        return;
                    }

                    boolean anyRunning = false;
                    for (WorkInfo info : workInfos) {
                        WorkInfo.State state = info.getState();
                        if (state == WorkInfo.State.ENQUEUED
                                || state == WorkInfo.State.RUNNING
                                || state == WorkInfo.State.BLOCKED) {
                            anyRunning = true;
                        }
                    }

                    if (anyRunning) {
                        feedSyncWasRunning = true;
                        return;
                    }

                    if (feedSyncWasRunning) {
                        feedSyncWasRunning = false;
                        SyncLog.d("feed sync finished, feedId=" + feedId + " -> stopping spinner, refreshing list");
                        binding.swipeRefresh.setRefreshing(false);
                        viewModel.refresh();
                    }
                });

        setupBackNavigation();
        setupRecyclerView();
        observeViewModel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String layoutStyle = getSharedPreferences(PrefsKeys.PREFS_NAME, MODE_PRIVATE).getString(PrefsKeys.ARTICLE_LAYOUT_STYLE, PrefsKeys.DEFAULT_LAYOUT);
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
        ArticleSwipeCallback.attach(binding.recyclerArticles, adapter, new ArticleSwipeCallback.Actions() {
            @Override
            public void onSwipeToggleRead(ArticleUiState item) {
                viewModel.toggleRead(item.getArticle());
            }

            @Override
            public void onSwipeToggleStar(ArticleUiState item) {
                viewModel.toggleStarred(item.getArticle());
            }
        });
    }

    private void observeViewModel() {
        viewModel.getArticleUiStates().observe(this, items -> {
            adapter.submitList(items);
            updateEmptyState(items);
        });
    }

    private void updateEmptyState(List<ArticleUiState> items) {
        if (!items.isEmpty()) {
            binding.emptyState.getRoot().setVisibility(View.GONE);
            return;
        }
        binding.emptyState.getRoot().setVisibility(View.VISIBLE);

        String query = viewModel.getSearchQuery();
        int kaomojiRes;
        int captionRes;
        if (query != null && !query.trim().isEmpty()) {
            kaomojiRes = R.string.empty_state_no_results_kaomoji;
            captionRes = R.string.empty_state_no_results;
        } else if (!viewModel.isShowRead() && viewModel.isShowUnread()) {
            kaomojiRes = R.string.empty_state_all_caught_up_kaomoji;
            captionRes = R.string.empty_state_all_caught_up;
        } else {
            kaomojiRes = R.string.empty_state_no_articles_for_feed_kaomoji;
            captionRes = R.string.empty_state_no_articles_for_feed;
        }
        binding.emptyState.textEmptyKaomoji.setText(kaomojiRes);
        binding.emptyState.textEmptyCaption.setText(captionRes);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_feed_articles, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                cancelPendingSearch();
                viewModel.setSearchQuery(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                scheduleSearch(newText);
                return true;
            }
        });
        return true;
    }

    private void scheduleSearch(String query) {
        cancelPendingSearch();
        pendingSearch = () -> viewModel.setSearchQuery(query);
        searchHandler.postDelayed(pendingSearch, SEARCH_DEBOUNCE_MS);
    }

    private void cancelPendingSearch() {
        if (pendingSearch != null) {
            searchHandler.removeCallbacks(pendingSearch);
            pendingSearch = null;
        }
    }

    @Override
    protected void onDestroy() {
        cancelPendingSearch();
        super.onDestroy();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem readItem = menu.findItem(R.id.filter_show_read);
        if (readItem != null) {
            readItem.setChecked(viewModel.isShowRead());
        }
        MenuItem unreadItem = menu.findItem(R.id.filter_show_unread);
        if (unreadItem != null) {
            unreadItem.setChecked(viewModel.isShowUnread());
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.filter_show_read || id == R.id.filter_show_unread) {
            return handleReadStateFilterClick(item);
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean handleReadStateFilterClick(MenuItem item) {
        boolean newChecked = !item.isChecked();
        boolean otherIsRead = item.getItemId() == R.id.filter_show_unread;
        boolean otherChecked = otherIsRead ? viewModel.isShowRead() : viewModel.isShowUnread();

        if (!newChecked && !otherChecked) {
            return true;
        }

        item.setChecked(newChecked);
        boolean showRead = item.getItemId() == R.id.filter_show_read ? newChecked : viewModel.isShowRead();
        boolean showUnread = item.getItemId() == R.id.filter_show_unread ? newChecked : viewModel.isShowUnread();
        viewModel.setReadStateFilter(showRead, showUnread);
        return true;
    }

    @Override
    public void onArticleClicked(ArticleUiState item) {
        viewModel.markRead(item.getArticle());

        if (item.getFeedOpenMode() == Feed.OPEN_MODE_BROWSER) {
            BrowserUtil.openUrl(this, item.getArticle().getLink());
        } else {
            startActivity(ArticleDetailActivity.newIntent(this, adapter.getCurrentList(), item.getArticle().getId()));
        }
    }

    @Override
    public void onArticleLongClicked(ArticleUiState item) {
        ArticleActionsBottomSheet.show(this, item.getArticle(), viewModel);
    }
}