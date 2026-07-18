package com.mowtiie.feedit.ui.activities;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.view.ActionMode;
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
import com.mowtiie.feedit.util.ArticleUiState;
import com.mowtiie.feedit.util.InsetsUtil;
import com.mowtiie.feedit.util.PrefsKeys;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends FeedItActivity implements ArticleAdapter.Listener {

    public static final String EXTRA_OPEN_FEED_ID = "extra_open_feed_id";

    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private ArticleAdapter adapter;
    private ActionBarDrawerToggle drawerToggle;

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

    private final ActivityResultLauncher<String> requestNotificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                // No-op either way — if denied, sync still runs, notifications just won't show.
            });

    private boolean crashDialogShown;

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
        String layoutStyle = getSharedPreferences(PrefsKeys.PREFS_NAME, MODE_PRIVATE)
                .getString(PrefsKeys.ARTICLE_LAYOUT_STYLE, PrefsKeys.LAYOUT_CARD);
        adapter.setLayoutStyle(layoutStyle);
    }

    private void applyStartupPage() {
        String startupPage = getSharedPreferences(PrefsKeys.PREFS_NAME, MODE_PRIVATE)
                .getString(PrefsKeys.STARTUP_PAGE, PrefsKeys.STARTUP_PAGE_ALL);

        int checkedItemId;
        if (PrefsKeys.STARTUP_PAGE_UNREAD.equals(startupPage)) {
            viewModel.selectUnread();
            setTitle(R.string.nav_unread);
            checkedItemId = R.id.nav_unread;
        } else if (PrefsKeys.STARTUP_PAGE_STARRED.equals(startupPage)) {
            viewModel.selectStarred();
            setTitle(R.string.nav_starred);
            checkedItemId = R.id.nav_starred;
        } else {
            viewModel.selectAll();
            setTitle(R.string.app_name);
            checkedItemId = R.id.nav_all;
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
                if (actionMode != null) {
                    actionMode.finish();
                } else if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
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
        } else if (id == R.id.nav_unread) {
            viewModel.selectUnread();
            setTitle(R.string.nav_unread);
        } else if (id == R.id.nav_starred) {
            viewModel.selectStarred();
            setTitle(R.string.nav_starred);
        }

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
                        SyncLog.d("manual-sync WorkInfo state = " + info.getState());
                        androidx.work.WorkInfo.State state = info.getState();
                        if (state == androidx.work.WorkInfo.State.ENQUEUED
                                || state == androidx.work.WorkInfo.State.RUNNING
                                || state == androidx.work.WorkInfo.State.BLOCKED) {
                            anyRunning = true;
                        }
                    }

                    if (!anyRunning) {
                        SyncLog.d("manual-sync: no runs in progress -> stopping spinner, refreshing list");
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
        } else if (viewModel.isUnreadOnly()) {
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
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                viewModel.setSearchQuery(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                viewModel.setSearchQuery(newText);
                return true;
            }
        });
        return true;
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
        }
        return super.onOptionsItemSelected(item);
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