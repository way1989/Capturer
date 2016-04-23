package com.way.capture.screenshot;

import android.app.Dialog;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.way.capture.R;
import com.way.capture.fragment.SettingsFragment;
import com.way.capture.utils.DensityUtil;

import java.nio.ByteBuffer;

public class TakeScreenshotService extends Service implements ImageReader.OnImageAvailableListener,
        View.OnClickListener, GlobalScreenshot.Callback {
    public static final int MAX_MOVE_TIMES = 20;
    public static final String ACTION_LONG_SCREENSHOT = "com.way.ACTION_LONG_SCREENSHOT";
    private static final String TAG = "CropScreenshotService";
    private static final int GET_BITMAP_MESSAGE = 0x122;
    private static final int SCROLL_MESSAGE = 0x123;
    private static final int TAKE_SCREENSHOT_MESSAGE = 0x124;
    private static final int OUT_TIME_MESSAGE = 0x125;
    private static final long OUT_TIME_DURATION = 8000L;
    private static final int CMD_SWIPE_TIME = 1000;
    private static final int CMD_WAIT_ENOUGH_TIME = 1500;
    private static final int CMD_MOVE_Y_HEIGHT = 200;
    private static final int CMD_START_MOVE_X = 10;
    private static final String DISPLAY_NAME = "Screenshot";
    private static final String EXTRA_RESULT_CODE = "result-code";
    private static final String EXTRA_DATA = "data";
    private static final boolean IS_DEVICE_ROOT = ShellCmdUtils.isDeviceRoot();
    private static GlobalScreenshot mScreenshot;
    private boolean mIsAutoLongScreenshot;
    private Dialog mDialog;
    private int mCurrentScrollCount = 0;
    private boolean mIsRunning;
    private MediaProjection mProjection;
    private VirtualDisplay mVirtualDisplay;
    private ImageReader mImageReader;
    private boolean mIsLongScreenshot;
    private WindowManager mWindowManager;
    private View mLongScreensshotToast;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case GET_BITMAP_MESSAGE:
                    takeScreenshot();
                    break;
                case SCROLL_MESSAGE:
                    if (mScreenshot == null) {
                        stopSelf();
                        return;
                    }
                    startCapture();
                    break;

                case TAKE_SCREENSHOT_MESSAGE:
                    startCapture();
                    break;
                case OUT_TIME_MESSAGE:
                    Toast.makeText(TakeScreenshotService.this, R.string.screenshot_failed_title,
                            Toast.LENGTH_SHORT).show();
                    Log.i(TAG, TAG + " take screenshot out of time...");
                    stopSelf();
                    break;
                default:
                    break;
            }
        }
    };

    public static Intent newIntent(Context context, int resultCode, Intent data, boolean isLongScreenshot) {
        Intent intent = new Intent(context, TakeScreenshotService.class);
        if (isLongScreenshot)
            intent.setAction(ACTION_LONG_SCREENSHOT);
        intent.putExtra(EXTRA_RESULT_CODE, resultCode);
        intent.putExtra(EXTRA_DATA, data);
        return intent;
    }

    public int getCurrentScrollCount() {
        return mCurrentScrollCount;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // return new Messenger(mHandler).getBinder();
        throw new AssertionError("Not supported.");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, TAG + " onCreate...");
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        // Dismiss the notification that brought us here.
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(GlobalScreenshot.SCREENSHOT_NOTIFICATION_ID);
        mScreenshot = new GlobalScreenshot(TakeScreenshotService.this);
        mIsAutoLongScreenshot = IS_DEVICE_ROOT && PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(SettingsFragment.LONG_SCREENSHOT_AUTO, true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, TAG + " onDestroy...");
        dismissLongScreenshotToast();
        if (mImageReader != null)
            mImageReader.close();
        if (mVirtualDisplay != null)
            mVirtualDisplay.release();
        if (mProjection != null) {
            mProjection.stop();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mIsRunning) {
            Log.d(TAG, "Already running! Ignoring...");
            Toast.makeText(this, R.string.screenshot_saving_title, Toast.LENGTH_SHORT).show();
            return START_NOT_STICKY;
        }
        Log.d(TAG, "Starting up!");
        mIsRunning = true;
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        final int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
        final Intent data = intent.getParcelableExtra(EXTRA_DATA);
        if (resultCode == 0 || data == null) {
            throw new IllegalStateException("Result code or data missing.");
        }
        mIsLongScreenshot = TextUtils.equals(intent.getAction(), ACTION_LONG_SCREENSHOT);
        if (mIsLongScreenshot && getResources().getConfiguration().orientation != Configuration.ORIENTATION_PORTRAIT) {
            Toast.makeText(this, R.string.turn_screen_orientation, Toast.LENGTH_SHORT).show();
            stopSelf();
            return START_NOT_STICKY;
        }
        MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mProjection = projectionManager.getMediaProjection(resultCode, data);

        mHandler.removeMessages(TAKE_SCREENSHOT_MESSAGE);
        mHandler.sendEmptyMessageDelayed(TAKE_SCREENSHOT_MESSAGE, 200L);
        return START_NOT_STICKY;
    }

    private void startCapture() {

        WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        mImageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 1);// 只获取一张图片
        mVirtualDisplay = mProjection.createVirtualDisplay(DISPLAY_NAME, screenWidth, screenHeight, displayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION, mImageReader.getSurface(), null, null);
        mImageReader.setOnImageAvailableListener(this, null);
        mHandler.removeMessages(OUT_TIME_MESSAGE);
        mHandler.sendEmptyMessageDelayed(OUT_TIME_MESSAGE, OUT_TIME_DURATION);// 设置截屏超时时间
    }

    private Bitmap addBorder(Bitmap bitmap, int width) {
        Bitmap newBitmap = Bitmap.createBitmap(width + bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas = new Canvas(newBitmap);
        canvas.drawColor(Color.TRANSPARENT);
        canvas.drawBitmap(bitmap, 0.0F, 0.0F, null);
        return newBitmap;
    }

    private void takeScreenshot() {
        Image image = mImageReader.acquireLatestImage();
        if (image == null) {
            throw new NullPointerException("image is null...");
        }
        int imageWidth = DensityUtil.getDisplayWidth(this);
        int imageHeight = image.getHeight();
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer byteBuffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride() - pixelStride * imageWidth;
        Bitmap bitmap = Bitmap.createBitmap(imageWidth + rowStride / pixelStride, imageHeight,
                Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(byteBuffer);
        if (rowStride != 0) {
            bitmap = addBorder(bitmap, -(rowStride / pixelStride));
        }
        image.close();
        if (mImageReader != null)
            mImageReader.close();
        if (mCurrentScrollCount == 0) {
            mScreenshot.takeScreenshot(bitmap, new Runnable() {
                @Override
                public void run() {
                    if (mIsLongScreenshot) {
                        showLongScreenshotToast();
                        scrollToNextScreen();
                    } else {
                        stopSelf();
                    }
                }
            }, this, mIsLongScreenshot);
        } else {
            mScreenshot.takeScreenshotByScroll(bitmap, new Runnable() {
                @Override
                public void run() {
                    if (mCurrentScrollCount >= MAX_MOVE_TIMES) {
                        mHandler.removeMessages(SCROLL_MESSAGE);
                        mHandler.sendEmptyMessageDelayed(SCROLL_MESSAGE, CMD_WAIT_ENOUGH_TIME);
                        return;
                    }
                    scrollToNextScreen();
                }
            }, this);
        }

    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        mHandler.removeMessages(OUT_TIME_MESSAGE);
        mImageReader.setOnImageAvailableListener(null, null);// 移除监听
        mHandler.removeMessages(GET_BITMAP_MESSAGE);
        mHandler.sendEmptyMessage(GET_BITMAP_MESSAGE);
    }

    private void scrollToNextScreen() {
        //如果手机没有root，就需要用户手动滚动屏幕了
        if (!mIsAutoLongScreenshot) {
            enableDialogTouchFlag(true);
            return;
        }

        int screenShowHeight = DensityUtil.getDisplayHeight(this);
        int cmdStartY = screenShowHeight / 2;
        int cmdEndY = cmdStartY - DensityUtil.dip2px(this, CMD_MOVE_Y_HEIGHT);

        ShellCmdUtils.execShellCmd(ShellCmdUtils.getSwipeCmd(CMD_START_MOVE_X, cmdStartY,
                CMD_START_MOVE_X, cmdEndY, CMD_SWIPE_TIME));

        mHandler.removeMessages(SCROLL_MESSAGE);
        mHandler.sendEmptyMessageDelayed(SCROLL_MESSAGE, CMD_WAIT_ENOUGH_TIME);

        mCurrentScrollCount++;
    }

    private void showLongScreenshotToast() {
        if (mDialog == null) {
            mDialog = new Dialog(this);
            final Window window = mDialog.getWindow();
            window.requestFeature(Window.FEATURE_NO_TITLE);
            final View rootView = LayoutInflater.from(this).inflate(R.layout.long_screenshot_indicator, null);
            mDialog.setContentView(rootView);
            mDialog.create();

            final WindowManager.LayoutParams lp = window.getAttributes();
            lp.y = 0;
            lp.type = WindowManager.LayoutParams.TYPE_TOAST;
            lp.format = PixelFormat.TRANSLUCENT;
            window.setAttributes(lp);
            addLongScreenshotToast();
            updateLongScreenshotWidth();

            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
            if (mIsAutoLongScreenshot)
                rootView.setOnClickListener(this);
        }
        if (!mDialog.isShowing())
            mDialog.show();
    }

    private void enableDialogTouchFlag(boolean enable) {
        if (mDialog == null || !mDialog.isShowing())
            return;
        TextView textView = (TextView) mLongScreensshotToast.findViewById(R.id.long_screenshot_text);
        TextView title = (TextView) mLongScreensshotToast.findViewById(R.id.long_screenshot_title);
        title.setText(getString(R.string.long_screenshot_indicator_title, mCurrentScrollCount));
        if (enable) {
            textView.setText(R.string.long_screenshot_indicator);
            mDialog.findViewById(R.id.long_screenshot_indicator_arrow).setVisibility(View.VISIBLE);
            mDialog.findViewById(R.id.long_screenshot_indicator).setVisibility(View.VISIBLE);
            mDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);//允许用户滚动屏幕
        } else {
            textView.setText(R.string.long_screenshot_progressing);
            mDialog.findViewById(R.id.long_screenshot_indicator).setVisibility(View.GONE);
            mDialog.findViewById(R.id.long_screenshot_indicator_arrow).setVisibility(View.GONE);
            mDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);//禁止用户滚动屏幕
        }
    }

    private void addLongScreenshotToast() {
        mLongScreensshotToast = LayoutInflater.from(this).inflate(R.layout.long_screenshot_toast, null);
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
            mLongScreensshotToast.findViewById(R.id.toast_dialog_bg_container).setOnClickListener(this);
        }
        layoutParams.y = 0;
        layoutParams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED;
        layoutParams.windowAnimations = R.style.VolumePanelAnimation;
        layoutParams.gravity = getResources().getInteger(
                R.integer.standard_notification_panel_layout_gravity);
        layoutParams.width = DensityUtil.getDisplayWidth(this);
        layoutParams.height = DensityUtil.dip2px(this, 88);

        mWindowManager.addView(mLongScreensshotToast, layoutParams);
    }

    private void updateLongScreenshotWidth() {
        if (mDialog != null) {
            int screenShowWidth = DensityUtil.getDisplayWidth(this);
            int screenShowHeight = DensityUtil.getDisplayHeight(this);
            final Resources res = getResources();
            final WindowManager.LayoutParams lp = mDialog.getWindow().getAttributes();
            lp.width = screenShowWidth - 2 * (CMD_START_MOVE_X + 1);
            lp.height = screenShowHeight;
            lp.gravity = res.getInteger(
                    R.integer.standard_notification_panel_layout_gravity);
            mDialog.getWindow().setAttributes(lp);
        }
    }

    private void dismissLongScreenshotToast() {
        if (mDialog != null && mDialog.isShowing())
            mDialog.dismiss();
        if (mLongScreensshotToast != null) {
            mWindowManager.removeViewImmediate(mLongScreensshotToast);
            mLongScreensshotToast = null;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.long_screenshot_indicator_root:
                mCurrentScrollCount = MAX_MOVE_TIMES;
                break;
            case R.id.toast_dialog_bg_container:
                if (mHandler.hasMessages(SCROLL_MESSAGE))
                    return;
                enableDialogTouchFlag(false);
                mHandler.sendEmptyMessage(SCROLL_MESSAGE);
                mCurrentScrollCount++;
                break;
            default:
                break;
        }
    }

    @Override
    public void onCaptureFinish(boolean succeed, boolean scrollScreenshot) {
        if (scrollScreenshot)
            dismissLongScreenshotToast();
    }

    @Override
    public void onSaveFinish(Uri uri) {
        dismissLongScreenshotToast();
        stopSelf();
    }

}
