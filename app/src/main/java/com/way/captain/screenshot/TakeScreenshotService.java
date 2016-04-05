package com.way.captain.screenshot;

import android.app.Dialog;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.hardware.display.DisplayManager;
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
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.way.captain.R;
import com.way.captain.fragment.SettingsFragment;
import com.way.captain.utils.DensityUtil;

import java.nio.ByteBuffer;

public class TakeScreenshotService extends Service implements ImageReader.OnImageAvailableListener, View.OnClickListener, GlobalScreenshot.Callback {
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
    private static GlobalScreenshot mScreenshot;
    private Dialog mDialog;
    private int mCurrentScrollCount = 0;
    private boolean mIsRunning;
    private int mScreenWidth;
    private int mScreenHeight;
    private MediaProjection mProjection;
    private MediaProjectionManager mProjectionManager;
    private ImageReader mImageReader;
    private boolean mIsLongScreenshot;
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
                    //takeScreenshotByScroll();
                    startCapture();
                    break;

                case TAKE_SCREENSHOT_MESSAGE:
                    //takeScreenshot();
                    startCapture();
                    break;
                case OUT_TIME_MESSAGE:
                    Toast.makeText(TakeScreenshotService.this, R.string.screenshot_failed_title, Toast.LENGTH_SHORT).show();

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
        if(isLongScreenshot)
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
        // Dismiss the notification that brought us here.
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(GlobalScreenshot.SCREENSHOT_NOTIFICATION_ID);
        mScreenshot = new GlobalScreenshot(TakeScreenshotService.this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, TAG + " onDestroy...");
        dismissLongScreenshotToast();
        if (mImageReader != null)
            mImageReader.close();
        if (mProjection != null)
            mProjection.stop();
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
        if(intent == null){
            stopSelf();
            return START_NOT_STICKY;
        }

        final int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
        final Intent data = intent.getParcelableExtra(EXTRA_DATA);
        if (resultCode == 0 || data == null) {
            throw new IllegalStateException("Result code or data missing.");
        }
        mIsLongScreenshot = TextUtils.equals(intent.getAction(), ACTION_LONG_SCREENSHOT);
        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mProjection = mProjectionManager.getMediaProjection(resultCode, data);

        mHandler.removeMessages(TAKE_SCREENSHOT_MESSAGE);
        mHandler.sendEmptyMessageDelayed(TAKE_SCREENSHOT_MESSAGE, 200L);
        return START_NOT_STICKY;
    }

    private void startCapture() {

        WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(displayMetrics);
        mScreenWidth = displayMetrics.widthPixels;
        mScreenHeight = displayMetrics.heightPixels;
        mImageReader = ImageReader.newInstance(mScreenWidth, mScreenHeight, PixelFormat.RGBA_8888, 1);// 只获取一张图片
        mProjection.createVirtualDisplay(DISPLAY_NAME, mScreenWidth, mScreenHeight, displayMetrics.densityDpi,
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
        int imageHeight = image.getHeight();
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer byteBuffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride() - pixelStride * mScreenWidth;
        Bitmap bitmap = Bitmap.createBitmap(mScreenWidth + rowStride / pixelStride, imageHeight,
                Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(byteBuffer);
        if (rowStride != 0) {
            bitmap = addBorder(bitmap, -(rowStride / pixelStride));
        }
        image.close();
        if (mImageReader != null)
            mImageReader.close();
//        if (mProjection != null)
//            mProjection.stop();
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
                        //takeScreenshotByScroll();
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

        int screenShowHeight = DensityUtil.getDisplayHeight(this);
        int cmd_start_y = screenShowHeight / 2;
        int cmd_end_y = cmd_start_y - DensityUtil.dip2px(this, CMD_MOVE_Y_HEIGHT);

        ShellCmdUtils.execShellCmd(ShellCmdUtils.getSwipeCmd(CMD_START_MOVE_X, cmd_start_y,
                CMD_START_MOVE_X, cmd_end_y, CMD_SWIPE_TIME));

        mHandler.removeMessages(SCROLL_MESSAGE);
        mHandler.sendEmptyMessageDelayed(SCROLL_MESSAGE, CMD_WAIT_ENOUGH_TIME);

        mCurrentScrollCount++;
    }

    private void showLongScreenshotToast() {
        if (mDialog == null) {
            mDialog = new Dialog(this);
            final Window window = mDialog.getWindow();
            window.requestFeature(Window.FEATURE_NO_TITLE);
            mDialog.setContentView(R.layout.toast_dialog);
            mDialog.create();

            final WindowManager.LayoutParams lp = window.getAttributes();
            lp.token = null;
            lp.y = 0;
            lp.type = WindowManager.LayoutParams.TYPE_TOAST;
            lp.format = PixelFormat.TRANSLUCENT;
            lp.setTitle(TAG);
            window.setAttributes(lp);
            updateLongScreenshotWidth();

            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
            mDialog.findViewById(R.id.toast_dialog_bg_container).setOnClickListener(this);
        }
        if (!mDialog.isShowing())
            mDialog.show();
    }

    private void updateLongScreenshotWidth() {
        if (mDialog != null) {
            WindowManager windowManager =
                    (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            int screenShowWidth = display.getWidth();
            int screenShowHeight = display.getHeight();
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
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.toast_dialog_bg_container:
                mCurrentScrollCount = MAX_MOVE_TIMES;
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
