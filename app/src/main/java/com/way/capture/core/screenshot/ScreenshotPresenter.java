package com.way.capture.core.screenshot;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.MediaActionSound;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.way.capture.App;
import com.way.capture.R;
import com.way.capture.data.DataInfo;
import com.way.capture.fragment.SettingsFragment;
import com.way.capture.utils.LongScreenshotUtil;
import com.way.capture.utils.RxBus;
import com.way.capture.utils.RxEvent;

import rx.Observer;
import rx.Subscriber;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by android on 16-8-19.
 */
public class ScreenshotPresenter implements ScreenshotContract.Presenter {
    private static final String TAG = "ScreenshotPresenter";

    private final Context mContext;
    private final ScreenshotContract.View mView;
    private MediaActionSound mCameraSound;
    private Bitmap mScreenBitmap;
    private CompositeSubscription mSubscriptions;
    private ScreenshotModel mScreenshotModel;

    public ScreenshotPresenter(ScreenshotContract.View view, int resultCode, Intent data) {
        mView = view;

        mScreenshotModel = new ScreenshotModel(resultCode, data);
        mSubscriptions = new CompositeSubscription();
        mContext = App.getContext();

        // Setup the Camera shutter sound
        if (PreferenceManager.getDefaultSharedPreferences(mContext)
                .getBoolean(SettingsFragment.SCREENSHOT_SOUND, true)) {
            mCameraSound = new MediaActionSound();
            mCameraSound.load(MediaActionSound.SHUTTER_CLICK);
        }
    }

    @Override
    public void takeScreenshot() {
        mSubscriptions.clear();
        mSubscriptions.add(mScreenshotModel.getNewBitmap().subscribe(new Observer<Bitmap>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG, "takeScreenshot... onError e = " + e.getMessage());
                mView.showScreenshotError(e);
            }

            @Override
            public void onNext(Bitmap bitmap) {
                Log.d(TAG, "takeScreenshot... onNext bitmap = " + bitmap);
                mScreenBitmap = bitmap;
                mView.showScreenshotAnim(mScreenBitmap, false, true);
            }
        }));

    }

    @Override
    public void playCaptureSound() {
        if (mCameraSound != null)
            mCameraSound.play(MediaActionSound.SHUTTER_CLICK);
    }

    @Override
    public void takeLongScreenshot(boolean isAutoScroll) {
        if (mScreenBitmap == null || mScreenBitmap.isRecycled()) {
            mView.showScreenshotError(new NullPointerException("bitmap is null"));
            return;
        }
        mSubscriptions.clear();
        mSubscriptions.add(mScreenshotModel.takeLongScreenshot(mScreenBitmap, isAutoScroll)
                .subscribe(new Subscriber<Bitmap>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        if (mScreenBitmap != null && !mScreenBitmap.isRecycled())
                            mView.showScreenshotAnim(mScreenBitmap, true, false);
                        else
                            mView.showScreenshotError(e);
                    }

                    @Override
                    public void onNext(Bitmap bitmap) {
                        Log.i("LongScreenshotUtil", "onNext...");
//                        if(mScreenBitmap == null || mScreenBitmap.isRecycled()){
//                            mView.showScreenshotError(new Throwable("bitmap is null..."));
//                            return;
//                        }
                        if(bitmap == null || bitmap.isRecycled()){
                            mView.showScreenshotAnim(mScreenBitmap, true, false);
                        }else if (bitmap.getHeight() == mScreenBitmap.getHeight()
                                || mScreenBitmap.getHeight() / mScreenshotModel.getHeight() > 9) {
                            mView.showScreenshotAnim(bitmap, true, false);
                        } else {
                            mView.onCollageFinish();
                            mScreenBitmap.recycle();
                            mScreenBitmap = bitmap;
                        }
                    }
                }));

    }

    private String getRealPathFromURI(Uri contentUri) {
        String path = null;
        String[] projection = new String[]{MediaStore.Images.Media.DATA};
        Cursor cursor = mContext.getContentResolver().query(contentUri, projection, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst())
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            cursor.close();
        }
        return path;
    }

    @Override
    public void stopLongScreenshot() {
        mSubscriptions.clear();
        LongScreenshotUtil.getInstance().stop();
        if (mScreenBitmap != null && !mScreenBitmap.isRecycled()) {
            mView.showScreenshotAnim(mScreenBitmap, true, false);
        } else {
            mView.showScreenshotError(new Throwable("bitmap is null..."));
        }
    }

    @Override
    public void setBitmap(Bitmap bitmap) {
        if (mScreenBitmap != null && !mScreenBitmap.isRecycled())
            mScreenBitmap.recycle();
        mScreenBitmap = bitmap;
    }

    @Override
    public void release() {
        if (mScreenBitmap != null && !mScreenBitmap.isRecycled())
            mScreenBitmap.recycle();
        if (mCameraSound != null)
            mCameraSound.release();
        mScreenshotModel.release();
    }

    @Override
    public void saveScreenshot(final int style) {
        final Notification.Builder notificationBuilder = initNotificationBuilder();
        if (style == ScreenshotModule.STYLE_SAVE_ONLY) {
            mView.notify(notificationBuilder.build());
        }
        mSubscriptions.clear();
        mSubscriptions.add(mScreenshotModel.saveScreenshot(mScreenBitmap, notificationBuilder).subscribe(new Observer<Uri>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG, "saveScreenshot onError... e = " + e);
                //mView.showScreenshotError(e);
            }

            @Override
            public void onNext(Uri uri) {
                Log.d(TAG, "saveScreenshot onNext...");
                onSaveFinish(uri, notificationBuilder, style);
            }
        }));

    }

    @NonNull
    private Notification.Builder initNotificationBuilder() {
        final Resources r = mContext.getResources();

        final int imageWidth = mScreenBitmap.getWidth();
        final int imageHeight = mScreenBitmap.getHeight();
        final int iconSize = r.getDimensionPixelSize(android.R.dimen.notification_large_icon_height);
        int previewWidth = r.getDimensionPixelSize(R.dimen.notification_panel_width);
        if (previewWidth <= 0) {
            previewWidth = mScreenshotModel.getWidth();
        }
        final int previewHeight = r.getDimensionPixelSize(R.dimen.notification_max_height);

        final Bitmap preview = Bitmap.createBitmap(previewWidth, previewHeight, mScreenBitmap.getConfig());
        Canvas c = new Canvas(preview);
        Paint paint = new Paint();
        ColorMatrix desat = new ColorMatrix();
        desat.setSaturation(0.25f);
        paint.setColorFilter(new ColorMatrixColorFilter(desat));
        Matrix matrix = new Matrix();
        matrix.postTranslate((previewWidth - imageWidth) / 2, (previewHeight - imageHeight) / 2);
        c.drawBitmap(mScreenBitmap, matrix, paint);
        c.drawColor(0x40FFFFFF);
        c.setBitmap(null);

        final Bitmap croppedIcon = Bitmap.createScaledBitmap(preview, iconSize, iconSize, true);

        final long now = System.currentTimeMillis();

        final Notification.Builder notificationBuilder = new Notification.Builder(mContext)
                .setTicker(r.getString(R.string.screenshot_saving_ticker))
                .setContentTitle(r.getString(R.string.screenshot_saving_title))
                .setContentText(r.getString(R.string.screenshot_saving_text)).setSmallIcon(R.drawable.stat_notify_image)
                .setWhen(now).setColor(r.getColor(R.color.system_notification_accent_color));

        Notification.BigPictureStyle notificationStyle = new Notification.BigPictureStyle().bigPicture(preview);
        notificationBuilder.setStyle(notificationStyle);

        Notification n = notificationBuilder.build();
        n.flags |= Notification.FLAG_NO_CLEAR;


        // On the tablet, the large icon makes the notification appear as if it
        // is clickable (and
        // on small devices, the large icon is not shown) so defer showing the
        // large icon until
        // we compose the final post-save notification below.
        notificationBuilder.setLargeIcon(croppedIcon);
        // But we still don't set it for the expanded view, allowing the
        // smallIcon to show here.
        notificationStyle.bigLargeIcon((Bitmap) null);
        return notificationBuilder;
    }

    private void onSaveFinish(Uri uri, Notification.Builder builder, int style) {
        String path = getRealPathFromURI(uri);
        if (!TextUtils.isEmpty(path))
            RxBus.getInstance().post(new RxEvent.NewPathEvent(DataInfo.TYPE_SCREEN_SHOT, path));
        Resources r = mContext.getResources();
        // Create the intent to show the screenshot in gallery
        Intent launchIntent = new Intent(Intent.ACTION_VIEW);
        launchIntent.setDataAndType(uri, "image/png");
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

        final long now = System.currentTimeMillis();

        builder.setContentTitle(r.getString(R.string.screenshot_saved_title))
                .setContentText(r.getString(R.string.screenshot_saved_text))
                .setContentIntent(PendingIntent.getActivity(mContext, 0, launchIntent, 0)).setWhen(now)
                .setAutoCancel(true).setColor(r.getColor(R.color.system_notification_accent_color));

        Notification n = builder.build();
        n.flags &= ~Notification.FLAG_NO_CLEAR;
        switch (style) {
            case ScreenshotModule.STYLE_SAVE_ONLY:
                mView.notify(n);
                mView.finish();
                break;
            case ScreenshotModule.STYLE_SAVE_TO_EDIT:
                mView.editScreenshot(uri);
                break;
            case ScreenshotModule.STYLE_SAVE_TO_SHARE:
                mView.shareScreenshot(uri);
                break;
            default:
                break;
        }

        // Recycle the bitmap data
        if (mScreenBitmap != null && !mScreenBitmap.isRecycled()) {
            mScreenBitmap.recycle();
        }
    }

}
