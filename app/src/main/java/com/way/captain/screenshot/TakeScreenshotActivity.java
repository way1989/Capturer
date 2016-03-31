package com.way.captain.screenshot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.way.captain.fragment.SettingsFragment;

public class TakeScreenshotActivity extends Activity {
    private boolean mIsLongScreenshot;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIsLongScreenshot = TextUtils.equals(TakeScreenshotService.ACTION_LONG_SCREENSHOT, getIntent().getAction());
        Log.i("broncho", "TakeScreenshotActivity onCreate mIsLongScreenshot = " + mIsLongScreenshot);
        ScreenshotHelper.fireScreenCaptureIntent(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!ScreenshotHelper.handleActivityResult(this, requestCode, resultCode, data, mIsLongScreenshot)) {
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
