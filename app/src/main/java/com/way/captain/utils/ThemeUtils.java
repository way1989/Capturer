package com.way.captain.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.view.Window;

/**
 * Created by way on 16/4/10.
 */
public class ThemeUtils {
    public static void setStatusBarColor(Activity activity, @ColorInt int color) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                final Window window = activity.getWindow();
                window.setStatusBarColor(getStatusBarColor(color));
                applyTaskDescription(activity, color);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                final View decorView = activity.getWindow().getDecorView();
                boolean lightStatusEnabled = isColorLight(color);

                final int systemUiVisibility = decorView.getSystemUiVisibility();
                if (lightStatusEnabled) {
                    decorView.setSystemUiVisibility(systemUiVisibility | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                } else {
                    decorView.setSystemUiVisibility(systemUiVisibility & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isColorLight(@ColorInt int color) {
        double darkness = 1.0D - (0.299D * (double) Color.red(color) + 0.587D * (double) Color.green(color) + 0.114D * (double) Color.blue(color)) / 255.0D;
        return darkness < 0.4D;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static void applyTaskDescription(@NonNull Activity activity, int color) {
        // Sets color of entry in the system recents page
        try {
            ActivityManager.TaskDescription td = new ActivityManager.TaskDescription(
                    (String) activity.getTitle(),
                    ((BitmapDrawable) activity.getApplicationInfo().loadIcon(activity.getPackageManager())).getBitmap(),
                    color);
            activity.setTaskDescription(td);
        } catch (Exception ignored) {

        }
    }

    public static int getStatusBarColor(int primaryColor) {
        float[] arrayOfFloat = new float[3];
        Color.colorToHSV(primaryColor, arrayOfFloat);
        arrayOfFloat[2] *= 0.9F;
        return Color.HSVToColor(arrayOfFloat);
    }

    public static void setFabBackgroundTint(FloatingActionButton fab, int color) {
        ColorStateList fabColorStateList = new ColorStateList(
                new int[][]{
                        new int[]{}
                },
                new int[]{
                        color,
                }
        );
        fab.setBackgroundTintList(fabColorStateList);
    }
}
