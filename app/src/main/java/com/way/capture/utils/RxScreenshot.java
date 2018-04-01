package com.way.capture.utils;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
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
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * Created by way on 2018/3/25.
 */

public class RxScreenshot extends Observable<Bitmap> {
    public static final String DISPLAY_NAME = "Screenshot";
    private static final String TAG = "RxScreenshot";
    private MediaProjection mProjection;

    public RxScreenshot(Application application, int resultCode, Intent data) {
        MediaProjectionManager projectionManager = (MediaProjectionManager) application
                .getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mProjection = projectionManager.getMediaProjection(resultCode, data);
    }

    @Override
    protected void subscribeActual(Observer<? super Bitmap> observer) {
        final int width = ViewUtils.getWidth();
        final int height = ViewUtils.getHeight();
        ImageReader imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1);// 只获取一张图片
        Listener listener = new Listener(mProjection, imageReader, observer);
        observer.onSubscribe(listener);
        imageReader.setOnImageAvailableListener(listener, null);

        VirtualDisplay virtualDisplay = mProjection.createVirtualDisplay(DISPLAY_NAME, width,
                height, ViewUtils.getDensityDpi(), DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION,
                imageReader.getSurface(), null, null);

        Log.d(TAG, "subscribeActual: start screenshot...");
    }


    private static final class Listener implements Disposable, ImageReader.OnImageAvailableListener {
        private final AtomicBoolean unSubscribed = new AtomicBoolean();
        private final MediaProjection mProjection;
        private final ImageReader mImageReader;
        private final Observer<? super Bitmap> observer;

        Listener(MediaProjection projection, ImageReader imageReader, Observer<? super Bitmap> observer) {
            this.mProjection = projection;
            this.mImageReader = imageReader;
            this.observer = observer;
        }

        @Override
        public void dispose() {
            Log.d(TAG, "dispose: screenshot finish...");
            if (unSubscribed.compareAndSet(false, true)) {
                mImageReader.setOnImageAvailableListener(null, null);
                mImageReader.close();
                mProjection.stop();
            }
        }

        @Override
        public boolean isDisposed() {
            return unSubscribed.get();
        }

        @Override
        public void onImageAvailable(ImageReader onImageAvailable) {
            Log.d(TAG, "onImageAvailable: isDisposed = " + isDisposed() + ", onImageAvailable = " + onImageAvailable);
            if (!isDisposed()) {
                Image image = mImageReader.acquireLatestImage();
                if (image == null) {
                    observer.onError(new Exception("image is null..."));
                } else {
                    int imageWidth = ViewUtils.getWidth();
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
                        observer.onError(new Exception("bitmap is null"));
                    } else {
                        observer.onNext(bitmap);
                        observer.onComplete();
                    }
                }
                dispose();
            }
        }

        private Bitmap addBorder(Bitmap bitmap, int width) {
            Bitmap newBitmap = Bitmap.createBitmap(width + bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
            Canvas canvas = new Canvas(newBitmap);
            canvas.drawColor(Color.TRANSPARENT);
            canvas.drawBitmap(bitmap, 0.0F, 0.0F, null);
            return newBitmap;
        }

    }
}
