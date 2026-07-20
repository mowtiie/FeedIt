package com.mowtiie.feedit.ui.activities;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mowtiie.feedit.R;
import com.mowtiie.feedit.databinding.ActivityFeedNotificationSettingsBinding;
import com.mowtiie.feedit.model.FeedTags;
import com.mowtiie.feedit.ui.adapters.FeedNotificationAdapter;
import com.mowtiie.feedit.ui.viewmodel.FeedNotificationSettingsViewModel;
import com.mowtiie.feedit.util.InsetsUtil;

import java.util.List;

public class FeedNotificationSettingsActivity extends FeedItActivity implements FeedNotificationAdapter.Listener {

    private ActivityFeedNotificationSettingsBinding binding;
    private FeedNotificationSettingsViewModel viewModel;
    private FeedNotificationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityFeedNotificationSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        InsetsUtil.applyEdgeToEdgeInsets(binding.appBarLayout, binding.recyclerFeedNotifications);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setTitle(R.string.title_feed_notifications);

        viewModel = new ViewModelProvider(this).get(FeedNotificationSettingsViewModel.class);

        adapter = new FeedNotificationAdapter(this);
        binding.recyclerFeedNotifications.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerFeedNotifications.setAdapter(adapter);

        viewModel.getDisplayedFeeds().observe(this, feeds -> {
            adapter.submitList(feeds);
            updateEmptyState(feeds);
        });
    }

    private void updateEmptyState(List<FeedTags> feeds) {
        if (!feeds.isEmpty()) {
            binding.emptyState.getRoot().setVisibility(View.GONE);
            return;
        }
        binding.emptyState.getRoot().setVisibility(View.VISIBLE);

        String query = viewModel.getSearchQuery();
        int kaomojiRes;
        int captionRes;
        if (query != null && !query.trim().isEmpty()) {
            kaomojiRes = R.string.empty_state_no_results_kaomoji;
            captionRes = R.string.empty_state_no_matching_feeds;
        } else {
            kaomojiRes = R.string.empty_state_no_feeds_kaomoji;
            captionRes = R.string.empty_state_no_feeds;
        }
        binding.emptyState.textEmptyKaomoji.setText(kaomojiRes);
        binding.emptyState.textEmptyCaption.setText(captionRes);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_feed_notifications, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

        assert searchView != null;
        searchView.setQueryHint(getString(R.string.hint_search_feed_notification));
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
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_sort) {
            showSortDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSortDialog() {
        FeedNotificationSettingsViewModel.SortOrder[] values = {
                FeedNotificationSettingsViewModel.SortOrder.NAME_ASC,
                FeedNotificationSettingsViewModel.SortOrder.NAME_DESC,
                FeedNotificationSettingsViewModel.SortOrder.NOTIFY_ON_FIRST,
                FeedNotificationSettingsViewModel.SortOrder.NOTIFY_OFF_FIRST
        };
        String[] labels = {
                getString(R.string.sort_feeds_name_asc),
                getString(R.string.sort_feeds_name_desc),
                getString(R.string.sort_feeds_notify_on_first),
                getString(R.string.sort_feeds_notify_off_first)
        };

        int checkedIndex = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == viewModel.getSortOrder()) {
                checkedIndex = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_sort_feeds_title)
                .setSingleChoiceItems(labels, checkedIndex, (dialog, which) -> {
                    viewModel.setSortOrder(values[which]);
                    dialog.dismiss();
                })
                .show();
    }

    @Override
    public void onNotifyNewToggled(FeedTags item, boolean enabled) {
        viewModel.setFeedNotifyNew(item.getFeed().getId(), enabled);
    }
}