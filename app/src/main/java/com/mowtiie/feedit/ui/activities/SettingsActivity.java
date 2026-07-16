package com.mowtiie.feedit.ui.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.mowtiie.feedit.R;
import com.mowtiie.feedit.databinding.ActivitySettingsBinding;
import com.mowtiie.feedit.ui.viewmodel.SettingsViewModel;
import com.mowtiie.feedit.util.InsetsUtil;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SettingsViewModel viewModel;

    private final ActivityResultLauncher<String[]> openOpmlLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    viewModel.importOpml(uri);
                }
            });

    private final ActivityResultLauncher<String> createOpmlLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("text/xml"), uri -> {
                if (uri != null) {
                    viewModel.exportOpml(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        InsetsUtil.applyEdgeToEdgeInsets(binding.appBarLayout, binding.scrollContent);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        setupListeners();
        observeViewModel();
    }

    private void setupListeners() {
        binding.switchWifiOnly.setOnCheckedChangeListener((button, checked) -> viewModel.setWifiOnly(checked));
        binding.switchNotifications.setOnCheckedChangeListener(
                (button, checked) -> viewModel.setNotificationsEnabled(checked));

        binding.toggleDarkMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            String mode;
            if (checkedId == R.id.button_dark_light) {
                mode = "light";
            } else if (checkedId == R.id.button_dark_dark) {
                mode = "dark";
            } else {
                mode = "system";
            }
            viewModel.setDarkMode(mode);
        });

        binding.buttonImportOpml.setOnClickListener(v -> openOpmlLauncher.launch(new String[]{"*/*"}));
        binding.buttonExportOpml.setOnClickListener(v -> createOpmlLauncher.launch("feedit-subscriptions.opml"));
    }

    private void observeViewModel() {
        viewModel.getWifiOnly().observe(this, checked -> binding.switchWifiOnly.setChecked(checked));
        viewModel.getNotificationsEnabled().observe(this, checked -> binding.switchNotifications.setChecked(checked));

        viewModel.getDarkMode().observe(this, mode -> {
            int id;
            if ("light".equals(mode)) {
                id = R.id.button_dark_light;
            } else if ("dark".equals(mode)) {
                id = R.id.button_dark_dark;
            } else {
                id = R.id.button_dark_system;
            }
            binding.toggleDarkMode.check(id);
        });

        viewModel.getOpmlBusy().observe(this, busy -> {
            binding.progressOpml.setVisibility(busy ? View.VISIBLE : View.GONE);
            binding.buttonImportOpml.setEnabled(!busy);
            binding.buttonExportOpml.setEnabled(!busy);
        });

        viewModel.getOpmlMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
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
}