package com.way.capture.core.screenrecord;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;
import com.way.capture.R;
import com.way.capture.fragment.SettingsFragment;
import com.way.capture.utils.ViewUtils;

import java.util.concurrent.TimeUnit;

import io.reactivex.functions.Consumer;

import static android.graphics.PixelFormat.TRANSLUCENT;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;

final class OverlayView extends FrameLayout {
    private static final String TAG = "OverlayView";
    private static final long COUNTDOWN_DELAY = 3600L;
    private static final int COUNTDOWN_MAX = 3;
    private final Listener mListener;

    private TextView mRecordingTimeTextView;
    private View mSwitchButton;
    private View mCloseButton;
    private long mLastFiredTime;

    private OverlayView(Context context, Listener listener) {
        super(context);
        this.mListener = listener;
        initViews(context);
    }

    static OverlayView create(Context context, Listener listener) {
        return new OverlayView(context, listener);
    }

    static WindowManager.LayoutParams createLayoutParams(Context context) {
        Resources res = context.getResources();
        int width = WindowManager.LayoutParams.WRAP_CONTENT;
        int height = WindowManager.LayoutParams.WRAP_CONTENT;
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(width, height,
                ViewUtils.getFloatType(), FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL | FLAG_LAYOUT_NO_LIMITS
                | FLAG_LAYOUT_INSET_DECOR | FLAG_LAYOUT_IN_SCREEN, TRANSLUCENT);
        params.windowAnimations = R.style.VolumePanelAnimation;
        params.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED;
        params.y = res.getDimensionPixelSize(R.dimen.overlay_height) * 2;
        params.gravity = Gravity.TOP | Gravity.CENTER;
        return params;
    }

    private void initViews(Context context) {
        inflate(context, R.layout.layout_float_view, this);

        mSwitchButton = findViewById(R.id.screen_record_switch);
        mSwitchButton.setSelected(false);
        mCloseButton = findViewById(R.id.screen_record_close);
        mRecordingTimeTextView = findViewById(R.id.start_text);
        RxView.clicks(mSwitchButton)
                .throttleFirst(2, TimeUnit.SECONDS)
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) throws Exception {
                        if (mSwitchButton.isSelected()) {
                            mListener.onStopClick();
                        } else {
                            checkCountDown();
                        }
                        mSwitchButton.setSelected(!mSwitchButton.isSelected());
                    }
                });
        RxView.clicks(mCloseButton)
                .throttleFirst(2, TimeUnit.SECONDS)
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) throws Exception {
                        mListener.onCancelClick();
                    }
                });
    }

    void checkCountDown() {
        boolean isCountDown = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getBoolean(SettingsFragment.SHOW_COUNTDOWN_KEY, true);
        if (!isCountDown) {
            startRecording();
        } else {
            mSwitchButton.setEnabled(false);
            mCloseButton.setEnabled(false);
            ValueAnimator countDownAnimator = ValueAnimator.ofInt(COUNTDOWN_MAX, 0);
            countDownAnimator.setDuration(COUNTDOWN_DELAY);
            countDownAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    final int value = (int) animation.getAnimatedValue();
                    //Log.d(TAG, "onAnimationUpdate: value = " + value);
                    mRecordingTimeTextView.setText("  " + value + "  ");
                }
            });
            countDownAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    startRecording();
                }
            });
            countDownAnimator.start();
        }
    }

    private void startRecording() {
        mRecordingTimeTextView.setText("00:00");
        mCloseButton.setVisibility(View.GONE);
        mCloseButton.setEnabled(true);
        mSwitchButton.setBackgroundResource(R.drawable.stop);
        mSwitchButton.setEnabled(true);
        mListener.onStartClick();
    }

    public void updateRecordingTime(long time) {
        if (SystemClock.elapsedRealtime() - mLastFiredTime < 1000) {
            return;
        }
        mRecordingTimeTextView.setText(DateUtils.formatElapsedTime(time / 1000));
        mLastFiredTime = SystemClock.elapsedRealtime();
    }


    interface Listener {
        /**
         * Called when cancel is clicked. This view is unusable once this
         * callback is invoked.
         */
        void onCancelClick();

        /**
         * Called when start is clicked and it is appropriate to start
         * recording. This view will hide itself completely before invoking this
         * callback.
         */
        void onStartClick();

        /**
         * Called when stop is clicked. This view is unusable once this callback
         * is invoked.
         */
        void onStopClick();
    }
}
