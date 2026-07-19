package com.mowtiie.feedit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mowtiie.feedit.R;
import com.mowtiie.feedit.databinding.ActivitySettingsBinding;
import com.mowtiie.feedit.ui.viewmodel.SettingsViewModel;
import com.mowtiie.feedit.util.BatteryOptimizationHelper;
import com.mowtiie.feedit.util.InsetsUtil;
import com.mowtiie.feedit.util.PrefsKeys;

public class SettingsActivity extends FeedItActivity {

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
        setTitle(R.string.title_settings);

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

        binding.toggleStartupPage.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            String page;
            if (checkedId == R.id.button_startup_starred) {
                page = PrefsKeys.STARTUP_PAGE_STARRED;
            } else {
                page = PrefsKeys.STARTUP_PAGE_ALL;
            }
            viewModel.setStartupPage(page);
        });

        binding.toggleContrast.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            String level;
            if (checkedId == R.id.button_contrast_medium) {
                level = PrefsKeys.CONTRAST_MEDIUM;
            } else if (checkedId == R.id.button_contrast_high) {
                level = PrefsKeys.CONTRAST_HIGH;
            } else {
                level = PrefsKeys.CONTRAST_STANDARD;
            }
            boolean changed = !level.equals(viewModel.getContrastLevel().getValue());
            viewModel.setContrastLevel(level);
            if (changed) {
                recreate();
            }
        });

        binding.switchDynamicColors.setOnCheckedChangeListener((button, checked) -> {
            boolean actuallyChanged = checked != Boolean.TRUE.equals(viewModel.getDynamicColorsEnabled().getValue());
            viewModel.setDynamicColorsEnabled(checked);
            if (actuallyChanged) {
                recreate();
            }
        });

        binding.rowArticleLayout.setOnClickListener(v -> showArticleLayoutDialog());
        binding.rowFeedNotifications.setOnClickListener(v -> startActivity(new Intent(this, FeedNotificationSettingsActivity.class)));
        binding.rowSyncFrequency.setOnClickListener(v -> showSyncFrequencyDialog());
        binding.rowBatteryOptimization.setOnClickListener(v -> handleBatteryOptimizationClick());

        binding.buttonImportOpml.setOnClickListener(v -> openOpmlLauncher.launch(new String[]{"*/*"}));
        binding.buttonExportOpml.setOnClickListener(v -> createOpmlLauncher.launch("feedit-subscriptions.opml"));
    }

    private void handleBatteryOptimizationClick() {
        Intent intent = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)
                ? BatteryOptimizationHelper.buildAppSettingsIntent(this)
                : BatteryOptimizationHelper.buildRequestExemptionIntent(this);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBatteryOptimizationRow();
    }

    private void updateBatteryOptimizationRow() {
        boolean unrestricted = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this);
        binding.textBatteryOptimizationValue.setText(unrestricted
                ? R.string.battery_optimization_unrestricted
                : R.string.battery_optimization_restricted);
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

        viewModel.getStartupPage().observe(this, page -> {
            int id;
            if (PrefsKeys.STARTUP_PAGE_STARRED.equals(page)) {
                id = R.id.button_startup_starred;
            } else {
                id = R.id.button_startup_all;
            }
            binding.toggleStartupPage.check(id);
        });

        viewModel.getContrastLevel().observe(this, level -> {
            int id;
            if (PrefsKeys.CONTRAST_MEDIUM.equals(level)) {
                id = R.id.button_contrast_medium;
            } else if (PrefsKeys.CONTRAST_HIGH.equals(level)) {
                id = R.id.button_contrast_high;
            } else {
                id = R.id.button_contrast_standard;
            }
            binding.toggleContrast.check(id);
        });

        viewModel.getDynamicColorsEnabled().observe(this, enabled -> binding.switchDynamicColors.setChecked(enabled));

        viewModel.getArticleLayoutStyle().observe(this, style ->
                binding.textArticleLayoutValue.setText(articleLayoutDisplayName(style)));

        viewModel.getSyncIntervalMinutes().observe(this, minutes ->
                binding.textSyncFrequencyValue.setText(syncFrequencyDisplayName(minutes)));

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

    private void showArticleLayoutDialog() {
        String[] values = {
                PrefsKeys.LAYOUT_LIST, PrefsKeys.LAYOUT_COMPACT, PrefsKeys.LAYOUT_CARD,
                PrefsKeys.LAYOUT_MAGAZINE
        };
        String[] labels = {
                getString(R.string.option_layout_list), getString(R.string.option_layout_compact),
                getString(R.string.option_layout_card), getString(R.string.option_layout_magazine)
        };

        String current = viewModel.getArticleLayoutStyle().getValue();
        int checkedIndex = 2;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(current)) {
                checkedIndex = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_article_layout_title)
                .setSingleChoiceItems(labels, checkedIndex, (dialog, which) -> {
                    viewModel.setArticleLayoutStyle(values[which]);
                    dialog.dismiss();
                })
                .show();
    }

    private String articleLayoutDisplayName(String style) {
        if (PrefsKeys.LAYOUT_LIST.equals(style)) {
            return getString(R.string.option_layout_list);
        } else if (PrefsKeys.LAYOUT_COMPACT.equals(style)) {
            return getString(R.string.option_layout_compact);
        } else if (PrefsKeys.LAYOUT_MAGAZINE.equals(style)) {
            return getString(R.string.option_layout_magazine);
        } else {
            return getString(R.string.option_layout_card);
        }
    }

    private void showSyncFrequencyDialog() {
        long[] values = {15L, 30L, 60L, 120L, 240L, 360L, 720L};
        String[] labels = {
                getString(R.string.option_sync_15min), getString(R.string.option_sync_30min),
                getString(R.string.option_sync_1hour), getString(R.string.option_sync_2hours),
                getString(R.string.option_sync_4hours), getString(R.string.option_sync_6hours),
                getString(R.string.option_sync_12hours)
        };

        Long current = viewModel.getSyncIntervalMinutes().getValue();
        int checkedIndex = 2;
        if (current != null) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] == current) {
                    checkedIndex = i;
                    break;
                }
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_sync_frequency_title)
                .setSingleChoiceItems(labels, checkedIndex, (dialog, which) -> {
                    viewModel.setSyncIntervalMinutes(values[which]);
                    dialog.dismiss();
                })
                .show();
    }

    private String syncFrequencyDisplayName(Long minutes) {
        if (minutes == null) {
            return getString(R.string.option_sync_1hour);
        }
        if (minutes == 15L) {
            return getString(R.string.option_sync_15min);
        } else if (minutes == 30L) {
            return getString(R.string.option_sync_30min);
        } else if (minutes == 120L) {
            return getString(R.string.option_sync_2hours);
        } else if (minutes == 240L) {
            return getString(R.string.option_sync_4hours);
        } else if (minutes == 360L) {
            return getString(R.string.option_sync_6hours);
        } else if (minutes == 720L) {
            return getString(R.string.option_sync_12hours);
        } else {
            return getString(R.string.option_sync_1hour);
        }
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