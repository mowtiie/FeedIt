package com.mowtiie.feedit.ui.activities;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.work.WorkManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mowtiie.feedit.R;
import com.mowtiie.feedit.crash.CrashReporter;
import com.mowtiie.feedit.data.ArticleDao;
import com.mowtiie.feedit.databinding.ActivityMainBinding;
import com.mowtiie.feedit.model.Feed;
import com.mowtiie.feedit.sync.SyncScheduler;
import com.mowtiie.feedit.sync.SyncLog;
import com.mowtiie.feedit.ui.adapters.ArticleAdapter;
import com.mowtiie.feedit.ui.viewmodel.MainViewModel;
import com.mowtiie.feedit.util.ArticleActionsBottomSheet;
import com.mowtiie.feedit.util.ArticleUiState;
import com.mowtiie.feedit.util.InsetsUtil;
import com.mowtiie.feedit.util.PrefsKeys;

import java.util.List;

public class MainActivity extends FeedItActivity implements ArticleAdapter.Listener {

    public static final String EXTRA_OPEN_FEED_ID = "extra_open_feed_id";

    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private ArticleAdapter adapter;
    private ActionBarDrawerToggle drawerToggle;

    private final ActivityResultLauncher<String> requestNotificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                // No-op either way — if denied, sync still runs, notifications just won't show.
            });

    private boolean crashDialogShown;

    private boolean isOnAllScope = true;

    private boolean manualSyncWasRunning;

    private static final long SEARCH_DEBOUNCE_MS = 250L;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;

    private final ActivityResultLauncher<Intent> saveCrashLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                Uri uri = result.getData().getData();
                if (uri == null) return;

                if (CrashReporter.writeReportToUri(this, uri)) {
                    Toast.makeText(this, R.string.toast_crash_save_success, Toast.LENGTH_SHORT).show();
                    CrashReporter.deleteReport(this);
                } else {
                    Toast.makeText(this, R.string.toast_crash_save_failure, Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        InsetsUtil.applyEdgeToEdgeInsets(binding.appBarLayout, binding.recyclerArticles);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        setSupportActionBar(binding.toolbar);
        setupDrawer();
        setupBackNavigation();
        setupRecyclerView();
        setupSwipeRefresh();
        observeViewModel();

        maybeRequestNotificationPermission();

        long openFeedId = getIntent().getLongExtra(EXTRA_OPEN_FEED_ID, -1L);
        if (openFeedId != -1L) {
            viewModel.selectFeed(openFeedId);
        } else {
            applyStartupPage();
        }

        if (!crashDialogShown) {
            crashDialogShown = true;
            CrashReporter.showDialogIfPending(this, saveCrashLauncher);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String layoutStyle = getSharedPreferences(PrefsKeys.PREFS_NAME, MODE_PRIVATE).getString(PrefsKeys.ARTICLE_LAYOUT_STYLE, PrefsKeys.LAYOUT_MAGAZINE);
        adapter.setLayoutStyle(layoutStyle);
        viewModel.refresh();
    }

    private void applyStartupPage() {
        String startupPage = getSharedPreferences(PrefsKeys.PREFS_NAME, MODE_PRIVATE)
                .getString(PrefsKeys.STARTUP_PAGE, PrefsKeys.STARTUP_PAGE_ALL);

        int checkedItemId;
        if (PrefsKeys.STARTUP_PAGE_STARRED.equals(startupPage)) {
            viewModel.selectStarred();
            setTitle(R.string.nav_starred);
            checkedItemId = R.id.nav_starred;
            isOnAllScope = false;
        } else {
            viewModel.selectAll();
            setTitle(R.string.app_name);
            checkedItemId = R.id.nav_all;
            isOnAllScope = true;
        }

        binding.navView.getMenu().findItem(checkedItemId).setChecked(true);
    }

    private void maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void setupDrawer() {
        drawerToggle = new ActionBarDrawerToggle(
                this, binding.drawerLayout, binding.toolbar,
                R.string.content_desc_open_drawer, R.string.content_desc_open_drawer);
        binding.drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        binding.navView.setNavigationItemSelectedListener(this::onDrawerItemSelected);
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private boolean onDrawerItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_manage_feeds) {
            startActivity(new Intent(this, FeedManagementActivity.class));
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } else if (id == R.id.nav_manage_tags) {
            startActivity(new Intent(this, TagManagementActivity.class));
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } else if (id == R.id.nav_about) {
            startActivity(new Intent(this, AboutActivity.class));
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }

        if (id == R.id.nav_all) {
            viewModel.selectAll();
            setTitle(R.string.app_name);
            isOnAllScope = true;
        } else if (id == R.id.nav_starred) {
            viewModel.selectStarred();
            setTitle(R.string.nav_starred);
            isOnAllScope = false;
        }
        invalidateOptionsMenu();

        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void setupRecyclerView() {
        adapter = new ArticleAdapter(this);
        binding.recyclerArticles.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerArticles.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> {
            SyncLog.d("manual refresh triggered by pull-to-refresh");
            manualSyncWasRunning = true;
            SyncScheduler.triggerManualSync(this);
        });

        WorkManager.getInstance(this)
                .getWorkInfosForUniqueWorkLiveData(SyncScheduler.MANUAL_SYNC_WORK_NAME)
                .observe(this, workInfos -> {
                    if (workInfos == null || workInfos.isEmpty()) {
                        return;
                    }

                    boolean anyRunning = false;
                    for (androidx.work.WorkInfo info : workInfos) {
                        androidx.work.WorkInfo.State state = info.getState();
                        if (state == androidx.work.WorkInfo.State.ENQUEUED
                                || state == androidx.work.WorkInfo.State.RUNNING
                                || state == androidx.work.WorkInfo.State.BLOCKED) {
                            anyRunning = true;
                        }
                    }

                    if (anyRunning) {
                        manualSyncWasRunning = true;
                        return;
                    }

                    if (manualSyncWasRunning) {
                        manualSyncWasRunning = false;
                        SyncLog.d("manual-sync finished -> stopping spinner, refreshing list");
                        binding.swipeRefresh.setRefreshing(false);
                        viewModel.refresh();
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
            kaomojiRes = R.string.empty_state_no_articles_kaomoji;
            captionRes = R.string.empty_state_no_articles;
        }
        binding.emptyState.textEmptyKaomoji.setText(kaomojiRes);
        binding.emptyState.textEmptyCaption.setText(captionRes);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

        assert searchView != null;
        searchView.setQueryHint(getString(R.string.hint_search_articles));
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
        MenuItem filterItem = menu.findItem(R.id.action_filter);
        if (filterItem != null) {
            filterItem.setVisible(isOnAllScope);
        }

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
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        int id = item.getItemId();
        if (id == R.id.action_sort) {
            showSortDialog();
            return true;
        } else if (id == R.id.action_mark_all_read) {
            viewModel.markAllRead();
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

    private void showSortDialog() {
        String[] labels = {
                getString(R.string.sort_newest),
                getString(R.string.sort_oldest),
                getString(R.string.sort_unread_first),
                getString(R.string.sort_by_feed)
        };
        ArticleDao.SortOrder[] values = ArticleDao.SortOrder.values();
        int checkedIndex = viewModel.getSortOrder().ordinal();

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.sort_dialog_title)
                .setSingleChoiceItems(labels, checkedIndex, (dialog, which) -> {
                    viewModel.setSortOrder(values[which]);
                    dialog.dismiss();
                })
                .show();
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

    @Override
    public void onArticleLongClicked(ArticleUiState item) {
        ArticleActionsBottomSheet.show(this, item.getArticle(), viewModel);
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