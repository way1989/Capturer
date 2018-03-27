package com.way.capture.core.screenshot;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.way.capture.App;
import com.way.capture.R;
import com.way.capture.core.DeleteScreenshot;
import com.way.capture.utils.BitmapCollageUtil;
import com.way.capture.utils.OsUtil;
import com.way.capture.utils.RxScreenshot;
import com.way.capture.utils.ScrollUtils;
import com.way.capture.utils.ViewUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;


/**
 * Created by android on 16-11-23.
 */

public class ScreenshotModel implements ScreenshotContract.Model {
    private static final String TAG = "ScreenshotModel";
    //    private static final String DISPLAY_NAME = "Screenshot";
    private static final long SCROLL_DURATION = 1000L;
    private static final long WAIT_FOR_SCROLL_TIME = 2000L;
    private static final String SCREENSHOTS_DIR_NAME = "Screenshots";
    private static final String SCREENSHOT_FILE_NAME_TEMPLATE = "Screenshot_%s.png";
    private static final String SCREENSHOT_SHARE_SUBJECT_TEMPLATE = "Screenshot (%s)";
    //    private MediaProjection mProjection;
//    private VirtualDisplay mVirtualDisplay;
//    private ImageReader mImageReader;
    private int mLastRotation;
    private int mResultCode;
    private Intent mIntent;

    public ScreenshotModel(int resultCode, Intent data) {
        mResultCode = resultCode;
        mIntent = data;
//        MediaProjectionManager projectionManager = (MediaProjectionManager) App.getContext()
//                .getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        //mProjection = projectionManager.getMediaProjection(resultCode, data);
    }

    @Override
    public Observable<Bitmap> getNewBitmap() {
        Log.d(TAG, "getNewBitmap...");
        mLastRotation = ViewUtils.getRotation();
        return new RxScreenshot(App.getContext(), mResultCode, mIntent)
                .subscribeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Observable<Bitmap> takeLongScreenshot(final Bitmap oldBitmap, boolean isAutoScroll) {
        int rotation = ViewUtils.getRotation();
        if (rotation != mLastRotation) {
            return Observable.error(new Throwable("device rotation is change..."));
        }
        Observable<Boolean> observable;
        if (isAutoScroll) {
            observable = scrollNextScreen(SCROLL_DURATION).delay(WAIT_FOR_SCROLL_TIME, TimeUnit.MILLISECONDS);
        } else {
            observable = Observable.just(true);
        }
        return observable.flatMap(new Function<Boolean, ObservableSource<Bitmap>>() {
            @Override
            public ObservableSource<Bitmap> apply(Boolean aBoolean) throws Exception {
                return getNewBitmap();
            }
        }).map(new Function<Bitmap, Bitmap>() {
            @Override
            public Bitmap apply(Bitmap bitmap) throws Exception {
                return collageLongBitmap(oldBitmap, bitmap);
            }
        });
    }

    @Override
    public Observable<Uri> saveScreenshot(final Bitmap bitmap, final Notification.Builder notificationBuilder) {
        return Observable.create(new ObservableOnSubscribe<Uri>() {
            @Override
            public void subscribe(ObservableEmitter<Uri> e) throws Exception {
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
                        e.onNext(uri);
                        e.onComplete();
                    } else {
                        e.onError(new Throwable("save uri is null..."));
                    }
                } finally {
                    OsUtil.closeSilently(out);
                }
            }
        });
    }

    @Override
    public void release() {
//        if (mImageReader != null)
//            mImageReader.close();
//        if (mVirtualDisplay != null)
//            mVirtualDisplay.release();
//        if (mProjection != null) {
//            mProjection.stop();
//        }
    }

    private Observable<Boolean> scrollNextScreen(final long duration) {
        return Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> e) throws Exception {
                final int height = ViewUtils.getHeight();
                Log.i(TAG, "scrollNextScreen... screenHeight = " + height
                        + ", current thread name = " + Thread.currentThread().getName());
                ScrollUtils.scrollToNextScreen(height, duration);//scroll
                e.onNext(true);
                e.onComplete();
            }
        });
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
        Bitmap collageBitmap = BitmapCollageUtil.getInstance()
                .collageLongBitmap(oldBitmap, newBitmap);

        if (collageBitmap == null || collageBitmap.isRecycled()) {
            //throw new NullPointerException("bitmap is null");
            return null;
        }
        return collageBitmap;
    }

}
