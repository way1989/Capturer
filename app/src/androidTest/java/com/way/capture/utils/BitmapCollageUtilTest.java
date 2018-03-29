package com.way.capture.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.test.InstrumentationRegistry;
import android.util.Log;


import com.way.capture.R;

import org.junit.Test;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertNotNull;

/**
 * Created by android on 18-3-26.
 */
public class BitmapCollageUtilTest {
    private static final String TAG = "BitmapCollageUtilTest";

    public static void closeSilently(Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Throwable t) {
            // do nothing
        }
    }

    private static void saveData(Bitmap bitmap, File path) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
        } catch (IOException e) {
            Log.e(TAG, "Failed to write image", e);
        } finally {
            closeSilently(out);
        }
    }

    @Test
    public void collageLongBitmap() throws Exception {
        Log.i(TAG, "collageLongBitmap: start test....");
        // Context of the app under test.
        final Context appContext = InstrumentationRegistry.getTargetContext();

        Bitmap oldBitmap = BitmapFactory.decodeStream(appContext.getResources().openRawResource(R.raw.first_1080));
        Bitmap newBitmap = BitmapFactory.decodeStream(appContext.getResources().openRawResource(R.raw.second_1080));
        Bitmap firstBitmap = BitmapCollageUtil.getInstance().collageLongBitmap(oldBitmap, newBitmap);

        Bitmap oldBitmapNo = BitmapFactory.decodeStream(appContext.getResources().openRawResource(R.raw.first_1080_no));
        Bitmap newBitmapNo = BitmapFactory.decodeStream(appContext.getResources().openRawResource(R.raw.second_1080_no));
        Bitmap bitmap = BitmapCollageUtil.getInstance().collageLongBitmap(firstBitmap, oldBitmapNo);

        Log.d(TAG, "collageLongBitmap test collage bitmap onNext: bitmap = " + bitmap.getWidth() + "x" + bitmap.getHeight());
        if (bitmap != null)
        saveData(bitmap, new File(appContext.getExternalCacheDir(), "1.png"));
        assertNotNull(bitmap);
    }

    @Test
    public void testColorDifferent () throws Exception {
        int a = Color.parseColor("#000000");
        int b = Color.parseColor("#ffffff");

        final int oldRed = Color.red(a);
        final int oldGreen = Color.green(a);
        final int oldBlue = Color.blue(a);

        final int newRed = Color.red(b);
        final int newGreen = Color.green(b);
        final int newBlue = Color.blue(b);

        final int redDifferent = Math.abs(oldRed - newRed);
        final int greenDifferent = Math.abs(oldGreen - newGreen);
        final int blueDifferent = Math.abs(oldBlue - newBlue);

        final double dif = Math.sqrt(redDifferent * redDifferent + greenDifferent * greenDifferent + blueDifferent * blueDifferent);
        Log.e(TAG, "testColorDifferent: dif = " + dif);
    }
}