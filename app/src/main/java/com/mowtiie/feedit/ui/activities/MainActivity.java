package com.mowtiie.feedit.ui.activities;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.work.WorkManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mowtiie.feedit.R;
import com.mowtiie.feedit.data.ArticleDao;
import com.mowtiie.feedit.databinding.ActivityMainBinding;
import com.mowtiie.feedit.model.Feed;
import com.mowtiie.feedit.model.FeedTags;
import com.mowtiie.feedit.model.Tag;
import com.mowtiie.feedit.sync.SyncScheduler;
import com.mowtiie.feedit.ui.adapters.ArticleAdapter;
import com.mowtiie.feedit.util.ArticleUiState;
import com.mowtiie.feedit.util.InsetsUtil;
import com.mowtiie.feedit.ui.viewmodel.MainViewModel;

import java.util.List;

public class MainActivity extends AppCompatActivity implements ArticleAdapter.Listener {

    public static final String EXTRA_OPEN_FEED_ID = "extra_open_feed_id";

    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private ArticleAdapter adapter;
    private ActionBarDrawerToggle drawerToggle;

    private MenuItem currentlyCheckedItem;

    private final ActivityResultLauncher<String> requestNotificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                // no-op either way — denied just means sync runs without notifications
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
        }
    }

    private void maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> SyncScheduler.triggerManualSync(this));

        WorkManager.getInstance(this)
                .getWorkInfosForUniqueWorkLiveData(SyncScheduler.MANUAL_SYNC_WORK_NAME)
                .observe(this, workInfos -> {
                    if (workInfos == null || workInfos.isEmpty()) return;
                    if (workInfos.get(0).getState().isFinished()) {
                        binding.swipeRefresh.setRefreshing(false);
                        viewModel.refresh();
                    }
                });
    }

    private void setupDrawer() {
        drawerToggle = new ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, R.string.content_desc_open_drawer, R.string.content_desc_open_drawer);
        binding.drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        binding.navView.setNavigationItemSelectedListener(this::onDrawerItemSelected);
        currentlyCheckedItem = binding.navView.getMenu().findItem(R.id.nav_all);
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
            // TODO: launch FeedManagementActivity once it exists (Phase 4)
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } else if (id == R.id.nav_manage_tags) {
            // TODO: launch TagManagementActivity once it exists (Phase 4)
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } else if (id == R.id.nav_settings) {
            // TODO: launch SettingsActivity once it exists (Phase 6)
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }

        if (id == R.id.nav_all) {
            viewModel.selectAll();
        } else if (id == R.id.nav_unread) {
            viewModel.selectUnread();
        } else if (id == R.id.nav_starred) {
            viewModel.selectStarred();
        } else if (item.getGroupId() == R.id.group_tags) {
            viewModel.selectTag(id);
        } else if (item.getGroupId() == R.id.group_feeds) {
            viewModel.selectFeed(id);
        }

        if (currentlyCheckedItem != null) {
            currentlyCheckedItem.setChecked(false);
        }

        item.setCheckable(true);
        item.setChecked(true);
        currentlyCheckedItem = item;

        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void setupRecyclerView() {
        adapter = new ArticleAdapter(this);
        binding.recyclerArticles.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerArticles.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.getArticleUiStates().observe(this, items -> {
            adapter.submitList(items);
            binding.textEmptyState.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        });
        viewModel.getTags().observe(this, this::populateTagGroup);
        viewModel.getFeedsWithTags().observe(this, this::populateFeedGroup);
    }

    private void populateTagGroup(List<Tag> tags) {
        SubMenu subMenu = binding.navView.getMenu().findItem(R.id.header_tags).getSubMenu();
        subMenu.removeGroup(R.id.group_tags);
        for (Tag tag : tags) {
            subMenu.add(R.id.group_tags, (int) tag.getId(), Menu.NONE, tag.getName()).setCheckable(true);
        }
    }

    private void populateFeedGroup(List<FeedTags> feeds) {
        SubMenu subMenu = binding.navView.getMenu().findItem(R.id.header_feeds).getSubMenu();
        subMenu.removeGroup(R.id.group_feeds);
        for (FeedTags feedTags : feeds) {
            String title = feedTags.getFeed().getTitle() != null
                    ? feedTags.getFeed().getTitle() : feedTags.getFeed().getUrl();
            subMenu.add(R.id.group_feeds, (int) feedTags.getFeed().getId(), Menu.NONE, title)
                    .setCheckable(true);
        }
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
        } else if (id == R.id.action_filter) {
            showFilterDialog();
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

    private void showFilterDialog() {
        boolean[] checked = {viewModel.isUnreadOnly(), viewModel.isStarredOnly()};
        String[] labels = {
                getString(R.string.filter_unread_only),
                getString(R.string.filter_starred_only)
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.filter_dialog_title)
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton(R.string.filter_apply,
                        (dialog, which) -> viewModel.applyReadStateFilter(checked[0], checked[1]))
                .setNegativeButton(R.string.filter_cancel, null)
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

    @Override
    public void onStarToggled(ArticleUiState item) {
        viewModel.toggleStarred(item.getArticle());
    }
}