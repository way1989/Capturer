package com.way.capture.core.screenshot;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.way.capture.R;
import com.way.capture.core.BaseModule;
import com.way.capture.fragment.SettingsFragment;
import com.way.capture.service.ShakeService;
import com.way.capture.utils.RxScreenshot;
import com.way.capture.utils.ScreenshotAnimatorUtil;
import com.way.capture.utils.ScrollUtils;
import com.way.capture.utils.ViewUtils;
import com.way.capture.widget.freecrop.FreeCropView;
import com.way.capture.widget.rectcrop.CropImageView;
import com.way.capture.widget.swipe.SwipeHelper;
import com.way.capture.widget.swipe.SwipeVerticalLayout;

/**
 * Created by android on 16-8-22.
 */
public class ScreenshotModule implements BaseModule, ScreenshotContract.View, SwipeVerticalLayout.Callback,
        SwipeHelper.PressListener, View.OnClickListener {
    public static final int SCREENSHOT_NOTIFICATION_ID = 789;
    private static final String TAG = "ScreenshotModule";
    private static final long PREVIEW_OUT_TIME = 4000L;// 超时自动保存
    private static final int AUTO_SAVE_MESSAGE = 0x123;

    private static final boolean IS_DEVICE_ROOT = ScrollUtils.isDeviceRoot();
    //system manager
    private NotificationManager mNotificationManager;
    private KeyguardManager mKeyguardManager;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowLayoutParams;
    private LayoutInflater mLayoutInflater;
    //view
    private FrameLayout mRootView;
    private SwipeVerticalLayout mSwipeVerticalLayout;
    private Button mLongScreenshotBtn;

    private ImageView mScreenshotView;
    private ImageView mScreenshotFlash;
    private TextView mLongScreenshotToast;
    private ViewGroup mLongScreenshotCover;
    private ValueAnimator mScreenshotAnimation;
    private ValueAnimator mExitScreenshotAnimation;
    private Context mContext;
    private String mAction;
    private ScreenshotContract.Presenter mPresenter;
    private boolean mIsAutoLongScreenshot;
    private ObjectAnimator mFloatAnim;
    private Dialog mRectCropDialog;
    private Dialog mFreeCropDialog;
    private boolean mIsRunning;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AUTO_SAVE_MESSAGE:
                    mSwipeVerticalLayout.setEnabled(false);
                    if (!mKeyguardManager.isKeyguardLocked() || !mKeyguardManager.isKeyguardSecure()) {
                        mLongScreenshotBtn.animate().alpha(0);
                    }
                    playScreenshotDropOutAnimation();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public boolean isRunning() {
        return mIsRunning;
    }

    @Override
    public void onStart(Context context, String action, int resultCode, Intent data) {
        mContext = context;
        mAction = action;
        //init system manager
        initSystemManager(context);

        //init views
        initViews();
        mIsAutoLongScreenshot = IS_DEVICE_ROOT && PreferenceManager.getDefaultSharedPreferences(mContext)
                .getBoolean(SettingsFragment.LONG_SCREENSHOT_AUTO, true);
        //init presenter
        mPresenter = new ScreenshotPresenter(this, resultCode, data);

        //cancel the notification
        mNotificationManager.cancel(SCREENSHOT_NOTIFICATION_ID);
        Log.i(TAG, "takeScreenshot...");
        mPresenter.takeScreenshot();
        mIsRunning = true;
    }

    private void initSystemManager(Context context) {
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mKeyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mWindowLayoutParams = new WindowManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 0, ViewUtils.getFloatType(),
                WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                PixelFormat.TRANSLUCENT);
        mWindowLayoutParams.setTitle("Screenshot");
        mWindowLayoutParams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED;//lock current screen orientation
    }

    private void initViews() {
        mRootView = (FrameLayout) mLayoutInflater.inflate(R.layout.global_screenshot, null);
        mSwipeVerticalLayout = mRootView.findViewById(R.id.swipe_layout);
        mSwipeVerticalLayout.setEnabled(false);
        mSwipeVerticalLayout.setCallback(this);
        mSwipeVerticalLayout.setPressListener(this);
        mLongScreenshotBtn = mRootView.findViewById(R.id.scroll_screenshot_btn);
        mLongScreenshotBtn.setOnClickListener(this);

        mScreenshotView = mRootView.findViewById(R.id.global_screenshot);
        mScreenshotFlash = mRootView.findViewById(R.id.global_screenshot_flash);
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        clearFloatAnim();
        removeScreenshotView();
        dismissLongScreenshotToast();
        mPresenter.release();
        mIsRunning = false;
    }

    @Override
    public void showScreenshotAnim(Bitmap bitmap, final boolean longScreenshot, final boolean needCheckAction) {
        if (needCheckAction) {
            switch (mAction) {
                case ShakeService.Action.ACTION_FREE_CROP:
                    showFreeCropLayout(bitmap);
                    return;
                case ShakeService.Action.ACTION_RECT_CROP:
                    showRectCropLayout(bitmap);
                    return;
            }
        }
        // Add the view for the animation
        final int heightPixels = ViewUtils.getHeight();
        final float scale = Math.min(0.8f, (float) heightPixels / bitmap.getHeight());
        Log.i(TAG, "bitmap.getHeight() = " + bitmap.getHeight()
                + ", mDisplayMetrics.heightPixels = " + heightPixels
                + ", scale = " + scale);
        final Bitmap resizeBitmap = ViewUtils.resizeBitmap(bitmap, scale);

        // Optimizations
        resizeBitmap.setHasAlpha(false);
        resizeBitmap.prepareToDraw();
        mScreenshotView.setImageBitmap(resizeBitmap);

        mRootView.requestFocus();

        // Setup the animation with the screenshot just taken
        if (mScreenshotAnimation != null) {
            mScreenshotAnimation.end();
            mScreenshotAnimation.removeAllListeners();
        }

        mWindowManager.addView(mRootView, mWindowLayoutParams);
        mScreenshotAnimation = ScreenshotAnimatorUtil.createScreenshotDropInAnimation(mScreenshotView, mScreenshotFlash);

        mScreenshotAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                onCaptureFinish(longScreenshot, needCheckAction);
            }
        });
        try {
            mRootView.post(new Runnable() {
                @Override
                public void run() {
                    // Play the shutter sound to notify that we've taken a screenshot
                    mPresenter.playCaptureSound();
                    mScreenshotView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                    mScreenshotView.buildLayer();
                    mScreenshotAnimation.start();
                }
            });
        } catch (Exception e) {
            showScreenshotError(e);
        }
    }

    private void onCaptureFinish(boolean longScreenshot, boolean needCheckAction) {
        Log.i(TAG, "onCaptureFinish... longScreenshot = " + longScreenshot);

        if (longScreenshot) {
            dismissLongScreenshotToast();
        }

        if (needCheckAction && !longScreenshot && mContext.getResources().getConfiguration().orientation
                != Configuration.ORIENTATION_LANDSCAPE && !mKeyguardManager.isKeyguardLocked()) {
            mLongScreenshotBtn.setVisibility(View.VISIBLE);
            mLongScreenshotBtn.animate().alpha(1f);
        } else {
            mLongScreenshotBtn.setVisibility(View.GONE);
            mLongScreenshotBtn.setAlpha(1f);
        }
        startFloatAnim();
        mSwipeVerticalLayout.setEnabled(true);


        mHandler.removeMessages(AUTO_SAVE_MESSAGE);
        // three seconds to save and exit
        mHandler.sendEmptyMessageDelayed(AUTO_SAVE_MESSAGE, PREVIEW_OUT_TIME);
    }

    private void startFloatAnim() {
        if (mFloatAnim == null) {
            mFloatAnim = ObjectAnimator.ofFloat(mScreenshotView, "translationY", 0f, ViewUtils.dp2px(8), 0f);
            mFloatAnim.setDuration(2000L);
            mFloatAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            mFloatAnim.setRepeatCount(ValueAnimator.INFINITE);
            mFloatAnim.setRepeatMode(ValueAnimator.REVERSE);
        }
        mFloatAnim.start();
    }

    private void clearFloatAnim() {
        if (mFloatAnim != null && mFloatAnim.isRunning())
            mFloatAnim.cancel();
    }

    @Override
    public void showScreenshotError(Throwable e) {
        Toast.makeText(mContext, R.string.screenshot_failed_title, Toast.LENGTH_SHORT).show();
        notifyScreenshotError(mContext, mNotificationManager);
    }

    @Override
    public void onCollageFinish() {
        takeLongScreenshot();
    }

    @Override
    public void notify(Notification notification) {
        mNotificationManager.notify(SCREENSHOT_NOTIFICATION_ID, notification);
    }

    @Override
    public void finish() {

    }

    private void playAlphaAnim(boolean isDismiss) {
        mHandler.removeMessages(AUTO_SAVE_MESSAGE);
        if (!isDismiss)
            mHandler.sendEmptyMessageDelayed(AUTO_SAVE_MESSAGE, PREVIEW_OUT_TIME);
        if (isDismiss) {
            if (!mKeyguardManager.isKeyguardLocked() || !mKeyguardManager.isKeyguardSecure()) {
                mLongScreenshotBtn.animate().alpha(0);
            }
            clearFloatAnim();
        } else {
            if (!mKeyguardManager.isKeyguardLocked() || !mKeyguardManager.isKeyguardSecure()) {
                mLongScreenshotBtn.animate().alpha(1);
            }
            startFloatAnim();
        }
    }

    @Override
    public void onChildDismissed(View v, int direction) {
        mSwipeVerticalLayout.setEnabled(false);
        mHandler.removeMessages(AUTO_SAVE_MESSAGE);
        removeScreenshotView();
        switch (direction) {
            case SwipeHelper.SWIPE_TO_TOP:
                Log.i(TAG, TAG + " onChildDismissed... SWIPE_TO_TOP");
                break;
            case SwipeHelper.SWIPE_TO_BOTTOM:
                Log.i(TAG, TAG + " onChildDismissed... SWIPE_TO_BOTTOM");
                mPresenter.saveScreenshot();
                break;
            default:
                break;
        }
        mIsRunning = false;
    }

    @Override
    public void onBeginDrag(View v) {
        playAlphaAnim(true);
    }

    @Override
    public void onDragCancelled(View v) {
        playAlphaAnim(false);
    }

    @Override
    public void onChildSnappedBack(View animView) {

    }

    @Override
    public boolean updateSwipeProgress(View animView, boolean dismissable, float swipeProgress) {
        return false;
    }

    @Override
    public void onClick(View v) {
        mHandler.removeMessages(AUTO_SAVE_MESSAGE);
        mSwipeVerticalLayout.setEnabled(false);
        switch (v.getId()) {
            case R.id.scroll_screenshot_btn:
                mLongScreenshotBtn.setVisibility(View.GONE);
                addCover();
                addToast();
                removeScreenshotView();
                takeLongScreenshot();
                break;
            case R.id.long_screenshot_title:
                Log.i(TAG, "onClick... toast_dialog_bg_container");
                updateLongScreenshotView(false);
                mPresenter.takeLongScreenshot(mIsAutoLongScreenshot);
                break;
            case R.id.long_screenshot_indicator_root:
                stopLongScreenshot();
                break;
            default:
                break;
        }
    }

    private void takeLongScreenshot() {
        if (mIsAutoLongScreenshot) {
            mPresenter.takeLongScreenshot(true);
        } else {
            updateLongScreenshotView(true);
        }
    }

    private void stopLongScreenshot() {
        mPresenter.stopLongScreenshot();
    }

    @Override
    public boolean onPress(boolean isDown) {
        playAlphaAnim(isDown);
        return true;
    }

    private void updateLongScreenshotView(boolean isShowToast) {
        mLongScreenshotToast.setText(isShowToast ? R.string.long_screenshot_indicator : R.string.long_screenshot_progressing);
        mLongScreenshotCover.setVisibility(isShowToast ? View.VISIBLE : View.GONE);
    }

    private void dismissLongScreenshotToast() {
        if (mLongScreenshotCover != null) {
            mWindowManager.removeViewImmediate(mLongScreenshotCover);
            mLongScreenshotCover = null;
        }
        if (mLongScreenshotToast != null) {
            mWindowManager.removeViewImmediate(mLongScreenshotToast);
            mLongScreenshotToast = null;
        }
    }

    private void addCover() {
        mLongScreenshotCover = (ViewGroup) mLayoutInflater.inflate(R.layout.long_screenshot_indicator, null);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 0,
                ViewUtils.getFloatType(),
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                PixelFormat.TRANSLUCENT);
        if (!mIsAutoLongScreenshot) {
            layoutParams.flags = WindowManager.LayoutParams.FLAG_FULLSCREEN
                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
        } else {
            int screenShowWidth = ViewUtils.getWidth();
            layoutParams.width = screenShowWidth - 24;//leave some space for swipe
            mLongScreenshotCover.setBackgroundColor(Color.TRANSPARENT);
            mLongScreenshotCover.setOnClickListener(this);
            mLongScreenshotCover.removeAllViews();
        }
        layoutParams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED;
        mWindowManager.addView(mLongScreenshotCover, layoutParams);
    }

    private void addToast() {
        final int statusBarHeight = ViewUtils.getStatusBarHeight();
        mLongScreenshotToast = (TextView) mLayoutInflater.inflate(R.layout.long_screenshot_toast, null);
        mLongScreenshotToast.setPadding(ViewUtils.dp2px(8), ViewUtils.getStatusBarHeight(), ViewUtils.dp2px(8), 0);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 0, ViewUtils.getFloatType(),
                WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                PixelFormat.TRANSLUCENT);
        if (!mIsAutoLongScreenshot) {
            layoutParams.flags = WindowManager.LayoutParams.FLAG_FULLSCREEN
                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
            mLongScreenshotToast.setOnClickListener(this);
        }
        layoutParams.y = 0;
        layoutParams.windowAnimations = R.style.VolumePanelAnimation;
        layoutParams.gravity = mContext.getResources().getInteger(
                R.integer.standard_notification_panel_layout_gravity);
        layoutParams.width = ViewUtils.getWidth();
        layoutParams.height = ViewUtils.dp2px(56) + statusBarHeight;
        layoutParams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED;
        mWindowManager.addView(mLongScreenshotToast, layoutParams);
    }

    private void removeScreenshotView() {
        try {
            mWindowManager.removeViewImmediate(mRootView);
        } catch (Exception e) {
        }
    }

    private void playScreenshotDropOutAnimation() {
        if (mExitScreenshotAnimation != null) {
            mExitScreenshotAnimation.end();
            mExitScreenshotAnimation.removeAllListeners();
        }
        mExitScreenshotAnimation = ScreenshotAnimatorUtil.createScreenshotDropOutAnimation(mScreenshotView);
        mExitScreenshotAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mPresenter.saveScreenshot();
                removeScreenshotView();
                mIsRunning = false;
            }
        });
        mExitScreenshotAnimation.start();
    }

    private void notifyScreenshotError(Context context, NotificationManager nManager) {
        Resources r = context.getResources();

        // Clear all existing notification, compose the new notification and show it
        Notification.Builder b = new Notification.Builder(context)
                .setTicker(r.getString(R.string.screenshot_failed_title))
                .setContentTitle(r.getString(R.string.screenshot_failed_title))
                .setContentText(r.getString(R.string.screenshot_failed_text))
                .setSmallIcon(R.drawable.stat_notify_image_error).setWhen(System.currentTimeMillis())
                .setVisibility(Notification.VISIBILITY_PUBLIC) // ok to show outside lockscreen
                .setCategory(Notification.CATEGORY_ERROR).setAutoCancel(true)
                .setColor(context.getResources().getColor(R.color.system_notification_accent_color));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            b.setChannelId(RxScreenshot.DISPLAY_NAME);
        }
        Notification n = new Notification.BigTextStyle(b).bigText(r.getString(R.string.screenshot_failed_text)).build();
        nManager.notify(SCREENSHOT_NOTIFICATION_ID, n);
    }

    private void showRectCropLayout(Bitmap bitmap) {
        if (mRectCropDialog == null) {
            mRectCropDialog = new Dialog(mContext);
            final Window window = mRectCropDialog.getWindow();
            window.requestFeature(Window.FEATURE_NO_TITLE);
            mRectCropDialog.setContentView(R.layout.layout_crop_float);
            mRectCropDialog.create();

            final WindowManager.LayoutParams lp = window.getAttributes();
            lp.token = null;
            lp.y = 0;
            lp.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED;
            lp.type = ViewUtils.getFloatType();
            lp.format = PixelFormat.TRANSLUCENT;
            lp.setTitle(TAG);
            window.setAttributes(lp);
            updateRectCropLayoutWidth();

            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        }
        final CropImageView cropImageView = (CropImageView) mRectCropDialog.findViewById(R.id.cropImageView);
        cropImageView.setImageBitmap(bitmap);
        cropImageView.setOnDoubleTapListener(new CropImageView.OnDoubleTapListener() {
            @Override
            public void onDoubleTab() {
                final Bitmap result = cropImageView.getCroppedBitmap();
                mPresenter.setBitmap(result);
                showScreenshotAnim(result, false, false);
                dismissRectCropLayout();
            }
        });
        final View toastView = mRectCropDialog.findViewById(R.id.crop_toast);
        toastView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                toastView.setVisibility(View.GONE);
                return true;
            }
        });

        if (!mRectCropDialog.isShowing())
            mRectCropDialog.show();
    }

    private void updateRectCropLayoutWidth() {
        if (mRectCropDialog != null) {
            int screenShowWidth = ViewUtils.getWidth();
            int screenShowHeight = ViewUtils.getHeight();
            final Resources res = mContext.getResources();
            final WindowManager.LayoutParams lp = mRectCropDialog.getWindow().getAttributes();
            lp.width = screenShowWidth;
            lp.height = screenShowHeight;
            lp.gravity = res.getInteger(
                    R.integer.standard_notification_panel_layout_gravity);
            mRectCropDialog.getWindow().setAttributes(lp);
        }
    }

    private void dismissRectCropLayout() {
        if (mRectCropDialog != null && mRectCropDialog.isShowing())
            mRectCropDialog.dismiss();
    }


    private void showFreeCropLayout(Bitmap bitmap) {
        if (mFreeCropDialog == null) {
            mFreeCropDialog = new Dialog(mContext);
            final Window window = mFreeCropDialog.getWindow();
            window.requestFeature(Window.FEATURE_NO_TITLE);
            mFreeCropDialog.setContentView(R.layout.layout_free_crop_float);
            mFreeCropDialog.create();

            final WindowManager.LayoutParams lp = window.getAttributes();
            lp.token = null;
            lp.y = 0;
            lp.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED;
            lp.type = ViewUtils.getFloatType();
            lp.format = PixelFormat.TRANSLUCENT;
            lp.setTitle(TAG);
            window.setAttributes(lp);
            updateFreeCropLayoutWidth();

            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        }
        final FreeCropView freeCropView = mFreeCropDialog.findViewById(R.id.free_crop_view);
        final View confirmBtn = mFreeCropDialog.findViewById(R.id.free_crop_ok_btn);
        final View toastView = mFreeCropDialog.findViewById(R.id.free_crop_toast);
        freeCropView.setFreeCropBitmap(bitmap);
        freeCropView.setOnStateListener(new FreeCropView.OnStateListener() {
            @Override
            public void onStart() {
                confirmBtn.setVisibility(View.GONE);
                toastView.setVisibility(View.GONE);
            }

            @Override
            public void onEnd(boolean ok) {
                if (ok) {
                    confirmBtn.setAlpha(0f);
                    confirmBtn.setVisibility(View.VISIBLE);
                    confirmBtn.animate().alpha(1f);
                } else {
                    toastView.setAlpha(0f);
                    toastView.setVisibility(View.VISIBLE);
                    toastView.animate().alpha(1f);
                }
            }
        });
        confirmBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Bitmap result = freeCropView.getFreeCropBitmap();
                mPresenter.setBitmap(result);
                if (result != null && !result.isRecycled())
                    showScreenshotAnim(result, false, false);
                else
                    showScreenshotError(new Throwable("bitmap is null..."));
                dismissFreeCropLayout();
            }
        });

        if (!mFreeCropDialog.isShowing())
            mFreeCropDialog.show();
    }

    private void updateFreeCropLayoutWidth() {
        if (mFreeCropDialog != null) {
            final Resources res = mContext.getResources();
            final WindowManager.LayoutParams lp = mFreeCropDialog.getWindow().getAttributes();
            lp.width = ViewUtils.getWidth();
            lp.height = ViewUtils.getHeight();
            lp.gravity = res.getInteger(
                    R.integer.standard_notification_panel_layout_gravity);
            mFreeCropDialog.getWindow().setAttributes(lp);
        }
    }

    private void dismissFreeCropLayout() {
        if (mFreeCropDialog != null && mFreeCropDialog.isShowing())
            mFreeCropDialog.dismiss();
    }
}
