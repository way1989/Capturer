package com.isseiaoki.simplecropview;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.way.captain.fragment.SettingsFragment;

public class TakeScreenshotActivity extends Activity {
    private String mAction;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAction = getIntent().getAction();
        ScreenshotHelper.fireScreenCaptureIntent(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!ScreenshotHelper.handleActivityResult(this, requestCode, resultCode, data, mAction)) {
            super.onActivityResult(requestCode, resultCode, data);
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(SettingsFragment.HIDE_FLOATVIEW_KEY, false).apply();

        }
        finish();
    }

    @Override
    protected void onStop() {
        if (!isFinishing()) {
            finish();
        }
        super.onStop();
    }
}
