package com.mowtiie.feedit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mowtiie.feedit.R;
import com.mowtiie.feedit.databinding.ActivityFeedManagementBinding;
import com.mowtiie.feedit.model.FeedTags;
import com.mowtiie.feedit.ui.adapters.FeedAdapter;
import com.mowtiie.feedit.ui.viewmodel.FeedListViewModel;
import com.mowtiie.feedit.util.InsetsUtil;

public class FeedManagementActivity extends AppCompatActivity implements FeedAdapter.Listener {

    private ActivityFeedManagementBinding binding;
    private FeedListViewModel viewModel;
    private FeedAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityFeedManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        InsetsUtil.applyEdgeToEdgeInsets(binding.appBarLayout, binding.recyclerFeeds);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        viewModel = new ViewModelProvider(this).get(FeedListViewModel.class);

        adapter = new FeedAdapter(this);
        binding.recyclerFeeds.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerFeeds.setAdapter(adapter);

        viewModel.getFeeds().observe(this, feeds -> {
            adapter.submitList(feeds);
            binding.textEmptyState.setVisibility(feeds.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.refresh(); // pick up anything changed in AddEditFeedActivity since we last showed
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_feed_management, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_add) {
            startActivity(new Intent(this, AddEditFeedActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFeedClicked(FeedTags item) {
        Intent intent = new Intent(this, AddEditFeedActivity.class);
        intent.putExtra(AddEditFeedActivity.EXTRA_FEED_ID, item.getFeed().getId());
        startActivity(intent);
    }

    @Override
    public void onDeleteClicked(FeedTags item) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_delete_feed_title)
                .setMessage(R.string.confirm_delete_feed_message)
                .setPositiveButton(R.string.action_delete,
                        (dialog, which) -> viewModel.deleteFeed(item.getFeed().getId(), viewModel::refresh))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }
}