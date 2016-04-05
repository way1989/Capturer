package com.way.captain.service;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.view.View;

import com.way.captain.R;
import com.way.captain.activity.MainActivity;
import com.way.captain.screenrecord.ScreenRecordShortcutLaunchActivity;
import com.way.captain.screenshot.TakeScreenshotActivity;
import com.way.captain.screenshot.TakeScreenshotService;
import com.way.captain.screenshot.crop.TakeCropScreenshotService;
import com.way.captain.widget.FloatMenuDialog;


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


    @Override
    public void onCreate() {
        super.onCreate();
        initData();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mIsRunning) {
            return START_STICKY;
        }
        mIsRunning = true;
        startForeground(NOTIFICATION_ID, createNotification());
        return START_REDELIVER_INTENT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        mIsRunning = false;
        mSensorManager.unregisterListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    /**
     * 通知を表示します。
     */
    private Notification createNotification() {
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setWhen(System.currentTimeMillis());
        builder.setSmallIcon(R.drawable.theme_captain);
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
        mVibrator.vibrate(30);
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
                    Intent screenRecordIntent = new Intent(ShakeService.this, com.way.captain.screenshot.crop.TakeScreenshotActivity.class);
                    screenRecordIntent.setAction(TakeCropScreenshotService.ACTION_FREE_SCREENSHOT);
                    screenRecordIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    startActivity(screenRecordIntent);
                } catch (ActivityNotFoundException e) {
                }
                break;
            case R.id.menu_rect_screenshot:
                try {
                    Intent screenRecordIntent = new Intent(ShakeService.this, com.way.captain.screenshot.crop.TakeScreenshotActivity.class);
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
    }

    private void showDialog() {
        if (mFloatMenuDialog == null)
            mFloatMenuDialog = new FloatMenuDialog(this, R.style.Theme_Dialog);
        mFloatMenuDialog.setOnClickListener(this);
        mFloatMenuDialog.show();
    }

    public boolean isShowDialog() {
        return mFloatMenuDialog != null && mFloatMenuDialog.isShowing();
    }
}
