package com.way.capture.module.screenshot;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.way.capture.R;
import com.way.capture.fragment.SettingsFragment;
import com.way.capture.screenshot.DeleteScreenshot;
import com.way.capture.screenshot.LongScreenshotUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by android on 16-8-19.
 */
public class ScreenshotPresenter implements ScreenshotContract.Presenter {
    private static final String TAG = "ScreenshotPresenter";
    private static final String SCREENSHOTS_DIR_NAME = "Screenshots";
    private static final String SCREENSHOT_FILE_NAME_TEMPLATE = "Screenshot_%s.png";
    private static final String SCREENSHOT_SHARE_SUBJECT_TEMPLATE = "Screenshot (%s)";
    private static final String DISPLAY_NAME = "Screenshot";
    private final Context mContext;
    private final ScreenshotContract.View mScreenshotView;
    private Display mDisplay;
    private DisplayMetrics mDisplayMetrics;
    private Matrix mDisplayMatrix;
    private int mLastRotation;
    private MediaActionSound mCameraSound;
    private Bitmap mScreenBitmap;
    private boolean isStopLongScreenshot;
    private MediaProjection mProjection;
    private VirtualDisplay mVirtualDisplay;
    private ImageReader mImageReader;

    public ScreenshotPresenter(Context context, ScreenshotContract.View screenshotView,
                               int resultCode, Intent data) {
        mContext = context;
        mScreenshotView = screenshotView;

        mDisplayMatrix = new Matrix();
        mDisplay = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        mDisplayMetrics = new DisplayMetrics();
        mDisplay.getRealMetrics(mDisplayMetrics);

        MediaProjectionManager projectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mProjection = projectionManager.getMediaProjection(resultCode, data);

        // Setup the Camera shutter sound
        if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(SettingsFragment.SCREENSHOT_SOUND, true)) {
            mCameraSound = new MediaActionSound();
            mCameraSound.load(MediaActionSound.SHUTTER_CLICK);
        }
    }

    @Override
    public void takeScreenshot() {
        mDisplay.getRealMetrics(mDisplayMetrics);
        mLastRotation = mDisplay.getRotation();//keep the first rotation
        Observable.create(new Observable.OnSubscribe<Bitmap>() {
            @Override
            public void call(final Subscriber<? super Bitmap> subscriber) {
                capture(subscriber);
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Bitmap>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                        mScreenshotView.showScreenshotError(e);
                    }

                    @Override
                    public void onNext(Bitmap bitmap) {
                        mScreenBitmap = bitmap;
                        mScreenshotView.showScreenshotAnim(mScreenBitmap, false);
                    }
                });
    }

    private void capture(final Subscriber<? super Bitmap> subscriber) {
        mImageReader = ImageReader.newInstance(mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels,
                PixelFormat.RGBA_8888, 1);// 只获取一张图片
        mVirtualDisplay = mProjection.createVirtualDisplay(DISPLAY_NAME,
                mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels, mDisplayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION, mImageReader.getSurface(), null, null);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                mImageReader.setOnImageAvailableListener(null, null);// 移除监听

                Image image = mImageReader.acquireLatestImage();
                if (image == null) {
                    subscriber.onError(new NullPointerException("image is null..."));
                    return;
                }
                int imageWidth = mDisplayMetrics.widthPixels;
                int imageHeight = image.getHeight();
                Image.Plane[] planes = image.getPlanes();
                ByteBuffer byteBuffer = planes[0].getBuffer();
                int pixelStride = planes[0].getPixelStride();
                int rowStride = planes[0].getRowStride() - pixelStride * imageWidth;
                Bitmap bitmap = Bitmap.createBitmap(imageWidth + rowStride / pixelStride, imageHeight,
                        Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(byteBuffer);
                if (rowStride != 0) {
                    bitmap = addBorder(bitmap, -(rowStride / pixelStride));
                }
                image.close();


                if (bitmap == null || bitmap.isRecycled()) {
                    subscriber.onError(new NullPointerException("bitmap is null"));
                } else {
                    subscriber.onNext(bitmap);
                    subscriber.onCompleted();
                }
            }
        }, new Handler(Looper.getMainLooper()));
    }

    private Bitmap addBorder(Bitmap bitmap, int width) {
        Bitmap newBitmap = Bitmap.createBitmap(width + bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas = new Canvas(newBitmap);
        canvas.drawColor(Color.TRANSPARENT);
        canvas.drawBitmap(bitmap, 0.0F, 0.0F, null);
        return newBitmap;
    }

    @Override
    public void playCaptureSound() {
        if (mCameraSound != null)
            mCameraSound.play(MediaActionSound.SHUTTER_CLICK);
    }

    @Override
    public void takeLongScreenshot() {
        if (mScreenBitmap == null || mScreenBitmap.isRecycled()) {
            //notifyScreenshotError(mContext, mNotificationManager);
            mScreenshotView.showScreenshotError(new NullPointerException("screenshot bitmap is null"));
            return;
        }

        mDisplay.getRealMetrics(mDisplayMetrics);
        int rotation = mDisplay.getRotation();
        if (isStopLongScreenshot || rotation != mLastRotation) {
            mScreenshotView.showScreenshotAnim(mScreenBitmap, true);
            return;
        }

        scrollNextScreen().flatMap(new Func1<Boolean, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(Boolean aBoolean) {
                return sleepForWhile();
            }
        }).flatMap(new Func1<Boolean, Observable<Bitmap>>() {
            @Override
            public Observable<Bitmap> call(Boolean succeed) {
                return collageLongBitmap();
            }
        }).map(new Func1<Bitmap, Bitmap>() {
            @Override
            public Bitmap call(Bitmap bitmap) {
                return takeScreenshotByScroll(bitmap);
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Bitmap>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        if (mScreenBitmap != null && !mScreenBitmap.isRecycled())
                            mScreenshotView.showScreenshotAnim(mScreenBitmap, true);
                        else
                            mScreenshotView.showScreenshotError(e);
                    }

                    @Override
                    public void onNext(Bitmap bitmap) {
                        Log.i("LongScreenshotUtil", "onNext...");
                        if (bitmap.getHeight() == mScreenBitmap.getHeight()
                                || mScreenBitmap.getHeight() / mDisplayMetrics.heightPixels > 9) {
                            mScreenshotView.showScreenshotAnim(bitmap, true);
                        } else {
                            mScreenshotView.onCollageFinish();
                            mScreenBitmap.recycle();
                            mScreenBitmap = bitmap;
                        }
                    }
                });
    }

    @Override
    public void stopLongScreenshot() {
        isStopLongScreenshot = true;
        LongScreenshotUtil.getInstance().stop();
    }

    @Override
    public void release() {
        isStopLongScreenshot = false;
        if (mScreenBitmap != null && !mScreenBitmap.isRecycled())
            mScreenBitmap.recycle();
        if (mCameraSound != null)
            mCameraSound.release();
        if (mImageReader != null)
            mImageReader.close();
        if (mVirtualDisplay != null)
            mVirtualDisplay.release();
        if (mProjection != null) {
            mProjection.stop();
        }
    }

    @Override
    public void saveScreenshot(final int style) {
        final Resources r = mContext.getResources();
        final long imageTime = System.currentTimeMillis();
        final String imageDate = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date(imageTime));
        final String imageFileName = String.format(SCREENSHOT_FILE_NAME_TEMPLATE, imageDate);
        final File screenshotDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                SCREENSHOTS_DIR_NAME);
        final String imageFilePath = new File(screenshotDir, imageFileName).getAbsolutePath();

        final int imageWidth = mScreenBitmap.getWidth();
        final int imageHeight = mScreenBitmap.getHeight();
        final int iconSize = r.getDimensionPixelSize(android.R.dimen.notification_large_icon_height);
        int previewWidth = r.getDimensionPixelSize(R.dimen.notification_panel_width);
        if (previewWidth <= 0) {
            // includes notification_panel_width==match_parent (-1)
            previewWidth = mDisplayMetrics.widthPixels;
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

        // For "public" situations we want to show all the same info but
        // omit the actual screenshot image.
        final Notification.Builder publicNotificationBuilder = new Notification.Builder(mContext)
                .setContentTitle(r.getString(R.string.screenshot_saving_title))
                .setContentText(r.getString(R.string.screenshot_saving_text)).setSmallIcon(R.drawable.stat_notify_image)
                .setCategory(Notification.CATEGORY_PROGRESS).setWhen(now)
                .setColor(r.getColor(R.color.system_notification_accent_color));

        notificationBuilder.setPublicVersion(publicNotificationBuilder.build());

        Notification n = notificationBuilder.build();
        n.flags |= Notification.FLAG_NO_CLEAR;
        if (style == ScreenshotModule.STYLE_SAVE_ONLY)
            mScreenshotView.notify(n);

        // On the tablet, the large icon makes the notification appear as if it
        // is clickable (and
        // on small devices, the large icon is not shown) so defer showing the
        // large icon until
        // we compose the final post-save notification below.
        notificationBuilder.setLargeIcon(croppedIcon);
        // But we still don't set it for the expanded view, allowing the
        // smallIcon to show here.
        notificationStyle.bigLargeIcon((Bitmap) null);

        Observable.create(new Observable.OnSubscribe<Uri>() {
            @Override
            public void call(Subscriber<? super Uri> subscriber) {
                saveInWorkThread(subscriber, screenshotDir, imageTime, imageFilePath,
                        imageFileName, imageWidth, imageHeight, r, notificationBuilder);

            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Uri>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Uri uri) {
                        onSaveFinish(uri, notificationBuilder, r, publicNotificationBuilder, style);

                    }
                });
    }

    private void saveInWorkThread(Subscriber<? super Uri> subscriber, File screenshotDir,
                                  long imageTime, String imageFilePath, String imageFileName,
                                  int imageWidth, int imageHeight, Resources r,
                                  Notification.Builder notificationBuilder) {
        // By default, AsyncTask sets the worker thread to have background
        // thread priority, so bump
        // it back up so that we save a little quicker.
        Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);
        try {
            // Create screenshot directory if it doesn't exist
            screenshotDir.mkdirs();

            // media provider uses seconds for DATE_MODIFIED and DATE_ADDED, but
            // milliseconds
            // for DATE_TAKEN
            long dateSeconds = imageTime / 1000;

            // Save
            OutputStream out = new FileOutputStream(imageFilePath);
            mScreenBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

            // Save the screenshot to the MediaStore
            ContentValues values = new ContentValues();
            ContentResolver resolver = mContext.getContentResolver();
            values.put(MediaStore.Images.ImageColumns.DATA, imageFilePath);
            values.put(MediaStore.Images.ImageColumns.TITLE, imageFileName);
            values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, imageFileName);
            values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, imageTime);
            values.put(MediaStore.Images.ImageColumns.DATE_ADDED, dateSeconds);
            values.put(MediaStore.Images.ImageColumns.DATE_MODIFIED, dateSeconds);
            values.put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.ImageColumns.WIDTH, imageWidth);
            values.put(MediaStore.Images.ImageColumns.HEIGHT, imageHeight);
            values.put(MediaStore.Images.ImageColumns.SIZE, new File(imageFilePath).length());
            Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            // Create a share intent
            String subjectDate = DateFormat.getDateTimeInstance().format(new Date(imageTime));
            String subject = String.format(SCREENSHOT_SHARE_SUBJECT_TEMPLATE, subjectDate);
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("image/png");
            sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, subject);

            Intent chooserIntent = Intent.createChooser(sharingIntent, null);
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

            notificationBuilder.addAction(R.drawable.ic_menu_share, r.getString(R.string.share),
                    PendingIntent.getActivity(mContext, 0, chooserIntent, PendingIntent.FLAG_CANCEL_CURRENT));

            Intent deleteIntent = new Intent();
            deleteIntent.setClass(mContext, DeleteScreenshot.class);
            deleteIntent.putExtra(DeleteScreenshot.SCREENSHOT_URI, uri.toString());

            notificationBuilder.addAction(R.drawable.ic_menu_delete, r.getString(R.string.screenshot_delete_action),
                    PendingIntent.getBroadcast(mContext, 0, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT));
            subscriber.onNext(uri);
            subscriber.onCompleted();
        } catch (Exception e) {
            // IOException/UnsupportedOperationException may be thrown if external storage is not mounted
            subscriber.onError(e);
        }

        // Recycle the bitmap data
        if (mScreenBitmap != null && !mScreenBitmap.isRecycled()) {
            mScreenBitmap.recycle();
        }
    }

    private void onSaveFinish(Uri uri, Notification.Builder builder, Resources r,
                              Notification.Builder publicNotificationBuilder, int style) {
        // Create the intent to show the screenshot in gallery
        Intent launchIntent
                //= new Intent(mContext, ImageEditorActivity.class);
                = new Intent(Intent.ACTION_VIEW);
        launchIntent.setDataAndType(uri, "image/png");
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

        final long now = System.currentTimeMillis();

        builder.setContentTitle(r.getString(R.string.screenshot_saved_title))
                .setContentText(r.getString(R.string.screenshot_saved_text))
                .setContentIntent(PendingIntent.getActivity(mContext, 0, launchIntent, 0)).setWhen(now)
                .setAutoCancel(true).setColor(r.getColor(R.color.system_notification_accent_color));

        // Update the text in the public version as well
        publicNotificationBuilder.setContentTitle(r.getString(R.string.screenshot_saved_title))
                .setContentText(r.getString(R.string.screenshot_saved_text))
                .setContentIntent(PendingIntent.getActivity(mContext, 0, launchIntent, 0)).setWhen(now)
                .setAutoCancel(true).setColor(r.getColor(R.color.system_notification_accent_color));

        builder.setPublicVersion(publicNotificationBuilder.build());

        Notification n = builder.build();
        n.flags &= ~Notification.FLAG_NO_CLEAR;
        switch (style) {
            case ScreenshotModule.STYLE_SAVE_ONLY:
                mScreenshotView.notify(n);
                mScreenshotView.finish();
                break;
            case ScreenshotModule.STYLE_SAVE_TO_EDIT:
                mScreenshotView.editScreenshot(uri);
                break;
            case ScreenshotModule.STYLE_SAVE_TO_SHARE:
                mScreenshotView.shareScreenshot(uri);
                break;
            default:
                break;
        }
    }

    private Observable<Boolean> scrollNextScreen() {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {

            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                Log.i(TAG, "scrollNextScreen... screenHeight = " + mDisplayMetrics.heightPixels
                        + ", " + Thread.currentThread());
                try {
                    ScrollUtils.scrollToNextScreen(mDisplayMetrics.heightPixels, 800L);//scroll
                    subscriber.onNext(true);
                    subscriber.onCompleted();
                } catch (IOException e) {
                    e.printStackTrace();
                    subscriber.onError(e);
                }

            }
        });
    }

    private Observable<Boolean> sleepForWhile() {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {

            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                Log.i(TAG, "sleepForWhile... " + Thread.currentThread());
                try {
                    // Do some long running operation
                    Thread.sleep(TimeUnit.MILLISECONDS.toMillis(1000));
                    subscriber.onNext(true);
                    subscriber.onCompleted();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    subscriber.onError(e);
                }

            }
        });
    }

    private Observable<Bitmap> collageLongBitmap() {
        return Observable.create(new Observable.OnSubscribe<Bitmap>() {
            @Override
            public void call(Subscriber<? super Bitmap> subscriber) {
                capture(subscriber);
            }
        });
    }

    private Bitmap takeScreenshotByScroll(Bitmap newBitmap) {
        if (mScreenBitmap == null || mScreenBitmap.isRecycled()) {
            throw new NullPointerException("mScreenBitmap is null");
        }

        if (newBitmap == null || newBitmap.isRecycled()) {
            return mScreenBitmap;
        }

        //tmp save the last bitmap
        final Bitmap oldBitmap = mScreenBitmap;

        //collage a new bitmap
        Bitmap collageBitmap = LongScreenshotUtil.getInstance()
                .collageLongBitmap(oldBitmap, newBitmap);

        if (collageBitmap == null || collageBitmap.isRecycled()) {
            return oldBitmap;
        }
        return collageBitmap;
    }
}
