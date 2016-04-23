package com.way.capture.utils;

import android.view.View;

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
}
