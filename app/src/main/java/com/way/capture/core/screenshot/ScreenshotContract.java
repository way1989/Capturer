package com.way.capture.core.screenshot;

import android.app.Notification;
import android.graphics.Bitmap;
import android.net.Uri;

import io.reactivex.Observable;


/**
 * Created by android on 16-8-19.
 */
public interface ScreenshotContract {
    interface Model {
        Observable<Bitmap> getNewBitmap();

        Observable<Bitmap> takeLongScreenshot(Bitmap oldBitmap, boolean isAutoScroll);

        Observable<Uri> saveScreenshot(Bitmap bitmap, Notification.Builder notificationBuilder);

        void release();
    }

    interface View {
        void showScreenshotAnim(Bitmap bitmap, boolean longScreenshot, boolean needCheckAction);

        void showScreenshotError(Throwable e);

        void onCollageFinish();

        void notify(Notification notification);

        void editScreenshot(Uri uri);

        void shareScreenshot(Uri uri);

        void finish();
    }

    interface Presenter {
        void takeScreenshot();

        void playCaptureSound();

        void takeLongScreenshot(boolean isAutoScroll);

        void saveScreenshot(int style);

        void release();

        void stopLongScreenshot();

        void setBitmap(Bitmap bitmap);
    }
}
