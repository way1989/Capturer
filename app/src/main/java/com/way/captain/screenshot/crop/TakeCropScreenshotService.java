package com.way.captain.screenshot.crop;

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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.way.captain.R;
import com.way.captain.utils.DensityUtil;
import com.way.captain.widget.freecrop.FreeCropView;
import com.way.captain.widget.rectcrop.CropImageView;

import java.nio.ByteBuffer;

public class TakeCropScreenshotService extends Service {
    public static final String ACTION_FREE_SCREENSHOT = "com.way.ACTION_FREE_SCREENSHOT";
    public static final String ACTION_RECT_SCREENSHOT = "com.way.ACTION_RECT_SCREENSHOT";
    private static final String TAG = "CropScreenshotService";
    private static final String DISPLAY_NAME = "Screenshot";
    private static final String EXTRA_RESULT_CODE = "result-code";
    private static final String EXTRA_DATA = "data";
    private static GlobalCropScreenshot mScreenshot;
    final Object mScreenshotLock = new Object();
    final Runnable mScreenshotTimeout = new Runnable() {
        @Override
        public void run() {
            synchronized (mScreenshotLock) {
                Toast.makeText(TakeCropScreenshotService.this, R.string.screenshot_failed_title, Toast.LENGTH_SHORT).show();
                stopSelf();
            }
        }
    };
    private boolean mIsRunning;
    private Bitmap mScreenShotBitmap;
    private int mScreenWidth;
    private int mScreenHeight;
    private MediaProjection mProjection;
    private MediaProjectionManager mProjectionManager;
    private VirtualDisplay mVirtualDisplay;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private ImageReader mImageReader;
    private WindowManager mWindowManager;
    private ImageReader.OnImageAvailableListener mListener = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            mHandler.removeCallbacks(mScreenshotTimeout);
            mImageReader.setOnImageAvailableListener(null, null);// 移除监听
            mHandler.removeCallbacks(getScreenshotBitmapTask);
            mHandler.post(getScreenshotBitmapTask);
        }
    };
    private Dialog mRectCropDialog;
    private Dialog mFreeCropDialog;
    private boolean isFreeCrop;
    Runnable getScreenshotBitmapTask = new Runnable() {

        @Override
        public void run() {
            getImage();
        }
    };

    public static Intent newIntent(Context context, int resultCode, Intent data, String action) {
        Intent intent = new Intent(context, TakeCropScreenshotService.class);
        intent.setAction(action);
        intent.putExtra(EXTRA_RESULT_CODE, resultCode);
        intent.putExtra(EXTRA_DATA, data);
        return intent;
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
        notificationManager.cancel(GlobalCropScreenshot.SCREENSHOT_NOTIFICATION_ID);
        mWindowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);

        mScreenshot = new GlobalCropScreenshot(TakeCropScreenshotService.this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateFreeCropLayoutWidth();
        updateRectCropLayoutWidth();
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
        } else {
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, TAG + " onDestroy...");
        if (mScreenShotBitmap != null && !mScreenShotBitmap.isRecycled())
            mScreenShotBitmap.recycle();
        mScreenShotBitmap = null;
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
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        Log.d(TAG, "Starting up!");
        mIsRunning = true;
        isFreeCrop = TextUtils.equals(ACTION_FREE_SCREENSHOT, intent.getAction());
        final int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
        final Intent data = intent.getParcelableExtra(EXTRA_DATA);
        if (resultCode == 0 || data == null) {
            throw new IllegalStateException("Result code or data missing.");
        }

        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mProjection = mProjectionManager.getMediaProjection(resultCode, data);
        mHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                startCapture();
            }
        }, 200L);
        return START_NOT_STICKY;
    }

    private void startCapture() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        mScreenWidth = displayMetrics.widthPixels;
        mScreenHeight = displayMetrics.heightPixels;
        mImageReader = ImageReader.newInstance(mScreenWidth, mScreenHeight, PixelFormat.RGBA_8888, 1);// 只获取一张图片
        mVirtualDisplay = mProjection.createVirtualDisplay(DISPLAY_NAME, mScreenWidth, mScreenHeight, displayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION, mImageReader.getSurface(), null, null);
        mImageReader.setOnImageAvailableListener(mListener, null);
        mHandler.postDelayed(mScreenshotTimeout, 5000L);// 设置截屏超时时间
    }

    private Bitmap addBorder(Bitmap bitmap, int width) {
        Bitmap newBitmap = Bitmap.createBitmap(width + bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas = new Canvas(newBitmap);
        canvas.drawColor(Color.TRANSPARENT);
        canvas.drawBitmap(bitmap, 0.0F, 0.0F, null);
        return newBitmap;
    }

    private void getImage() {

        Image image = mImageReader.acquireLatestImage();
        if (image == null) {
            throw new NullPointerException("image is null...");
        }
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        Log.i("broncho", "getImage: imageWidth = " + imageWidth + ", imageHeight = " + imageHeight
                + ", mScreenWidth = " + mScreenWidth + ", mScreenHeight = " + mScreenHeight);
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer byteBuffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride() - pixelStride * mScreenWidth;
        mScreenShotBitmap = Bitmap.createBitmap(mScreenWidth + rowStride / pixelStride, imageHeight,
                Bitmap.Config.ARGB_8888);
        mScreenShotBitmap.copyPixelsFromBuffer(byteBuffer);
        if (rowStride != 0) {
            mScreenShotBitmap = addBorder(mScreenShotBitmap, -(rowStride / pixelStride));
        }
        image.close();
        if (mImageReader != null)
            mImageReader.close();
        if (mProjection != null)
            mProjection.stop();
        mVirtualDisplay.release();
        takeScreenshot();
    }

    private void takeScreenshot() {
        if (mScreenShotBitmap == null || mScreenShotBitmap.isRecycled()) {
            stopSelf();
            return;
        }
        mScreenshot.takeScreenshot(mScreenShotBitmap, new Runnable() {
            @Override
            public void run() {
                if (mScreenShotBitmap == null || mScreenShotBitmap.isRecycled())
                    return;
                if (isFreeCrop)
                    showFreeCropLayout(removeNavigationBar(mScreenShotBitmap));
                else
                    showRectCropLayout(removeNavigationBar(mScreenShotBitmap));
            }
        }, false);
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

    private boolean hasNavigationBar() {
        boolean hasMenukey = ViewConfiguration.get(getApplicationContext()).hasPermanentMenuKey();
        boolean hasBackkey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
        return !hasMenukey && !hasBackkey;
    }

    private int getNavigationBarHeight() {
        Resources resources = getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    private int getNavigationBarWidth() {
        Resources resources = getResources();
        int resourceId = resources.getIdentifier("navigation_bar_width", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;

    }

    private void showRectCropLayout(Bitmap bitmap) {
        if (mRectCropDialog == null) {
            mRectCropDialog = new Dialog(this);
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
                mScreenshot.startAnimation(cropImageView.getCroppedBitmap(), new Runnable() {
                    @Override
                    public void run() {
                        stopSelf();
                    }
                });
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
            int screenShowWidth = DensityUtil.getDisplayWidth(this);
            int screenShowHeight = DensityUtil.getDisplayHeight(this);
            final Resources res = getResources();
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
            mFreeCropDialog = new Dialog(this);
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
                mScreenshot.startAnimation(freeCropView.getFreeCropBitmap(), new Runnable() {
                    @Override
                    public void run() {
                        stopSelf();
                    }
                });
                dismissFreeCropLayout();

            }
        });

        if (!mFreeCropDialog.isShowing())
            mFreeCropDialog.show();
    }

    private void updateFreeCropLayoutWidth() {
        if (mFreeCropDialog != null) {
            int screenShowWidth = DensityUtil.getDisplayWidth(this);
            int screenShowHeight = DensityUtil.getDisplayHeight(this);
            final Resources res = getResources();
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
