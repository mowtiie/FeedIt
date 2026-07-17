package com.mowtiie.feedit.ui.activities;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mowtiie.feedit.R;
import com.mowtiie.feedit.databinding.ActivityAddEditFeedBinding;
import com.mowtiie.feedit.databinding.DialogTagEditorBinding;
import com.mowtiie.feedit.model.Feed;
import com.mowtiie.feedit.model.Tag;
import com.mowtiie.feedit.ui.viewmodel.FeedEditViewModel;
import com.mowtiie.feedit.util.InsetsUtil;
import com.mowtiie.feedit.util.TagColorPicker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AddEditFeedActivity extends FeedItActivity {

    public static final String EXTRA_FEED_ID = "extra_feed_id";

    private ActivityAddEditFeedBinding binding;
    private FeedEditViewModel viewModel;

    private List<Tag> latestTags = new ArrayList<>();
    private Set<Long> latestSelectedIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityAddEditFeedBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        InsetsUtil.applyEdgeToEdgeInsets(binding.appBarLayout, binding.scrollContent);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        viewModel = new ViewModelProvider(this).get(FeedEditViewModel.class);

        long feedId = getIntent().getLongExtra(EXTRA_FEED_ID, 0L);
        if (feedId != 0L) {
            viewModel.setEditingFeedId(feedId);
            setTitle(R.string.title_edit_feed);
        } else {
            setTitle(R.string.title_add_feed);
        }

        binding.toggleOpenMode.check(R.id.button_open_in_app);
        setupListeners();
        observeViewModel();
    }

    private void setupListeners() {
        binding.switchNotify.setOnCheckedChangeListener((buttonView, isChecked) ->
                viewModel.setNotifyNew(isChecked));

        binding.toggleOpenMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            int mode = checkedId == R.id.button_open_browser ? Feed.OPEN_MODE_BROWSER : Feed.OPEN_MODE_IN_APP;
            viewModel.setOpenMode(mode);
        });
    }

    private void observeViewModel() {
        viewModel.getAllTags().observe(this, tags -> {
            latestTags = tags;
            renderTagChips();
        });
        viewModel.getSelectedTagIds().observe(this, ids -> {
            latestSelectedIds = ids;
            renderTagChips();
        });
        viewModel.getFormState().observe(this, this::prefillForm);
        viewModel.isLoading().observe(this, this::setLoading);
        viewModel.getErrorMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        });
        viewModel.getSaved().observe(this, saved -> {
            if (saved) {
                finish();
            }
        });
    }

    private void prefillForm(Feed feed) {
        if (feed == null) {
            return;
        }
        binding.editFeedUrl.setText(feed.getUrl());
        binding.editFeedTitle.setText(feed.getTitle());
        binding.switchNotify.setChecked(feed.isNotifyNew());
        binding.toggleOpenMode.check(feed.getOpenMode() == Feed.OPEN_MODE_BROWSER
                ? R.id.button_open_browser : R.id.button_open_in_app);
    }

    private void setLoading(boolean loading) {
        binding.progressSaving.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.editFeedUrl.setEnabled(!loading);
        binding.editFeedTitle.setEnabled(!loading);
    }

    private void renderTagChips() {
        binding.chipGroupTags.removeAllViews();

        for (Tag tag : latestTags) {
            Chip chip = new Chip(this);
            chip.setText(tag.getName());
            chip.setCheckable(true);
            chip.setChecked(latestSelectedIds.contains(tag.getId()));
            chip.setOnClickListener(v -> viewModel.toggleTag(tag.getId()));
            binding.chipGroupTags.addView(chip);
        }

        Chip addChip = new Chip(this);
        addChip.setText(R.string.action_add_tag);
        addChip.setCheckable(false);
        addChip.setOnClickListener(v -> showCreateTagDialog());
        binding.chipGroupTags.addView(addChip);
    }

    private void showCreateTagDialog() {
        DialogTagEditorBinding dialogBinding = DialogTagEditorBinding.inflate(getLayoutInflater());

        View[] fills = {
                dialogBinding.swatchFill0, dialogBinding.swatchFill1, dialogBinding.swatchFill2,
                dialogBinding.swatchFill3, dialogBinding.swatchFill4, dialogBinding.swatchFill5
        };
        View[] rings = {
                dialogBinding.swatchRing0, dialogBinding.swatchRing1, dialogBinding.swatchRing2,
                dialogBinding.swatchRing3, dialogBinding.swatchRing4, dialogBinding.swatchRing5
        };
        String[] selectedColor = {TagColorPicker.PRESET_COLORS[0]};
        TagColorPicker.setup(fills, rings, TagColorPicker.PRESET_COLORS[0], hex -> selectedColor[0] = hex);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_create_tag_title)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.action_create, (dialog, which) -> {
                    String name = dialogBinding.editTagName.getText() != null
                            ? dialogBinding.editTagName.getText().toString().trim() : "";
                    if (!name.isEmpty()) {
                        viewModel.createAndSelectTag(name, selectedColor[0]);
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_edit_feed, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_save) {
            String url = binding.editFeedUrl.getText() != null ? binding.editFeedUrl.getText().toString() : "";
            String title = binding.editFeedTitle.getText() != null ? binding.editFeedTitle.getText().toString() : "";
            viewModel.save(url, title);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}