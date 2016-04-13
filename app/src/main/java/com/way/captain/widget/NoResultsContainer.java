package com.way.captain.widget;

import android.content.Context;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.way.captain.R;


/**
 * This class is the default empty state view for most listviews/fragments
 * It allows the ability to set a main text, a main highlight text and a secondary text
 * By default this container has some strings loaded, but other classes can call the apis to change
 * the text
 */
public class NoResultsContainer extends LinearLayout {
    public NoResultsContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * This changes the Main text (top-most text) of the empty container
     *
     * @param resId String resource id
     */
    public void setMainText(@StringRes final int resId) {
        ((TextView) findViewById(R.id.no_results_main_text)).setText(resId);
    }

    public void setMainHighlightText(@StringRes final String text) {
        final TextView hightlightText = (TextView) findViewById(R.id.no_results_main_highlight_text);

        if (text == null || text.isEmpty()) {
            hightlightText.setVisibility(View.GONE);
        } else {
            hightlightText.setText(text);
            hightlightText.setVisibility(View.VISIBLE);
        }
    }

    public void setSecondaryText(@StringRes final int resId) {
        ((TextView) findViewById(R.id.no_results_secondary_text)).setText(resId);
    }

    public void setMainLogo(@DrawableRes final int resId) {
        ((ImageView) findViewById(R.id.no_results_logo)).setImageResource(resId);
        findViewById(R.id.no_results_logo).setVisibility(View.VISIBLE);
    }

    public void setTextColor(@ColorInt int color) {
        ((TextView) findViewById(R.id.no_results_main_text)).setTextColor(color);
        ((TextView) findViewById(R.id.no_results_main_highlight_text)).setTextColor(color);
        ((TextView) findViewById(R.id.no_results_secondary_text)).setTextColor(color);
    }
}