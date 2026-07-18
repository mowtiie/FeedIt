package com.mowtiie.feedit.ui.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mowtiie.feedit.BuildConfig;
import com.mowtiie.feedit.R;
import com.mowtiie.feedit.databinding.ActivityAboutBinding;
import com.mowtiie.feedit.util.InsetsUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class AboutActivity extends FeedItActivity {

    private ActivityAboutBinding binding;

    private int easterEggCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        InsetsUtil.applyEdgeToEdgeInsets(binding.appBarLayout, binding.scrollContent);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setTitle(R.string.title_about);

        binding.textVersionValue.setText(BuildConfig.VERSION_NAME);

        binding.rowVersion.setOnClickListener(v -> onVersionClicked());
        binding.rowLicense.setOnClickListener(v -> showLicenseDialog());
        binding.rowSourceCode.setOnClickListener(v -> openUrl(R.string.url_source_code));
        binding.rowSupport.setOnClickListener(v -> openUrl(R.string.url_support));
        binding.rowAuthor.setOnClickListener(v -> openUrl(R.string.url_author));
    }

    private void onVersionClicked() {
        easterEggCounter++;
        if (easterEggCounter == 7) {
            Toast.makeText(this, R.string.app_easter_egg, Toast.LENGTH_SHORT).show();
        }
    }

    private void showLicenseDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.preferences_app_license)
                .setMessage(readLicenseFromAssets())
                .setPositiveButton(R.string.button_close, null)
                .show();
    }

    private String readLicenseFromAssets() {
        StringBuilder stringBuilder = new StringBuilder();
        AssetManager assetManager = getAssets();

        try (InputStream inputStream = assetManager.open("license.txt");
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
        } catch (IOException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return stringBuilder.toString();
    }

    private void openUrl(int urlStringRes) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(urlStringRes))));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No browser app found", Toast.LENGTH_SHORT).show();
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