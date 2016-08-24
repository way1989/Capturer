package com.way.capture.module.screenshot;

import android.app.Notification;
import android.graphics.Bitmap;
import android.net.Uri;

/**
 * Created by android on 16-8-19.
 */
public interface ScreenshotContract {
    interface View<Persenter>{
        void showScreenshotAnim(Bitmap bitmap, boolean longScreenshot);
        void showScreenshotError(Throwable e);

        void onCollageFinish();
        void notify(Notification notification);
        void editScreenshot(Uri uri);
        void shareScreenshot(Uri uri);
        void finish();
    }

    interface Presenter{
        void takeScreenshot();
        void playCaptureSound();
        void takeLongScreenshot();
        void saveScreenshot(int style);
        void release();

        void stopLongScreenshot();
    }
}
