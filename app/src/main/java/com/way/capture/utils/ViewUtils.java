package com.way.capture.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import com.way.capture.App;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

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

    public static boolean isNavigationBarShow() {
        Display display = ((WindowManager) App.getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        Point realSize = new Point();
        display.getSize(size);
        display.getRealSize(realSize);
        Configuration configuration = App.getContext().getResources().getConfiguration();
        final boolean isLandscape = configuration.orientation == ORIENTATION_LANDSCAPE;
        return isLandscape ? realSize.x != size.x : realSize.y != size.y;
    }

    public static int getNavigationBarHeight() {
        if (!isNavigationBarShow()) {
            return 0;
        }
        Resources resources = App.getContext().getResources();
        Configuration configuration = resources.getConfiguration();
        final boolean isLandscape = configuration.orientation == ORIENTATION_LANDSCAPE;
        int resourceId = resources.getIdentifier(isLandscape ? "navigation_bar_width"
                : "navigation_bar_height", "dimen", "android");
        //获取NavigationBar的高度
        return resources.getDimensionPixelSize(resourceId);
    }

    public static int getStatusBarHeight() {
        final Resources resources = App.getContext().getResources();
        final int statusBarHeightId = resources.getIdentifier("status_bar_height", "dimen", "android");
        return resources.getDimensionPixelSize(statusBarHeightId);
    }

    public static int dp2px(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, App.getContext().getResources().getDisplayMetrics());
    }

    public static float px2dp(float px) {
        return px / App.getContext().getResources().getDisplayMetrics().density;
    }

    public static float sp2px(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, App.getContext().getResources().getDisplayMetrics());
    }

    public static float px2sp(float px) {
        return px / App.getContext().getResources().getDisplayMetrics().scaledDensity;
    }

    public static boolean isLandscape(Context context) {
        return context.getResources().getConfiguration()
                .orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    public static void setStatusBarStyle(@NonNull Activity activity, boolean onlyDarkStatusBar) {
        int flags = activity.getWindow().getDecorView().getSystemUiVisibility();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (onlyDarkStatusBar || PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(App.KEY_NIGHT_MODE, false)) {
                flags ^= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
        }
        activity.getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    public static void setNavigationBarStyle(@NonNull Activity activity,
                                             boolean onlyDarkNavigationBar, boolean translucent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isLandscape(activity)) {
            int flags = activity.getWindow().getDecorView().getSystemUiVisibility();
            if (translucent) {
                flags |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            }
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            if (!onlyDarkNavigationBar && PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(App.KEY_NIGHT_MODE, false)) {
                if (translucent) {
                    activity.getWindow().setNavigationBarColor(
                            Color.argb((int) (0.03 * 255), 0, 0, 0));
                } else {
                    activity.getWindow().setNavigationBarColor(Color.rgb(241, 241, 241));
                }
            } else {
                flags ^= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                if (translucent) {
                    activity.getWindow().setNavigationBarColor(
                            Color.argb((int) (0.2 * 255), 0, 0, 0));
                } else {
                    activity.getWindow().setNavigationBarColor(Color.rgb(26, 26, 26));
                }
            }
            activity.getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    public static int getFloatType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        return WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
    }
}
