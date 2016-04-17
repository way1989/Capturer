package com.way.captain.screenrecord;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.way.captain.R;
import com.way.captain.fragment.SettingsFragment;
import com.way.captain.utils.AppUtils;
import com.way.captain.widget.timely.TimelyView;

import static android.graphics.PixelFormat.TRANSLUCENT;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
import static android.view.WindowManager.LayoutParams.TYPE_TOAST;

final class OverlayView extends FrameLayout implements View.OnClickListener {
    private static final String TAG = "OverlayView";
    private static final int COUNTDOWN_DELAY = 1200;
    private static final int COUNTDOWN_MAX = 3;
    private final Listener mListener;
    private View mStartContainer;
    private TextView mQualityTextView;
    private View mStopContainer;
    private View mCountDownContainer;
    private TimelyView mCountDownTextView;

    private TextView mRecordingTimeTextView;
    private long mRecordingStartTime;

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
                TYPE_TOAST, FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL | FLAG_LAYOUT_NO_LIMITS
                | FLAG_LAYOUT_INSET_DECOR | FLAG_LAYOUT_IN_SCREEN, TRANSLUCENT);
        params.windowAnimations = R.style.VolumePanelAnimation;
        params.y = res.getDimensionPixelSize(R.dimen.overlay_height) * 2;
        params.gravity = Gravity.TOP | Gravity.CENTER;
        return params;
    }

    private void initViews(Context context) {
        inflate(context, R.layout.float_screen_record_control, this);

        mStartContainer = findViewById(R.id.record_overlay_buttons);
        View cancelButton = findViewById(R.id.record_overlay_cancel);
        View qualityButton = findViewById(R.id.record_change_quality);
        mQualityTextView = (TextView) findViewById(R.id.show_record_quality);
        View startButton = findViewById(R.id.record_overlay_start);
        mStopContainer = findViewById(R.id.recorder_layout);
        mCountDownContainer = findViewById(R.id.record_overlay_recording_root);
        mCountDownTextView = (TimelyView) findViewById(R.id.record_overlay_recording);
        mRecordingTimeTextView = (TextView) findViewById(R.id.recording_time);
        cancelButton.setOnClickListener(this);
        startButton.setOnClickListener(this);
        qualityButton.setOnClickListener(this);
        findViewById(R.id.record_overlay_stop).setOnClickListener(this);

        CheatSheet.setup(cancelButton);
        CheatSheet.setup(startButton);
        CheatSheet.setup(qualityButton);

        int videoSizePercentageProvider = Integer.valueOf(PreferenceManager
                .getDefaultSharedPreferences(getContext()).getString(SettingsFragment.VIDEO_SIZE_KEY, "100"));
        switch (videoSizePercentageProvider) {
            case 100:
                mQualityTextView.setText(R.string.float_record_super_hd_quality);
                break;
            case 75:
                mQualityTextView.setText(R.string.float_record_hd_quality);
                break;
            case 50:
                mQualityTextView.setText(R.string.float_record_normal_quality);
                break;
            default:
                mQualityTextView.setText(R.string.float_record_super_hd_quality);
                break;
        }
    }

    void onStartClicked() {
        boolean isCountDown = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getBoolean(SettingsFragment.SHOW_COUNTDOWN_KEY, true);
        if (!isCountDown) {
            mStartContainer.animate().alpha(0).withEndAction(new Runnable() {
                @Override
                public void run() {
                    startRecording();
                }
            });
        } else {
            mStartContainer.animate().alpha(0);
            mCountDownContainer.setVisibility(VISIBLE);
            mCountDownContainer.animate().alpha(1).withEndAction(new Runnable() {
                @Override
                public void run() {
                    countdown(COUNTDOWN_MAX);
                }
            });
        }
    }

    private void startRecording() {
        mStopContainer.setVisibility(View.VISIBLE);
        mStopContainer.animate().alpha(1);
        mListener.onStart();
        mRecordingStartTime = SystemClock.uptimeMillis();
        updateRecordingTime();
    }

    private void countdown(final int index) {
        if (index == 0) {// countdown to end
            countdownComplete();
            return;
        }
        ObjectAnimator countDownAnimator = mCountDownTextView.animate(index, index - 1);
        countDownAnimator.setDuration(COUNTDOWN_DELAY);
        countDownAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                countdown(index - 1);
            }
        });
        countDownAnimator.start();
    }

    private void countdownComplete() {
        mCountDownContainer.animate().alpha(0).withEndAction(new Runnable() {
            @Override
            public void run() {
                mCountDownContainer.setVisibility(View.GONE);
                startRecording();
            }
        });
    }

    private void updateRecordingTime() {

        long now = SystemClock.uptimeMillis();
        long delta = now - mRecordingStartTime;

        String text = AppUtils.getVideoRecordTime(delta, false);
        final long targetNextUpdateDelay = 1000;
        mRecordingTimeTextView.setText(text);

        long actualNextUpdateDelay = targetNextUpdateDelay - (delta % targetNextUpdateDelay);
        postDelayed(new Runnable() {
            @Override
            public void run() {
                updateRecordingTime();
            }
        }, actualNextUpdateDelay);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.record_overlay_cancel:
                mListener.onCancel();
                break;
            case R.id.record_overlay_start:
                onStartClicked();
                break;
            case R.id.record_overlay_stop:
                mListener.onStop();
                break;
            case R.id.record_change_quality:
                changVideoQuality();
                break;
            default:
                break;
        }
    }

    private void changVideoQuality() {
        int videoSizePercentageProvider = Integer.valueOf(PreferenceManager
                .getDefaultSharedPreferences(getContext()).getString(SettingsFragment.VIDEO_SIZE_KEY, "100"));
        switch (videoSizePercentageProvider) {
            case 100:
                PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                        .putString(SettingsFragment.VIDEO_SIZE_KEY, "50").apply();
                mQualityTextView.setText(R.string.float_record_normal_quality);
                break;
            case 75:
                PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                        .putString(SettingsFragment.VIDEO_SIZE_KEY, "100").apply();
                mQualityTextView.setText(R.string.float_record_super_hd_quality);
                break;
            case 50:
                PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                        .putString(SettingsFragment.VIDEO_SIZE_KEY, "75").apply();
                mQualityTextView.setText(R.string.float_record_hd_quality);
                break;
            default:
                PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                        .putString(SettingsFragment.VIDEO_SIZE_KEY, "100").apply();
                mQualityTextView.setText(R.string.float_record_super_hd_quality);
                break;
        }
    }

    interface Listener {
        /**
         * Called when cancel is clicked. This view is unusable once this
         * callback is invoked.
         */
        void onCancel();

        /**
         * Called when start is clicked and it is appropriate to start
         * recording. This view will hide itself completely before invoking this
         * callback.
         */
        void onStart();

        /**
         * Called when stop is clicked. This view is unusable once this callback
         * is invoked.
         */
        void onStop();
    }
}
