package com.way.capture.service;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.way.capture.R;
import com.way.capture.activity.MainActivity;
import com.way.capture.screenrecord.ScreenRecordShortcutLaunchActivity;
import com.way.capture.screenshot.TakeScreenshotActivity;
import com.way.capture.screenshot.TakeScreenshotService;
import com.way.capture.screenshot.crop.TakeCropScreenshotActivity;
import com.way.capture.screenshot.crop.TakeCropScreenshotService;
import com.way.capture.widget.FloatMenuDialog;


/**
 * ChatHead Service
 */
public class ShakeService extends Service implements View.OnClickListener, SensorEventListener {

    private static final String TAG = "ShakeService";

    private static final int NOTIFICATION_ID = 9083150;

    private static final int SPEED_SHRESHOLD = 60;// 这个值越大需要越大的力气来摇晃手机
    private static final int UPTATE_INTERVAL_TIME = 50;
    private Vibrator mVibrator;
    private boolean mIsRunning;
    private KeyguardManager mKeyguardManager;
    private SensorManager mSensorManager = null;
    private Sensor mSensor;
    private FloatMenuDialog mFloatMenuDialog;
    private float mLastX;
    private float mLastY;
    private float mLastZ;
    private long mLastUpdateTime;
    /**
     * 监听是否点击了home键将客户端推到后台
     */
    private BroadcastReceiver mHomeKeyEventReceiver = new BroadcastReceiver() {
        String SYSTEM_REASON = "reason";
        String SYSTEM_HOME_KEY = "homekey";
        String SYSTEM_HOME_KEY_LONG = "recentapps";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                String reason = intent.getStringExtra(SYSTEM_REASON);
                if (TextUtils.equals(reason, SYSTEM_HOME_KEY)) {
                    //表示按了home键,程序到了后台
                    //Toast.makeText(getApplicationContext(), "home click", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "home key click....");
                    dismissDialog();
                } else if (TextUtils.equals(reason, SYSTEM_HOME_KEY_LONG)) {
                    //表示长按home键,显示最近使用的程序列表
                    //Toast.makeText(getApplicationContext(), "home long click", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "home key long click....");
                    dismissDialog();
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        initData();
    }

    public void initData() {
        mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mSensorManager = (SensorManager) this
                .getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (mSensor != null) {
            mSensorManager.registerListener(this, mSensor,
                    SensorManager.SENSOR_DELAY_GAME);
        }
        //注册Home监听广播
        registerReceiver(mHomeKeyEventReceiver, new IntentFilter(
                Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (TextUtils.equals(action, "com.way.action.SHOW_MENU"))
            showDialog();
        if (mIsRunning) {
            return START_STICKY;
        }
        mIsRunning = true;
        if (Build.VERSION.SDK_INT < 18) {
            startForeground(NOTIFICATION_ID, createNotification());//API < 18 ，此方法能有效隐藏Notification上的图标
        } else {
            Intent innerIntent = new Intent(this, GrayInnerService.class);
            startService(innerIntent);
            startForeground(NOTIFICATION_ID, new Notification());
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mIsRunning = false;
        mSensorManager.unregisterListener(this);
        //注销Home键监听广播
        unregisterReceiver(mHomeKeyEventReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification createNotification() {
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setWhen(System.currentTimeMillis());
        builder.setSmallIcon(R.drawable.ic_widgets);
        builder.setContentTitle(getString(R.string.chathead_content_title));
        builder.setContentText(getString(R.string.chathead_content_text));
        builder.setOngoing(true);
        builder.setPriority(NotificationCompat.PRIORITY_MIN);
        builder.setCategory(NotificationCompat.CATEGORY_SERVICE);

        // PendingIntent
        final Intent notifyIntent = new Intent(this, MainActivity.class);
        PendingIntent notifyPendingIntent = PendingIntent.getActivity(this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(notifyPendingIntent);

        return builder.build();
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.menu_screnshot_center:
                try {
                    Intent i = new Intent(ShakeService.this, MainActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    startActivity(i);
                } catch (Exception e) {
                }
                break;
            case R.id.menu_normal_screenshot:
                try {
                    Intent screenRecordIntent = new Intent(ShakeService.this, TakeScreenshotActivity.class);
                    screenRecordIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    startActivity(screenRecordIntent);
                } catch (ActivityNotFoundException e) {
                }
                break;
            case R.id.menu_screenrecord:
                try {
                    Intent screenRecordIntent = new Intent(ShakeService.this, ScreenRecordShortcutLaunchActivity.class);

                    screenRecordIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    startActivity(screenRecordIntent);
                } catch (ActivityNotFoundException e) {
                }

                break;
            case R.id.menu_long_screenshot:
                try {
                    Intent screenRecordIntent = new Intent(ShakeService.this, TakeScreenshotActivity.class);
                    screenRecordIntent.setAction(TakeScreenshotService.ACTION_LONG_SCREENSHOT);
                    screenRecordIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    startActivity(screenRecordIntent);
                } catch (ActivityNotFoundException e) {
                }
                break;
            case R.id.menu_free_screenshot:
                try {
                    Intent screenRecordIntent = new Intent(ShakeService.this, TakeCropScreenshotActivity.class);
                    screenRecordIntent.setAction(TakeCropScreenshotService.ACTION_FREE_SCREENSHOT);
                    screenRecordIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    startActivity(screenRecordIntent);
                } catch (ActivityNotFoundException e) {
                }
                break;
            case R.id.menu_rect_screenshot:
                try {
                    Intent screenRecordIntent = new Intent(ShakeService.this, TakeCropScreenshotActivity.class);
                    screenRecordIntent.setAction(TakeCropScreenshotService.ACTION_RECT_SCREENSHOT);
                    screenRecordIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    startActivity(screenRecordIntent);
                } catch (ActivityNotFoundException e) {
                }
                break;
            default:
                break;
        }

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long currentUpdateTime = System.currentTimeMillis();
        long timeInterval = currentUpdateTime - mLastUpdateTime;
        if (timeInterval < UPTATE_INTERVAL_TIME) {
            return;
        }
        mLastUpdateTime = currentUpdateTime;

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        float deltaX = x - mLastX;
        float deltaY = y - mLastY;
        float deltaZ = z - mLastZ;

        mLastX = x;
        mLastY = y;
        mLastZ = z;

        double speed = (Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ
                * deltaZ) / timeInterval) * 100;
        if (speed >= SPEED_SHRESHOLD && !isShowDialog() && !mKeyguardManager.isKeyguardLocked()) {
            mVibrator.vibrate(300);
            showDialog();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void showDialog() {
        if (mFloatMenuDialog == null)
            mFloatMenuDialog = new FloatMenuDialog(this, R.style.Theme_Dialog);
        mFloatMenuDialog.setOnClickListener(this);
        mFloatMenuDialog.show();
    }

    private void dismissDialog() {
        if (isShowDialog())
            mFloatMenuDialog.dismiss();
    }

    public boolean isShowDialog() {
        return mFloatMenuDialog != null && mFloatMenuDialog.isShowing();
    }

    /**
     * 给 API >= 18 的平台上用的灰色保活手段
     */
    public static class GrayInnerService extends Service {

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            startForeground(NOTIFICATION_ID, new Notification());
            stopForeground(true);
            stopSelf();
            return super.onStartCommand(intent, flags, startId);
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

    }
}
