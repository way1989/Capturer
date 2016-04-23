package com.way.capture.screenshot.crop;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.util.Log;

final class CropScreenshotHelper {
    private static final int CREATE_SCREENSHOT = 100;

    private CropScreenshotHelper() {
        throw new AssertionError("No instances.");
    }

    static void fireScreenCaptureIntent(Activity activity) {
        MediaProjectionManager manager = (MediaProjectionManager) activity
                .getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent intent = manager.createScreenCaptureIntent();
        activity.startActivityForResult(intent, CREATE_SCREENSHOT);
        Log.i("TakeCropScreenshotService", "fireScreenCaptureIntent...");
    }

    static boolean handleActivityResult(Activity activity, int requestCode, int resultCode, Intent data, String action) {
        Log.i("TakeCropScreenshotService", "handleActivityResult... requestCode = " + requestCode);
        if (requestCode != CREATE_SCREENSHOT) {
            return false;
        }
        if (resultCode == Activity.RESULT_OK) {
            Log.d("TakeCropScreenshotService", "Acquired permission to screen capture. Starting service.");
            activity.startService(TakeCropScreenshotService.newIntent(activity, resultCode, data, action));
        } else {
            Log.d("TakeCropScreenshotService", "Failed to acquire permission to screen capture.");
            return false;
        }
        return true;
    }
}
