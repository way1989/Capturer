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
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.way.capture.App;
import com.way.capture.R;
import com.way.capture.screenshot.DeleteScreenshot;
import com.way.capture.screenshot.LongScreenshotUtil;
import com.way.capture.utils.OsUtil;
import com.way.capture.utils.RxSchedulers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import rx.Emitter;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Cancellable;
import rx.functions.Func1;


/**
 * Created by android on 16-11-23.
 */

public class ScreenshotModel implements ScreenshotContract.Model {
    private static final String TAG = "ScreenshotModel";
    private static final String DISPLAY_NAME = "Screenshot";
    private static final long SCROLL_DURATION = 1000L;
    private static final long WAIT_FOR_SCROLL_TIME = 2000L;
    private static final String SCREENSHOTS_DIR_NAME = "Screenshots";
    private static final String SCREENSHOT_FILE_NAME_TEMPLATE = "Screenshot_%s.png";
    private static final String SCREENSHOT_SHARE_SUBJECT_TEMPLATE = "Screenshot (%s)";
    private MediaProjection mProjection;
    private VirtualDisplay mVirtualDisplay;
    private ImageReader mImageReader;
    private int mLastRotation;

    public ScreenshotModel(int resultCode, Intent data) {
        MediaProjectionManager projectionManager = (MediaProjectionManager) App.getContext()
                .getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mProjection = projectionManager.getMediaProjection(resultCode, data);
    }

    @Override
    public Observable<Bitmap> getNewBitmap() {
        Log.d(TAG, "getNewBitmap...");
        return Observable.fromEmitter(new Action1<Emitter<Bitmap>>() {
            @Override
            public void call(final Emitter<Bitmap> bitmapEmitter) {
                ImageReader.OnImageAvailableListener listener = new ImageReader.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader reader) {
                        Image image = mImageReader.acquireLatestImage();
                        if (image == null) {
                            bitmapEmitter.onError(new Exception("image is null..."));
                            return;
                        }
                        int imageWidth = getWidth();
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
                            bitmapEmitter.onError(new Exception("bitmap is null"));
                        } else {
                            bitmapEmitter.onNext(bitmap);
                            bitmapEmitter.onCompleted();
                        }
                    }
                };
                bitmapEmitter.setCancellation(new Cancellable() {
                    @Override
                    public void cancel() throws Exception {
                        mImageReader.setOnImageAvailableListener(null, null);
                    }
                });
                mLastRotation = getRotation();
                mImageReader = ImageReader.newInstance(getWidth(), getHeight(), PixelFormat.RGBA_8888, 1);// 只获取一张图片
                mVirtualDisplay = mProjection.createVirtualDisplay(DISPLAY_NAME, getWidth(), getHeight(),
                        getDensityDpi(), DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION,
                        mImageReader.getSurface(), null, null);
                mImageReader.setOnImageAvailableListener(listener, null);
            }
        }, Emitter.BackpressureMode.LATEST).timeout(2, TimeUnit.SECONDS)//2s out time
                .observeOn(AndroidSchedulers.mainThread()).subscribeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Observable<Bitmap> takeLongScreenshot(final Bitmap oldBitmap, boolean isAutoScroll) {
        int rotation = getRotation();
        if (rotation != mLastRotation) {
            return Observable.error(new Throwable("device rotation is change..."));
        }
        Observable<Boolean> observable;
        if (isAutoScroll) {
            observable = scrollNextScreen(SCROLL_DURATION).delay(WAIT_FOR_SCROLL_TIME, TimeUnit.MILLISECONDS);
        } else {
            observable = Observable.just(true);
        }
        return observable.flatMap(new Func1<Boolean, Observable<Bitmap>>() {
            @Override
            public Observable<Bitmap> call(Boolean aBoolean) {
                return getNewBitmap();
            }
        }).map(new Func1<Bitmap, Bitmap>() {
            @Override
            public Bitmap call(Bitmap bitmap) {
                return collageLongBitmap(oldBitmap, bitmap);
            }
        }).filter(new Func1<Bitmap, Boolean>() {
            @Override
            public Boolean call(Bitmap bitmap) {
                return bitmap != null && !bitmap.isRecycled();
            }
        }).compose(RxSchedulers.<Bitmap>io_main());
    }

    @Override
    public Observable<Uri> saveScreenshot(final Bitmap bitmap, final Notification.Builder notificationBuilder) {
        return Observable.create(new Observable.OnSubscribe<Uri>() {
            @Override
            public void call(Subscriber<? super Uri> subscriber) {
                OutputStream out = null;
                try {
                    Context context = App.getContext();
                    Resources r = context.getResources();
                    final long imageTime = System.currentTimeMillis();
                    final String imageDate = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date(imageTime));

                    final String imageFileName = String.format(SCREENSHOT_FILE_NAME_TEMPLATE, imageDate);
                    final File screenshotDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                            SCREENSHOTS_DIR_NAME);
                    final String imageFilePath = new File(screenshotDir, imageFileName).getAbsolutePath();

                    final int imageWidth = bitmap.getWidth();
                    final int imageHeight = bitmap.getHeight();

                    // Create screenshot directory if it doesn't exist
                    screenshotDir.mkdirs();

                    // media provider uses seconds for DATE_MODIFIED and DATE_ADDED, but
                    // milliseconds
                    // for DATE_TAKEN
                    long dateSeconds = imageTime / 1000;

                    // Save
                    out = new FileOutputStream(imageFilePath);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    out.flush();

                    // Save the screenshot to the MediaStore
                    ContentValues values = new ContentValues();
                    ContentResolver resolver = context.getContentResolver();
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

                    if (uri != null) {
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
                                PendingIntent.getActivity(context, 0, chooserIntent, PendingIntent.FLAG_CANCEL_CURRENT));

                        Intent deleteIntent = new Intent();
                        deleteIntent.setClass(context, DeleteScreenshot.class);
                        deleteIntent.putExtra(DeleteScreenshot.SCREENSHOT_URI, uri.toString());
                        notificationBuilder.addAction(R.drawable.ic_menu_delete, r.getString(R.string.screenshot_delete_action),
                                PendingIntent.getBroadcast(context, 0, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT));
                        subscriber.onNext(uri);
                        subscriber.onCompleted();
                    } else {
                        subscriber.onError(new Throwable("save uri is null..."));
                    }
                } catch (IOException e) {
                    subscriber.onError(e);
                } finally {
                    OsUtil.closeSilently(out);
                }
            }
        }).compose(RxSchedulers.<Uri>io_main());
    }

    @Override
    public void release() {
        if (mImageReader != null)
            mImageReader.close();
        if (mVirtualDisplay != null)
            mVirtualDisplay.release();
        if (mProjection != null) {
            mProjection.stop();
        }
    }

    private Observable<Boolean> scrollNextScreen(final long duration) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {

            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                Log.i(TAG, "scrollNextScreen... screenHeight = " + getHeight()
                        + ", current thread name = " + Thread.currentThread().getName());
                try {
                    ScrollUtils.scrollToNextScreen(getHeight(), duration);//scroll
                    subscriber.onNext(true);
                    subscriber.onCompleted();
                } catch (IOException e) {
                    e.printStackTrace();
                    subscriber.onError(e);
                }

            }
        }).compose(RxSchedulers.<Boolean>io_main());
    }

    private Bitmap collageLongBitmap(Bitmap oldBitmap, Bitmap newBitmap) {
        if (oldBitmap == null || oldBitmap.isRecycled()) {
            //throw new NullPointerException("bitmap is null");
            return null;
        }
        if (newBitmap == null || newBitmap.isRecycled()) {
            //throw new NullPointerException("bitmap is null");
            return null;
        }

        //collage a new bitmap
        Bitmap collageBitmap = LongScreenshotUtil.getInstance()
                .collageLongBitmap(oldBitmap, newBitmap);

        if (collageBitmap == null || collageBitmap.isRecycled()) {
            //throw new NullPointerException("bitmap is null");
            return null;
        }
        return collageBitmap;
    }

    private Bitmap addBorder(Bitmap bitmap, int width) {
        Bitmap newBitmap = Bitmap.createBitmap(width + bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas = new Canvas(newBitmap);
        canvas.drawColor(Color.TRANSPARENT);
        canvas.drawBitmap(bitmap, 0.0F, 0.0F, null);
        return newBitmap;
    }

    private int getDensityDpi() {
        Display display = ((WindowManager) App.getContext().getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getRealMetrics(displayMetrics);
        return displayMetrics.densityDpi;
    }

    public int getHeight() {
        Display display = ((WindowManager) App.getContext().getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getRealMetrics(displayMetrics);
        return displayMetrics.heightPixels;
    }

    public int getWidth() {
        Display display = ((WindowManager) App.getContext().getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getRealMetrics(displayMetrics);
        return displayMetrics.widthPixels;
    }

    public int getRotation() {
        Display display = ((WindowManager) App.getContext().getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getRealMetrics(displayMetrics);
        return display.getRotation();
    }


}
