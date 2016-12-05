package com.way.capture.screenshot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.way.capture.R;
import com.way.capture.module.ModuleService;

public class TakeScreenshotActivity extends Activity {
    private String mAction;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getIntent() == null || getIntent().getAction() == null)
            mAction = ModuleService.Action.ACTION_SCREENSHOT;
        else
            mAction = getIntent().getAction();
        try {
            ScreenshotHelper.fireScreenCaptureIntent(this);
        } catch (Exception e) {
            Toast.makeText(this, R.string.not_support_devices, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ScreenshotHelper.handleActivityResult(this, requestCode, resultCode, data, mAction);
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
