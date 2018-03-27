package com.way.capture.utils;

import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import com.way.capture.App;

public final class ViewUtils {

    private ViewUtils() {
    }

    public static float getCenterX(View v) {
        return (v.getLeft() + v.getRight()) / 2f;
    }

    public static float getCenterY(View v) {
        return (v.getTop() + v.getBottom()) / 2f;
    }


    public static boolean isVisible(View v) {
        return (v.getVisibility() == View.VISIBLE);
    }

    public static void setVisible(View v) {
        setVisibility(v, View.VISIBLE);
    }

    public static void setInvisible(View v) {
        setVisibility(v, View.INVISIBLE);
    }

    public static void setGone(View v) {
        setVisibility(v, View.GONE);
    }

    private static void setVisibility(View v, int visibility) {
        if (v.getVisibility() != visibility) {
            v.setVisibility(visibility);
        }
    }

    public static int getDensityDpi() {
        Display display = ((WindowManager) App.getContext().getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getRealMetrics(displayMetrics);
        return displayMetrics.densityDpi;
    }

    public static int getWidth() {
        Display display = ((WindowManager) App.getContext().getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getRealMetrics(displayMetrics);
        return displayMetrics.widthPixels;
    }

    public static int getHeight() {
        Display display = ((WindowManager) App.getContext().getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getRealMetrics(displayMetrics);
        return displayMetrics.heightPixels;
    }


    public static int getRotation() {
        Display display = ((WindowManager) App.getContext().getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getRealMetrics(displayMetrics);
        return display.getRotation();
    }

    public static int getFloatType() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        return WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
    }
}
