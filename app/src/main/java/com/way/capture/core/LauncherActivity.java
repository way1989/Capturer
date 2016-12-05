package com.way.capture.core;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.way.capture.R;

public class LauncherActivity extends Activity {

    public static void startCaptureActivity(Context context, String action) {
        Intent intent = new Intent(context, LauncherActivity.class);
        intent.setAction(action);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() == null || getIntent().getAction() == null) {
            finish();
            return;
        }

        try {
            LauncherHelper.fireScreenCaptureIntent(this);
        } catch (Exception e) {
            Toast.makeText(this, R.string.not_support_devices, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LauncherHelper.handleActivityResult(this, requestCode, resultCode, data, getIntent().getAction());
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
