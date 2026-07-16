package com.mowtiie.feedit.ui.activities;

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
import com.mowtiie.feedit.data.RepositoryCallback;
import com.mowtiie.feedit.databinding.ActivityTagManagementBinding;
import com.mowtiie.feedit.databinding.DialogTagEditorBinding;
import com.mowtiie.feedit.model.Tag;
import com.mowtiie.feedit.ui.adapters.TagAdapter;
import com.mowtiie.feedit.ui.viewmodel.TagListViewModel;
import com.mowtiie.feedit.util.InsetsUtil;
import com.mowtiie.feedit.util.TagColorPicker;

public class TagManagementActivity extends AppCompatActivity implements TagAdapter.Listener {

    private ActivityTagManagementBinding binding;
    private TagListViewModel viewModel;
    private TagAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityTagManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        InsetsUtil.applyEdgeToEdgeInsets(binding.appBarLayout, binding.recyclerTags);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        viewModel = new ViewModelProvider(this).get(TagListViewModel.class);

        adapter = new TagAdapter(this);
        binding.recyclerTags.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerTags.setAdapter(adapter);

        viewModel.getTags().observe(this, tags -> {
            adapter.submitList(tags);
            binding.textEmptyState.setVisibility(tags.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_tag_management, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_add) {
            showTagDialog(null);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onEditClicked(Tag tag) {
        showTagDialog(tag);
    }

    @Override
    public void onDeleteClicked(Tag tag) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_delete_tag_title)
                .setMessage(R.string.confirm_delete_tag_message)
                .setPositiveButton(R.string.action_delete,
                        (dialog, which) -> viewModel.deleteTag(tag.getId(), null))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showTagDialog(Tag existingTag) {
        DialogTagEditorBinding dialogBinding = DialogTagEditorBinding.inflate(getLayoutInflater());

        View[] fills = {
                dialogBinding.swatchFill0, dialogBinding.swatchFill1, dialogBinding.swatchFill2,
                dialogBinding.swatchFill3, dialogBinding.swatchFill4, dialogBinding.swatchFill5
        };
        View[] rings = {
                dialogBinding.swatchRing0, dialogBinding.swatchRing1, dialogBinding.swatchRing2,
                dialogBinding.swatchRing3, dialogBinding.swatchRing4, dialogBinding.swatchRing5
        };

        String initialColor = existingTag != null && existingTag.getColor() != null
                ? existingTag.getColor() : TagColorPicker.PRESET_COLORS[0];
        String[] selectedColor = {initialColor};
        TagColorPicker.setup(fills, rings, initialColor, hex -> selectedColor[0] = hex);

        if (existingTag != null) {
            dialogBinding.editTagName.setText(existingTag.getName());
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(existingTag == null ? R.string.dialog_create_tag_title : R.string.dialog_edit_tag_title)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.action_save, (dialog, which) -> {
                    String name = dialogBinding.editTagName.getText() != null
                            ? dialogBinding.editTagName.getText().toString().trim() : "";
                    if (name.isEmpty()) {
                        return;
                    }
                    Tag tag = existingTag != null ? existingTag : new Tag();
                    tag.setName(name);
                    tag.setColor(selectedColor[0]);
                    if (tag.getCreatedAt() == 0) {
                        tag.setCreatedAt(System.currentTimeMillis());
                    }
                    viewModel.saveTag(tag, new RepositoryCallback<Long>() {
                        @Override
                        public void onComplete(Long result) {
                        }

                        @Override
                        public void onError(Exception e) {
                            // TODO: surface a Snackbar/Toast once a shared error-display pattern exists
                        }
                    });
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }
}