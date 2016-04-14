package com.way.captain.screenshot.crop;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class TakeCropScreenshotActivity extends Activity {
    private String mAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAction = getIntent().getAction();
        CropScreenshotHelper.fireScreenCaptureIntent(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!CropScreenshotHelper.handleActivityResult(this, requestCode, resultCode, data, mAction)) {
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
