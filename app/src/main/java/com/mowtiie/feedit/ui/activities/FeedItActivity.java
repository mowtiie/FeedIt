package com.mowtiie.feedit.ui.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.mowtiie.feedit.util.ThemeManager;

public abstract class FeedItActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyPersistedContrast(this);
        super.onCreate(savedInstanceState);
    }
}