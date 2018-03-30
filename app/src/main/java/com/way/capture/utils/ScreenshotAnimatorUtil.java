package com.way.capture.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.PointF;
import android.view.View;
import android.view.animation.Interpolator;

public class ScreenshotAnimatorUtil {
    //anim
    private static final int SCREENSHOT_FLASH_TO_PEAK_DURATION = 130;
    private static final int SCREENSHOT_DROP_IN_DURATION = 430;
    private static final int SCREENSHOT_DROP_OUT_DELAY = 500;
    private static final int SCREENSHOT_DROP_OUT_DURATION = 430;
    private static final int SCREENSHOT_DROP_OUT_SCALE_DURATION = 370;
    private static final float SCREENSHOT_SCALE = 1f;
    private static final float SCREENSHOT_DROP_IN_MIN_SCALE = SCREENSHOT_SCALE * 0.689f;

    public static ValueAnimator createScreenshotDropInAnimation(final View screenshotView, final View flashView) {
        final float flashPeakDurationPct = ((float) (SCREENSHOT_FLASH_TO_PEAK_DURATION) / SCREENSHOT_DROP_IN_DURATION);
        final float flashDurationPct = 2f * flashPeakDurationPct;
        final Interpolator flashAlphaInterpolator = new Interpolator() {
            @Override
            public float getInterpolation(float x) {
                // Flash the flash view in and out quickly
                if (x <= flashDurationPct) {
                    return (float) Math.sin(Math.PI * (x / flashDurationPct));
                }
                return 0;
            }
        };
        final Interpolator scaleInterpolator = new Interpolator() {
            @Override
            public float getInterpolation(float x) {
                // We start scaling when the flash is at it's peak
                if (x < flashPeakDurationPct) {
                    return 0;
                }
                return (x - flashDurationPct) / (1f - flashDurationPct);
            }
        };
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(SCREENSHOT_DROP_IN_DURATION);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                screenshotView.setAlpha(0f);
                screenshotView.setTranslationX(0f);
                screenshotView.setTranslationY(0f);
                screenshotView.setScaleX(SCREENSHOT_SCALE);
                screenshotView.setScaleY(SCREENSHOT_SCALE);
                screenshotView.setVisibility(View.VISIBLE);
                flashView.setAlpha(0f);
                flashView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                flashView.setVisibility(View.GONE);
            }
        });
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float t = (Float) animation.getAnimatedValue();
                float scaleT = (SCREENSHOT_SCALE)
                        - scaleInterpolator.getInterpolation(t) * (SCREENSHOT_SCALE - SCREENSHOT_DROP_IN_MIN_SCALE);
//                mBackgroundView.setAlpha(scaleInterpolator.getInterpolation(t) * BACKGROUND_ALPHA);
                screenshotView.setAlpha(t);
                screenshotView.setScaleX(scaleT);
                screenshotView.setScaleY(scaleT);
                flashView.setAlpha(flashAlphaInterpolator.getInterpolation(t));
            }
        });
        return anim;
    }

    public static ValueAnimator createScreenshotDropOutAnimation(final View screenshotView) {
        int w = ViewUtils.getWidth();
        int h = ViewUtils.getHeight();
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setStartDelay(SCREENSHOT_DROP_OUT_DELAY);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                screenshotView.setVisibility(View.GONE);
                screenshotView.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        });

        // In the case where there is a status bar, animate to the origin of
        // the bar (top-left)
        final float scaleDurationPct = (float) SCREENSHOT_DROP_OUT_SCALE_DURATION / SCREENSHOT_DROP_OUT_DURATION;
        final Interpolator scaleInterpolator = new Interpolator() {
            @Override
            public float getInterpolation(float x) {
                if (x < scaleDurationPct) {
                    // Decelerate, and scale the input accordingly
                    return (float) (1f - Math.pow(1f - (x / scaleDurationPct), 2f));
                }
                return 1f;
            }
        };

        // Determine the bounds of how to scale
        float halfScreenWidth = w / 2f;
        float halfScreenHeight = h;
        final PointF finalPos = new PointF(-halfScreenWidth, halfScreenHeight);

        // Animate the screenshot to the status bar
        anim.setDuration(SCREENSHOT_DROP_OUT_DURATION);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float t = (Float) animation.getAnimatedValue();
                screenshotView.setAlpha(1f - scaleInterpolator.getInterpolation(t));
                screenshotView.setTranslationY(t * finalPos.y);
            }
        });
        return anim;
    }
}
