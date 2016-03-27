package com.way.captain.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.way.captain.R;
import com.way.captain.activity.MainActivity;
import com.way.captain.floatview.FloatingView;
import com.way.captain.floatview.FloatingViewListener;
import com.way.captain.floatview.FloatingViewManager;
import com.way.captain.fragment.SettingsFragment;
import com.way.screenshot.TakeScreenshotActivity;
import com.way.screenshot.TakeScreenshotService;
import com.way.telecine.TelecineShortcutLaunchActivity;


/**
 * ChatHead Service
 */
public class ChatHeadService extends Service implements FloatingViewListener, View.OnClickListener,
        View.OnLongClickListener, SharedPreferences.OnSharedPreferenceChangeListener, SensorEventListener {

    private static final String TAG = "ChatHeadService";
    private static final int MESSAGE_CENTER_SCREENSHOT = 0x000;
    private static final int MESSAGE_NORMAL_SCREENSHOT = 0x001;
    private static final int MESSAGE_RECORD_SCREENSHOT = 0x002;
    private static final int MESSAGE_LONG_SCREENSHOT = 0x003;
    private static final int MESSAGE_RECT_SCREENSHOT = 0x004;
    private static final int MESSAGE_FREE_SCREENSHOT = 0x005;
    private static final long DELAY_TIME = 300L;

    /**
     * 通知ID
     */
    private static final int NOTIFICATION_ID = 9083150;


    /**
     * FloatingViewManager
     */
    //private FloatingViewManager mFloatingViewManager;
    private static final int SPEED_SHRESHOLD = 45;// 这个值越大需要越大的力气来摇晃手机
    private static final int UPTATE_INTERVAL_TIME = 50;
    /**
     * Vibrator
     */
    private Vibrator mVibrator;
    private WindowManager mWindowManager;
    private FloatingView mFloatingView;
    private ImageView mIconView;
    private boolean mIsRunning;
    private SharedPreferences mPreferences;
    private SensorManager sensorManager = null;
    private Sensor sensor;
    private Vibrator vibrator = null;
    private boolean isRequest = false;
    private float lastX;
    private float lastY;
    private float lastZ;
    private long lastUpdateTime;
    private FloatMenuDialog mFloatMenuDialog;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MESSAGE_CENTER_SCREENSHOT:
                    Intent i = new Intent(ChatHeadService.this, MainActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    startActivity(i);
                    break;
                case MESSAGE_NORMAL_SCREENSHOT:
                    try {
                        Intent screenRecordIntent = new Intent(ChatHeadService.this, TakeScreenshotActivity.class);
                        screenRecordIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                        startActivity(screenRecordIntent);
                    } catch (ActivityNotFoundException e) {
                    }
                    break;
                case MESSAGE_RECORD_SCREENSHOT:
                    try {
                        Intent screenRecordIntent = new Intent(ChatHeadService.this, TelecineShortcutLaunchActivity.class);

                        screenRecordIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                        startActivity(screenRecordIntent);
                    } catch (ActivityNotFoundException e) {
                    }
                    break;
                case MESSAGE_LONG_SCREENSHOT:
                    try {
                        Intent screenRecordIntent = new Intent(ChatHeadService.this, TakeScreenshotActivity.class);
                        screenRecordIntent.setAction(TakeScreenshotService.ACTION_LONG_SCREENSHOT);
                        screenRecordIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                        startActivity(screenRecordIntent);
                    } catch (ActivityNotFoundException e) {
                    }
                    break;
                case MESSAGE_RECT_SCREENSHOT:
                    break;
                case MESSAGE_FREE_SCREENSHOT:
                    break;
                default:
                    break;
            }
        }
    };
    @Override
    public void onCreate() {
        super.onCreate();
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mPreferences.registerOnSharedPreferenceChangeListener(this);

        initData();
        if (sensorManager != null) {
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        if (sensor != null) {
            sensorManager.registerListener(this, sensor,
                    SensorManager.SENSOR_DELAY_GAME);
        }
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

        final DisplayMetrics metrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(metrics);
        //final LayoutInflater inflater = LayoutInflater.from(this);
        //final ImageView iconView = (ImageView) inflater.inflate(R.layout.widget_chathead, null, false);
        mIconView = new ImageView(this);
        mIconView.setId(R.id.fab);
        mIconView.setImageResource(R.drawable.theme_captain);
        mIconView.setOnClickListener(this);
        mIconView.setOnLongClickListener(this);


//        mFloatingViewManager = new FloatingViewManager(this, this);
//        mFloatingViewManager.setFixedTrashIconImage(R.drawable.ic_trash_fixed);
//        mFloatingViewManager.setActionTrashIconImage(R.drawable.ic_trash_action);
//        final FloatingViewManager.Options options = new FloatingViewManager.Options();
//        options.shape = FloatingViewManager.SHAPE_CIRCLE;
//        options.overMargin = (int) (16 * metrics.density);
//        mFloatingViewManager.addViewToWindow(iconView, options);

//        mFloatingView = new FloatingView(this);
//        mFloatingView.setShape(FloatingViewManager.SHAPE_CIRCLE);
//        mFloatingView.setOverMargin((int) (16 * metrics.density));
//        mFloatingView.setInitCoords(metrics.widthPixels, metrics.heightPixels / 2);
//
//        mFloatingView.addView(mIconView);
//        mWindowManager.addView(mFloatingView, mFloatingView.getWindowLayoutParams());

        // 常駐起動
        startForeground(NOTIFICATION_ID, createNotification());

        return START_REDELIVER_INTENT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        destroy();
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFinishFloatingView() {
        stopSelf();
    }

    /**
     * View
     */
    private void destroy() {
        /*if (mFloatingViewManager != null) {
            mFloatingViewManager.removeAllViewToWindow();
            mFloatingViewManager = null;
        }*/
        mPreferences.unregisterOnSharedPreferenceChangeListener(this);
        if (mFloatingView != null) {
            mIsRunning = false;
            mWindowManager.removeViewImmediate(mFloatingView);
            mFloatingView = null;
        }

    }

    /**
     * 通知を表示します。
     */
    private Notification createNotification() {
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setWhen(System.currentTimeMillis());
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle(getString(R.string.chathead_content_title));
        builder.setContentText(getString(R.string.chathead_content_text));
        builder.setOngoing(true);
        builder.setPriority(NotificationCompat.PRIORITY_MIN);
        builder.setCategory(NotificationCompat.CATEGORY_SERVICE);

        // PendingIntent作成
        final Intent notifyIntent = new Intent(this, MainActivity.class);
        PendingIntent notifyPendingIntent = PendingIntent.getActivity(this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(notifyPendingIntent);

        return builder.build();
    }

    @Override
    public void onClick(final View v) {
        mVibrator.vibrate(30);
        if (mFloatMenuDialog != null && mFloatMenuDialog.isShowing()) {
            mFloatMenuDialog.cancel();
        }
        switch (v.getId()) {
            case R.id.menu_center:
                mHandler.removeMessages(MESSAGE_CENTER_SCREENSHOT);
                mHandler.sendEmptyMessageDelayed(MESSAGE_CENTER_SCREENSHOT, DELAY_TIME);
                break;
            case R.id.menu_one:
                mHandler.removeMessages(MESSAGE_NORMAL_SCREENSHOT);
                mHandler.sendEmptyMessageDelayed(MESSAGE_NORMAL_SCREENSHOT, DELAY_TIME);
                break;
            case R.id.menu_two:
                mHandler.removeMessages(MESSAGE_RECORD_SCREENSHOT);
                mHandler.sendEmptyMessageDelayed(MESSAGE_RECORD_SCREENSHOT, DELAY_TIME);

                break;
            case R.id.menu_three:
                mHandler.removeMessages(MESSAGE_LONG_SCREENSHOT);
                mHandler.sendEmptyMessageDelayed(MESSAGE_LONG_SCREENSHOT, DELAY_TIME);
                break;
            case R.id.menu_four:
                break;
            case R.id.menu_five:
                break;
            default:
//                v.animate().alpha(0).withEndAction(new Runnable() {
//                    @Override
//                    public void run() {
//                        v.setVisibility(View.GONE);
//                        PreferenceManager.getDefaultSharedPreferences(ChatHeadService.this).edit()
//                                .putBoolean(SettingsFragment.HIDE_FLOATVIEW_KEY, true).apply();
//                    }
//                });
//                Intent i = new Intent(ChatHeadService.this, TelecineShortcutLaunchActivity.class);
//                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(i);
                break;
        }

    }

    @Override
    public boolean onLongClick(final View v) {
        mVibrator.vibrate(40);
        v.animate().alpha(0f).withEndAction(new Runnable() {
            @Override
            public void run() {
//                mFloatingView.setDraggable(false);
                v.setVisibility(View.GONE);
                PreferenceManager.getDefaultSharedPreferences(ChatHeadService.this).edit()
                        .putBoolean(SettingsFragment.HIDE_FLOATVIEW_KEY, true).apply();
                Intent i = new Intent(ChatHeadService.this, TakeScreenshotActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            }
        });

        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.i(TAG, "onSharedPreferenceChanged... key = " + key);
        if (key.equals(SettingsFragment.HIDE_FLOATVIEW_KEY)) {
            if (!sharedPreferences.getBoolean(SettingsFragment.HIDE_FLOATVIEW_KEY, false)) {
                mIconView.setVisibility(View.VISIBLE);
                mIconView.animate().alpha(1);
//                mFloatingView.setDraggable(true);
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long currentUpdateTime = System.currentTimeMillis();
        long timeInterval = currentUpdateTime - lastUpdateTime;
        if (timeInterval < UPTATE_INTERVAL_TIME) {
            return;
        }
        lastUpdateTime = currentUpdateTime;

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        float deltaX = x - lastX;
        float deltaY = y - lastY;
        float deltaZ = z - lastZ;

        lastX = x;
        lastY = y;
        lastZ = z;

        double speed = (Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ
                * deltaZ) / timeInterval) * 100;
        if (speed >= SPEED_SHRESHOLD && !isRequest) {
            vibrator.vibrate(300);
            onShake();
        }
    }

    private void onShake() {
        isRequest = true;
        showDialog();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void initData() {
        sensorManager = (SensorManager) this
                .getSystemService(Context.SENSOR_SERVICE);
        vibrator = (Vibrator) this.getSystemService(Service.VIBRATOR_SERVICE);
    }

    private void showDialog() {
        if (mFloatMenuDialog == null)
            mFloatMenuDialog = new FloatMenuDialog(this, R.style.Theme_Dialog);
        mFloatMenuDialog.setOnClickListener(this);
        mFloatMenuDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                isRequest = false;
            }
        });
        mFloatMenuDialog.show();
    }

}
