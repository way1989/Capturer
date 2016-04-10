package com.way.captain.utils;

import android.app.Activity;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Pair;
import android.view.View;

import com.way.captain.R;
import com.way.captain.fragment.DetailFragment;

import java.io.File;

public class AppUtils {

    public static final String APP_ABSOLUTE_ROOT_PATH = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_MOVIES).getAbsolutePath();
    public static final String APP_SCREENSHOT_ABSOLUTE_ROOT_PATH = Environment
            .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
    //screenshot
    public static final String SCREENSHOT_FOLDER_NAME = "Screenshots";
    public static final String SCREENSHOT_FOLDER_PATH = new File(APP_SCREENSHOT_ABSOLUTE_ROOT_PATH,
            SCREENSHOT_FOLDER_NAME).getAbsolutePath();
    // gif 作品库
    public static final String GIF_PRODUCTS_FOLDER_NAME = "Gifs";
    public static final String GIF_PRODUCTS_FOLDER_PATH = new File(APP_ABSOLUTE_ROOT_PATH,
            GIF_PRODUCTS_FOLDER_NAME).getAbsolutePath();

    // videos folder, maybe not be needed
    public static final String VIDEOS_FOLDER_NAME = "ScreenRecord";
    public static final String VIDEOS_FOLDER_PATH = new File(APP_ABSOLUTE_ROOT_PATH,
            VIDEOS_FOLDER_NAME).getAbsolutePath();

    public static boolean isMarshmallow() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    public static boolean isLollipop() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }
}
