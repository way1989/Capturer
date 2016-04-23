package com.way.capture.screenshot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

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
