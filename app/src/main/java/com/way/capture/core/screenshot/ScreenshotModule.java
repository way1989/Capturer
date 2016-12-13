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
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.way.capture.R;
import com.way.capture.core.BaseModule;
import com.way.capture.fragment.SettingsFragment;
import com.way.capture.service.ModuleService;
import com.way.capture.utils.DensityUtil;
import com.way.capture.utils.ScrollUtils;
import com.way.capture.widget.freecrop.FreeCropView;
import com.way.capture.widget.rectcrop.CropImageView;
import com.way.capture.widget.swipe.SwipeHelper;
import com.way.capture.widget.swipe.SwipeVerticalLayout;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created by android on 16-8-22.
 */
public class ScreenshotModule implements BaseModule, ScreenshotContract.View, SwipeVerticalLayout.Callback,
        SwipeHelper.PressListener, View.OnClickListener {
    public static final int SCREENSHOT_NOTIFICATION_ID = 789;
    //save style
    static final int STYLE_SAVE_ONLY = 0;
    static final int STYLE_SAVE_TO_SHARE = 1;
    static final int STYLE_SAVE_TO_EDIT = 2;
    private static final String TAG = "TakeScreenshotService";
    private static final String SCREENSHOT_SHARE_SUBJECT_TEMPLATE = "Screenshot (%s)";
    private static final long PREVIEW_OUT_TIME = 3000L;// 超时自动保存
    private static final long TAKE_SCREENSHOT_DELAY = 500L;
    private static final int TAKE_SCREENSHOT_MESSAGE = 0x121;
    private static final int TAKE_LONG_SCREENSHOT_MESSAGE = 0x122;
    private static final int AUTO_SAVE_MESSAGE = 0x123;
    //anim
    private static final int SCREENSHOT_FLASH_TO_PEAK_DURATION = 130;
    private static final int SCREENSHOT_DROP_IN_DURATION = 430;
    private static final int SCREENSHOT_DROP_OUT_DELAY = 500;
    private static final int SCREENSHOT_DROP_OUT_DURATION = 430;
    private static final int SCREENSHOT_DROP_OUT_SCALE_DURATION = 370;
    private static final float BACKGROUND_ALPHA = 0.8f;
    private static final float SCREENSHOT_SCALE = 1f;
    private static final float SCREENSHOT_DROP_IN_MIN_SCALE = SCREENSHOT_SCALE * 0.689f;
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
    private Button mEditBtn;
    private Button mLongScreenshotBtn;
    private Button mShareBtn;
    private ImageView mBackgroundView;
    private ImageView mScreenshotView;
    private ImageView mScreenshotFlash;
    private View mBtnControlView;
    private View mSwipeUpToDeleteView;
    private View mSwipeDownToSaveView;
    private View mLongScreenshotToast;
    private View mLongScreenshotCover;
    private ValueAnimator mScreenshotAnimation;
    private ValueAnimator mExitScreenshotAnimation;
    private Context mContext;
    private String mAction;
    private ScreenshotContract.Presenter mPresenter;
    private boolean mIsAutoLongScreenshot;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TAKE_SCREENSHOT_MESSAGE:
                    Log.i(TAG, "takeScreenshot...");
                    //cancel the notification
                    mNotificationManager.cancel(SCREENSHOT_NOTIFICATION_ID);
                    mPresenter.takeScreenshot();
                    break;
                case TAKE_LONG_SCREENSHOT_MESSAGE:
                    mPresenter.takeLongScreenshot(mIsAutoLongScreenshot);
                    break;
                case AUTO_SAVE_MESSAGE:
                    mSwipeVerticalLayout.setEnabled(false);
                    if (!mKeyguardManager.isKeyguardLocked() || !mKeyguardManager.isKeyguardSecure()) {
                        mShareBtn.animate().alpha(0);
                        mLongScreenshotBtn.animate().alpha(0);
                        mEditBtn.animate().alpha(0);
                    }
                    mSwipeDownToSaveView.animate().alpha(0);
                    mSwipeUpToDeleteView.animate().alpha(0);
                    playScreenshotDropOutAnimation();
                    break;
                default:
                    break;
            }
        }
    };
    private ObjectAnimator mFloatAnim;
    private Dialog mRectCropDialog;
    private Dialog mFreeCropDialog;

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
        mHandler.removeMessages(TAKE_SCREENSHOT_MESSAGE);
        mHandler.sendEmptyMessageDelayed(TAKE_SCREENSHOT_MESSAGE, TAKE_SCREENSHOT_DELAY);
    }

    private void initSystemManager(Context context) {
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mKeyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mWindowLayoutParams = new WindowManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 0, WindowManager.LayoutParams.TYPE_TOAST,
                WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                PixelFormat.TRANSLUCENT);
        mWindowLayoutParams.setTitle("Screenshot");
        mWindowLayoutParams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED;//lock current screen orientation
    }

    private void initViews() {
        mRootView = (FrameLayout) mLayoutInflater.inflate(R.layout.global_screenshot_root, null);
        mSwipeVerticalLayout = (SwipeVerticalLayout) mRootView.findViewById(R.id.swipe_layout);
        mSwipeVerticalLayout.setEnabled(false);
        mBtnControlView = mRootView.findViewById(R.id.btn_view);
        mSwipeUpToDeleteView = mRootView.findViewById(R.id.swipe_to_delete);
        mSwipeDownToSaveView = mRootView.findViewById(R.id.swipe_to_save);
        mSwipeVerticalLayout.setCallback(this);
        mSwipeVerticalLayout.setPressListener(this);
        mEditBtn = (Button) mRootView.findViewById(R.id.edit_btn);
        mLongScreenshotBtn = (Button) mRootView.findViewById(R.id.scroll_screenshot_btn);
        mShareBtn = (Button) mRootView.findViewById(R.id.share_btn);
        mEditBtn.setOnClickListener(this);
        mLongScreenshotBtn.setOnClickListener(this);
        mShareBtn.setOnClickListener(this);

        mBackgroundView = (ImageView) mRootView.findViewById(R.id.global_screenshot_background);
        mScreenshotView = (ImageView) mRootView.findViewById(R.id.global_screenshot);
        mScreenshotFlash = (ImageView) mRootView.findViewById(R.id.global_screenshot_flash);
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        clearFloatAnim();
        removeScreenshotView();
        dismissLongScreenshotToast();
        mPresenter.release();
    }

    private Bitmap resizeBitmap(Bitmap bitmap, float scale) {
        Log.i(TAG, "resizeBitmap....");
        if (bitmap == null || bitmap.isRecycled())
            return null;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }

    private boolean hasNavigationBar() {
        boolean hasMenukey = ViewConfiguration.get(mContext).hasPermanentMenuKey();
        boolean hasBackkey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
        return !hasMenukey && !hasBackkey;
    }

    private int getNavigationBarWidth() {
        Resources resources = mContext.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_width", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;

    }

    private int getNavigationBarHeight() {
        Resources resources = mContext.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    private Bitmap removeNavigationBar(Bitmap bitmap) {
        boolean hasNavigationBar = hasNavigationBar();
        if (!hasNavigationBar)
            return bitmap;

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (height > width) {//竖屏情况
            int navigationHeight = getNavigationBarHeight();
            if (navigationHeight == 0)
                return bitmap;
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                    bitmap.getHeight() - navigationHeight);
        }

        //横屏
        int navigationWidth = getNavigationBarWidth();
        if (navigationWidth == 0)
            return bitmap;
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth() - navigationWidth,
                bitmap.getHeight());

    }

    @Override
    public void showScreenshotAnim(Bitmap bitmap, final boolean longScreenshot, final boolean needCheckAction) {
        if (needCheckAction) {
            switch (mAction) {
                case ModuleService.Action.ACTION_FREE_CROP:
                    showFreeCropLayout(removeNavigationBar(bitmap));
                    return;
                case ModuleService.Action.ACTION_RECT_CROP:
                    showRectCropLayout(removeNavigationBar(bitmap));
                    return;
            }
        }
        // Add the view for the animation
        if (longScreenshot) {
            int heightPixels = DensityUtil.getDisplayHeight(mContext);
            int count = bitmap.getHeight() / heightPixels;
            count = Math.max(1, Math.min(5, count));
            if (count > 1) bitmap = resizeBitmap(bitmap, 1.00f / count);
            Log.i(TAG, "mScreenBitmap.getHeight() = " + bitmap.getHeight()
                    + ", mDisplayMetrics.heightPixels = " + heightPixels
                    + ", count = " + count + ", 1.00f / count = " + (1.00f / count));
        }

        // Optimizations
        bitmap.setHasAlpha(false);
        bitmap.prepareToDraw();
        mScreenshotView.setImageBitmap(bitmap);

        mRootView.requestFocus();

        // Setup the animation with the screenshot just taken
        if (mScreenshotAnimation != null) {
            mScreenshotAnimation.end();
            mScreenshotAnimation.removeAllListeners();
        }

        mWindowManager.addView(mRootView, mWindowLayoutParams);
        mScreenshotAnimation = createScreenshotDropInAnimation();

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

        if (!mKeyguardManager.isKeyguardLocked() || !mKeyguardManager.isKeyguardSecure()) {
            mShareBtn.setVisibility(View.VISIBLE);
            mShareBtn.animate().alpha(1f);
            mEditBtn.setVisibility(View.VISIBLE);
            mEditBtn.animate().alpha(1f);
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
            mFloatAnim = ObjectAnimator.ofFloat(mScreenshotView, "translationY", 0f, DensityUtil.dip2px(mContext, 8), 0f);
            mFloatAnim.setDuration(2000L);
            mFloatAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            mFloatAnim.setRepeatCount(ValueAnimator.INFINITE);
            mFloatAnim.setRepeatMode(ValueAnimator.REVERSE);
        }
        mFloatAnim.start();
    }

    private void clearFloatAnim() {
        if (mFloatAnim != null && mFloatAnim.isRunning())
            mFloatAnim.end();
    }

    @Override
    public void showScreenshotError(Throwable e) {
        Toast.makeText(mContext, R.string.screenshot_failed_title, Toast.LENGTH_SHORT).show();
        notifyScreenshotError(mContext, mNotificationManager);
        stopSelf();
    }

    private void stopSelf() {
        mContext.stopService(new Intent(mContext, ModuleService.class));
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
    public void editScreenshot(Uri uri) {
        try {
            if (!mKeyguardManager.isKeyguardLocked()) {
                Intent editIntent = new Intent(Intent.ACTION_VIEW);
                editIntent.setDataAndType(uri, "image/png");
                editIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(editIntent);
            }
        } catch (Exception e) {
            Log.i(TAG, "STYLE_SAVE_TO_EDIT e = " + e);
        }
        stopSelf();
    }

    @Override
    public void shareScreenshot(Uri uri) {
        try {
            if (!mKeyguardManager.isKeyguardLocked()) {
                String subjectDate = DateFormat.getDateTimeInstance().format(new Date(System.currentTimeMillis()));
                String subject = String.format(SCREENSHOT_SHARE_SUBJECT_TEMPLATE, subjectDate);
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("image/png");
                sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
                Intent chooserIntent = Intent.createChooser(sharingIntent, null);
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(chooserIntent);
            }
        } catch (Exception e) {
            Log.i(TAG, "STYLE_SAVE_TO_SHARE e = " + e);
        }
        stopSelf();
    }

    @Override
    public void finish() {
        stopSelf();
    }

    private void playAlphaAnim(boolean isDismiss) {
        mHandler.removeMessages(AUTO_SAVE_MESSAGE);
        if (!isDismiss)
            mHandler.sendEmptyMessageDelayed(AUTO_SAVE_MESSAGE, PREVIEW_OUT_TIME);
        if (isDismiss) {
            if (!mKeyguardManager.isKeyguardLocked() || !mKeyguardManager.isKeyguardSecure()) {
                mBtnControlView.animate().alpha(0);
            }
            mSwipeUpToDeleteView.animate().alpha(1);
            mSwipeDownToSaveView.animate().alpha(1);
            clearFloatAnim();
        } else {

            if (!mKeyguardManager.isKeyguardLocked() || !mKeyguardManager.isKeyguardSecure()) {
                mBtnControlView.animate().alpha(1);
            }
            mSwipeUpToDeleteView.animate().alpha(0);
            mSwipeDownToSaveView.animate().alpha(0);
            startFloatAnim();
        }
    }

    @Override
    public void onChildDismissed(View v, int direction) {
        mSwipeVerticalLayout.setEnabled(false);
        mHandler.removeMessages(AUTO_SAVE_MESSAGE);
        switch (direction) {
            case SwipeHelper.SWIPE_TO_TOP:
                Log.i(TAG, TAG + " onChildDismissed... SWIPE_TO_TOP");
                stopSelf();
                break;
            case SwipeHelper.SWIPE_TO_BOTTOM:
                Log.i(TAG, TAG + " onChildDismissed... SWIPE_TO_BOTTOM");
                mPresenter.saveScreenshot(STYLE_SAVE_ONLY);
                removeScreenshotView();
                break;
            default:
                break;
        }
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
            case R.id.edit_btn:
                mRootView.findViewById(R.id.loading).animate().alpha(1);
                mPresenter.saveScreenshot(STYLE_SAVE_TO_EDIT);
                break;
            case R.id.scroll_screenshot_btn:
                showLongScreenshotToast();
                removeScreenshotView();
                takeLongScreenshot();
                break;
            case R.id.share_btn:
                mRootView.findViewById(R.id.loading).animate().alpha(1);
                mPresenter.saveScreenshot(STYLE_SAVE_TO_SHARE);
                break;
            case R.id.toast_dialog_bg_container:
                Log.i(TAG, "onClick... toast_dialog_bg_container");
                enableDialogTouchFlag(false);
                mHandler.removeMessages(TAKE_LONG_SCREENSHOT_MESSAGE);
                mHandler.sendEmptyMessage(TAKE_LONG_SCREENSHOT_MESSAGE);
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
            mHandler.removeMessages(TAKE_LONG_SCREENSHOT_MESSAGE);
            mHandler.sendEmptyMessage(TAKE_LONG_SCREENSHOT_MESSAGE);
        } else {
            enableDialogTouchFlag(true);
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

    private void enableDialogTouchFlag(boolean enable) {
        TextView textView = (TextView) mLongScreenshotToast.findViewById(R.id.long_screenshot_text);
        TextView title = (TextView) mLongScreenshotToast.findViewById(R.id.long_screenshot_title);
        title.setText(R.string.long_screenshot_indicator_title);
        if (enable) {
            textView.setText(R.string.long_screenshot_indicator);
            mLongScreenshotCover.findViewById(R.id.long_screenshot_indicator_arrow).setVisibility(View.VISIBLE);
            mLongScreenshotCover.findViewById(R.id.long_screenshot_indicator).setVisibility(View.VISIBLE);
        } else {
            textView.setText(R.string.long_screenshot_progressing);
            mLongScreenshotCover.findViewById(R.id.long_screenshot_indicator).setVisibility(View.GONE);
            mLongScreenshotCover.findViewById(R.id.long_screenshot_indicator_arrow).setVisibility(View.GONE);
        }
    }

    private void showLongScreenshotToast() {
        addCover();
        addToast();
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
        mLongScreenshotCover = mLayoutInflater.inflate(R.layout.long_screenshot_indicator, null);
        mLongScreenshotCover.setOnClickListener(this);
        //mLongScreenshotCover.setBackgroundColor(Color.parseColor("#80ff0000"));
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 0,
                WindowManager.LayoutParams.TYPE_TOAST,
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
            mLongScreenshotCover.setOnClickListener(null);
        }
        int screenShowWidth = DensityUtil.getDisplayWidth(mContext);
        layoutParams.width = screenShowWidth - 24;//leave some space for swipe
        layoutParams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED;
        mWindowManager.addView(mLongScreenshotCover, layoutParams);
    }

    private void addToast() {
        mLongScreenshotToast = mLayoutInflater.inflate(R.layout.long_screenshot_toast, null);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 0, WindowManager.LayoutParams.TYPE_TOAST,
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
            mLongScreenshotToast.findViewById(R.id.toast_dialog_bg_container).setOnClickListener(this);
        }
        layoutParams.y = 0;
        layoutParams.windowAnimations = R.style.VolumePanelAnimation;
        layoutParams.gravity = mContext.getResources().getInteger(
                R.integer.standard_notification_panel_layout_gravity);
        layoutParams.width = DensityUtil.getDisplayWidth(mContext);
        layoutParams.height = DensityUtil.dip2px(mContext, 88);
        layoutParams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED;
        mWindowManager.addView(mLongScreenshotToast, layoutParams);
    }

    private void removeScreenshotView() {
        try {
            mWindowManager.removeViewImmediate(mRootView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playScreenshotDropOutAnimation() {
        if (mExitScreenshotAnimation != null) {
            mExitScreenshotAnimation.end();
            mExitScreenshotAnimation.removeAllListeners();
        }
        mExitScreenshotAnimation = createScreenshotDropOutAnimation();
        mExitScreenshotAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mPresenter.saveScreenshot(STYLE_SAVE_ONLY);
                removeScreenshotView();
            }
        });
        mExitScreenshotAnimation.start();
    }

    private ValueAnimator createScreenshotDropInAnimation() {
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
                mBackgroundView.setAlpha(0f);
                mBackgroundView.setVisibility(View.VISIBLE);
                mScreenshotView.setAlpha(0f);
                mScreenshotView.setTranslationX(0f);
                mScreenshotView.setTranslationY(0f);
                mScreenshotView.setScaleX(SCREENSHOT_SCALE);
                mScreenshotView.setScaleY(SCREENSHOT_SCALE);
                mScreenshotView.setVisibility(View.VISIBLE);
                mScreenshotFlash.setAlpha(0f);
                mScreenshotFlash.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mScreenshotFlash.setVisibility(View.GONE);
            }
        });
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float t = (Float) animation.getAnimatedValue();
                float scaleT = (SCREENSHOT_SCALE)
                        - scaleInterpolator.getInterpolation(t) * (SCREENSHOT_SCALE - SCREENSHOT_DROP_IN_MIN_SCALE);
                mBackgroundView.setAlpha(scaleInterpolator.getInterpolation(t) * BACKGROUND_ALPHA);
                mScreenshotView.setAlpha(t);
                mScreenshotView.setScaleX(scaleT);
                mScreenshotView.setScaleY(scaleT);
                mScreenshotFlash.setAlpha(flashAlphaInterpolator.getInterpolation(t));
            }
        });
        return anim;
    }

    private ValueAnimator createScreenshotDropOutAnimation() {
        int w = DensityUtil.getDisplayWidth(mContext);
        int h = DensityUtil.getDisplayHeight(mContext);
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setStartDelay(SCREENSHOT_DROP_OUT_DELAY);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mBackgroundView.setVisibility(View.GONE);
                mScreenshotView.setVisibility(View.GONE);
                mScreenshotView.setLayerType(View.LAYER_TYPE_NONE, null);
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
                mBackgroundView.setAlpha((1f - t) * BACKGROUND_ALPHA);
                mScreenshotView.setAlpha(1f - scaleInterpolator.getInterpolation(t));
                mScreenshotView.setTranslationY(t * finalPos.y);
            }
        });
        return anim;
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
            lp.type = WindowManager.LayoutParams.TYPE_TOAST;
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
            int screenShowWidth = DensityUtil.getDisplayWidth(mContext);
            int screenShowHeight = DensityUtil.getDisplayHeight(mContext);
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
            lp.type = WindowManager.LayoutParams.TYPE_TOAST;
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
        final FreeCropView freeCropView = (FreeCropView) mFreeCropDialog.findViewById(R.id.free_crop_view);
        final View confirmBtn = mFreeCropDialog.findViewById(R.id.free_crop_ok_btn);
        freeCropView.setFreeCropBitmap(bitmap);
        freeCropView.setOnStateListener(new FreeCropView.OnStateListener() {
            @Override
            public void onStart() {
                confirmBtn.setVisibility(View.GONE);
                mFreeCropDialog.findViewById(R.id.free_crop_toast).setVisibility(View.GONE);
            }

            @Override
            public void onEnd() {
                confirmBtn.setVisibility(View.VISIBLE);
            }
        });
        confirmBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Bitmap result = freeCropView.getFreeCropBitmap();
                mPresenter.setBitmap(result);
                if(result != null && !result.isRecycled())
                showScreenshotAnim(result, false, false);
                dismissFreeCropLayout();
            }
        });

        if (!mFreeCropDialog.isShowing())
            mFreeCropDialog.show();
    }

    private void updateFreeCropLayoutWidth() {
        if (mFreeCropDialog != null) {
            int screenShowWidth = DensityUtil.getDisplayWidth(mContext);
            int screenShowHeight = DensityUtil.getDisplayHeight(mContext);
            final Resources res = mContext.getResources();
            final WindowManager.LayoutParams lp = mFreeCropDialog.getWindow().getAttributes();
            lp.width = screenShowWidth;
            lp.height = screenShowHeight;
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
