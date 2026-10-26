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
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
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
import com.google.android.material.snackbar.Snackbar;
import com.mowtiie.feedit.R;
import com.mowtiie.feedit.crash.CrashReporter;
import com.mowtiie.feedit.data.ArticleDao;
import com.mowtiie.feedit.databinding.ActivityMainBinding;
import com.mowtiie.feedit.model.Feed;
import com.mowtiie.feedit.model.FeedTags;
import com.mowtiie.feedit.model.Tag;
import com.mowtiie.feedit.sync.SyncScheduler;
import com.mowtiie.feedit.sync.SyncWorker;
import com.mowtiie.feedit.sync.SyncLog;
import com.mowtiie.feedit.ui.adapters.ArticleAdapter;
import com.mowtiie.feedit.ui.viewmodel.MainViewModel;
import com.mowtiie.feedit.util.ArticleActionsBottomSheet;
import com.mowtiie.feedit.util.ArticleSwipeCallback;
import com.mowtiie.feedit.util.BrowserUtil;
import com.mowtiie.feedit.util.ArticleUiState;
import com.mowtiie.feedit.util.InsetsUtil;
import com.mowtiie.feedit.util.PrefsKeys;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends FeedItActivity implements ArticleAdapter.Listener {

    public static final String EXTRA_OPEN_FEED_ID = "extra_open_feed_id";

    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private ArticleAdapter adapter;
    private ActionBarDrawerToggle drawerToggle;
    private SearchView searchView;
    private boolean skipNextResumeRefresh = true;

    private static final int FEED_ITEM_BASE = 0x10000000;
    private static final int TAG_ITEM_BASE = 0x20000000;
    private int selectedNavId = R.id.nav_all;
    private List<FeedTags> drawerFeeds = Collections.emptyList();
    private List<Tag> drawerTags = Collections.emptyList();
    private Map<Long, Integer> unreadCounts = new HashMap<>();

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
        setupFilterChips();
        setupSwipeRefresh();
        observeViewModel();

        long openFeedId = getIntent().getLongExtra(EXTRA_OPEN_FEED_ID, -1L);
        if (openFeedId != -1L) {
            viewModel.selectFeed(openFeedId);
            selectedNavId = FEED_ITEM_BASE + (int) openFeedId;
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
        String layoutStyle = getSharedPreferences(PrefsKeys.PREFS_NAME, MODE_PRIVATE).getString(PrefsKeys.ARTICLE_LAYOUT_STYLE, PrefsKeys.DEFAULT_LAYOUT);
        adapter.setLayoutStyle(layoutStyle);
        syncFilterChips();
        if (skipNextResumeRefresh) {
            // onCreate already selected a scope, which loaded the list.
            skipNextResumeRefresh = false;
        } else {
            viewModel.refresh();
        }
    }

    private void applyStartupPage() {
        String startupPage = getSharedPreferences(PrefsKeys.PREFS_NAME, MODE_PRIVATE)
                .getString(PrefsKeys.STARTUP_PAGE, PrefsKeys.STARTUP_PAGE_ALL);

        if (PrefsKeys.STARTUP_PAGE_STARRED.equals(startupPage)) {
            viewModel.selectStarred();
            setTitle(R.string.nav_starred);
            selectedNavId = R.id.nav_starred;
            isOnAllScope = false;
        } else {
            viewModel.selectAll();
            setTitle(R.string.app_name);
            selectedNavId = R.id.nav_all;
            isOnAllScope = true;
        }

        binding.navView.setCheckedItem(selectedNavId);
        syncFilterChips();
    }

    private void maybePromptForNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return;
        }
        android.content.SharedPreferences prefs = getSharedPreferences(PrefsKeys.PREFS_NAME, MODE_PRIVATE);
        if (prefs.getBoolean(PrefsKeys.NOTIFICATION_PROMPT_SHOWN, false)) {
            return;
        }
        prefs.edit().putBoolean(PrefsKeys.NOTIFICATION_PROMPT_SHOWN, true).apply();

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_notification_title)
                .setMessage(R.string.dialog_notification_message)
                .setPositiveButton(R.string.action_allow, (dialog, which) ->
                        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS))
                .setNegativeButton(R.string.action_not_now, null)
                .show();
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
        } else if (id >= TAG_ITEM_BASE) {
            viewModel.selectTag(id - TAG_ITEM_BASE);
            setTitle(item.getTitle());
            isOnAllScope = true;
        }
        selectedNavId = id;
        updateSearchHint();
        syncFilterChips();
        invalidateOptionsMenu();

        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
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

    private void setupSwipeRefresh() {
        binding.emptyState.buttonEmptyAddFeed.setOnClickListener(v ->
                startActivity(new Intent(this, AddEditFeedActivity.class)));
        binding.emptyState.buttonEmptyImport.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

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
                    int failedFeeds = 0;
                    for (androidx.work.WorkInfo info : workInfos) {
                        androidx.work.WorkInfo.State state = info.getState();
                        failedFeeds += info.getOutputData().getInt(SyncWorker.KEY_FAILED_COUNT, 0);
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
                        if (failedFeeds > 0) {
                            Snackbar.make(binding.getRoot(),
                                            getResources().getQuantityString(R.plurals.snackbar_refresh_failed, failedFeeds, failedFeeds),
                                            Snackbar.LENGTH_LONG)
                                    .setAction(R.string.action_details, v ->
                                            startActivity(new Intent(this, FeedManagementActivity.class)))
                                    .show();
                        }
                    }
                });
    }

    private void observeViewModel() {
        viewModel.getArticleUiStates().observe(this, items -> {
            adapter.submitList(items);
            updateEmptyState(items);
        });
        viewModel.getFeeds().observe(this, feeds -> {
            drawerFeeds = feeds;
            rebuildDrawerLists();
            updateEmptyState(adapter.getCurrentList());
            if (!feeds.isEmpty()) {
                maybePromptForNotifications();
            }
        });
        viewModel.getTags().observe(this, tags -> {
            drawerTags = tags;
            rebuildDrawerLists();
        });
        viewModel.getUnreadCounts().observe(this, counts -> {
            unreadCounts = counts;
            rebuildDrawerLists();
        });
    }

    private void rebuildDrawerLists() {
        Menu menu = binding.navView.getMenu();
        int totalUnread = 0;
        for (int count : unreadCounts.values()) {
            totalUnread += count;
        }
        setBadge(menu.findItem(R.id.nav_all), totalUnread);

        MenuItem tagsSection = menu.findItem(R.id.nav_section_tags);
        tagsSection.setVisible(!drawerTags.isEmpty());
        Menu tagsMenu = tagsSection.getSubMenu();
        tagsMenu.removeGroup(R.id.group_tags);
        for (Tag tag : drawerTags) {
            MenuItem item = tagsMenu.add(R.id.group_tags, TAG_ITEM_BASE + (int) tag.getId(), Menu.NONE, tag.getName());
            item.setIcon(R.drawable.ic_tag);
            item.setCheckable(true);
            setBadge(item, tagUnreadCount(tag.getId()));
            if (item.getItemId() == selectedNavId) {
                setTitle(tag.getName());
            }
        }

        if (isFeedScope(selectedNavId)) {
            for (FeedTags ft : drawerFeeds) {
                if (FEED_ITEM_BASE + (int) ft.getFeed().getId() == selectedNavId) {
                    Feed feed = ft.getFeed();
                    setTitle(feed.getTitle() != null ? feed.getTitle() : feed.getUrl());
                }
            }
            uncheckDrawerItems();
        } else if (menu.findItem(selectedNavId) != null) {
            binding.navView.setCheckedItem(selectedNavId);
        }
        updateSearchHint();
    }

    private static boolean isFeedScope(int navId) {
        return navId >= FEED_ITEM_BASE && navId < TAG_ITEM_BASE;
    }

    private void uncheckDrawerItems() {
        Menu menu = binding.navView.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            item.setChecked(false);
            if (item.hasSubMenu()) {
                for (int j = 0; j < item.getSubMenu().size(); j++) {
                    item.getSubMenu().getItem(j).setChecked(false);
                }
            }
        }
    }

    private int countOrZero(long feedId) {
        Integer count = unreadCounts.get(feedId);
        return count != null ? count : 0;
    }

    private int tagUnreadCount(long tagId) {
        int total = 0;
        for (FeedTags ft : drawerFeeds) {
            for (Tag tag : ft.getTags()) {
                if (tag.getId() == tagId) {
                    total += countOrZero(ft.getFeed().getId());
                    break;
                }
            }
        }
        return total;
    }

    private void setBadge(MenuItem item, int count) {
        if (item == null) {
            return;
        }
        if (count <= 0) {
            item.setActionView(null);
            return;
        }
        if (item.getActionView() == null) {
            TextView badge = (TextView) getLayoutInflater().inflate(R.layout.view_nav_badge, binding.navView, false);
            badge.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
            item.setActionView(badge);
        }
        ((TextView) item.getActionView()).setText(String.valueOf(count));
    }

    private void updateSearchHint() {
        if (searchView != null) {
            searchView.setQueryHint(getString(R.string.hint_search_in, getTitle()));
        }
    }

    private void updateEmptyState(List<ArticleUiState> items) {
        if (!items.isEmpty()) {
            binding.emptyState.getRoot().setVisibility(View.GONE);
            return;
        }
        binding.emptyState.getRoot().setVisibility(View.VISIBLE);

        boolean noFeeds = viewModel.isFeedsLoaded() && !viewModel.hasFeeds();
        binding.emptyState.buttonEmptyAddFeed.setVisibility(noFeeds ? View.VISIBLE : View.GONE);
        binding.emptyState.buttonEmptyImport.setVisibility(noFeeds ? View.VISIBLE : View.GONE);
        if (noFeeds) {
            binding.emptyState.textEmptyKaomoji.setText(R.string.empty_state_no_feeds_kaomoji);
            binding.emptyState.textEmptyCaption.setText(R.string.empty_state_no_feeds_caption);
            return;
        }

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
        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

        assert searchView != null;
        updateSearchHint();
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
            showMarkedReadSnackbar(viewModel.markAllRead());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showMarkedReadSnackbar(List<Long> markedIds) {
        if (markedIds.isEmpty()) {
            return;
        }
        Snackbar.make(binding.getRoot(),
                        getResources().getQuantityString(R.plurals.snackbar_marked_read, markedIds.size(), markedIds.size()),
                        Snackbar.LENGTH_LONG)
                .setAction(R.string.action_undo, v -> viewModel.markUnread(markedIds))
                .show();
    }

    private void setupFilterChips() {
        binding.chipFilterUnread.setOnClickListener(v -> onFilterChipClicked(binding.chipFilterUnread));
        binding.chipFilterRead.setOnClickListener(v -> onFilterChipClicked(binding.chipFilterRead));
        syncFilterChips();
    }

    private void onFilterChipClicked(com.google.android.material.chip.Chip clicked) {
        boolean showRead = binding.chipFilterRead.isChecked();
        boolean showUnread = binding.chipFilterUnread.isChecked();
        if (!showRead && !showUnread) {
            clicked.setChecked(true);
            return;
        }
        viewModel.setReadStateFilter(showRead, showUnread);
    }

    private void syncFilterChips() {
        binding.filterChipScroll.setVisibility(isOnAllScope ? View.VISIBLE : View.GONE);
        binding.chipFilterRead.setChecked(viewModel.isShowRead());
        binding.chipFilterUnread.setChecked(viewModel.isShowUnread());
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