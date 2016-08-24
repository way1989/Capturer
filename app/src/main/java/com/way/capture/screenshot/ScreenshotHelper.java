package com.way.capture.screenshot;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.util.Log;

import com.way.capture.module.ModuleService;

final class ScreenshotHelper {
    private static final String TAG = "ScreenshotHelper";
    private static final int CREATE_SCREENSHOT = 100;

    private ScreenshotHelper() {
        throw new AssertionError("No instances.");
    }

    static void fireScreenCaptureIntent(Activity activity) {
        MediaProjectionManager manager = (MediaProjectionManager) activity
                .getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent intent = manager.createScreenCaptureIntent();
        activity.startActivityForResult(intent, CREATE_SCREENSHOT);
        Log.i(TAG, "fireScreenCaptureIntent...");
    }

    static boolean handleActivityResult(Activity activity, int requestCode, int resultCode, Intent data, String action) {
        Log.i(TAG, "handleActivityResult... requestCode = " + requestCode);
        if (requestCode != CREATE_SCREENSHOT) {
            return false;
        }
        if (resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "Acquired permission to screen capture. Starting service.");
            activity.startService(ModuleService.newIntent(activity, action, resultCode, data));
        } else {
            Log.d(TAG, "Failed to acquire permission to screen capture.");
            return false;
        }
        return true;
    }
}
