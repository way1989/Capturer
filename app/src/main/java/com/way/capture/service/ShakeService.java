package com.way.capture.service;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.way.capture.BuildConfig;
import com.way.capture.R;
import com.way.capture.core.BaseModule;
import com.way.capture.core.LauncherActivity;
import com.way.capture.core.screenrecord.ScreenRecordModule;
import com.way.capture.core.screenshot.ScreenshotModule;
import com.way.capture.utils.AppUtil;
import com.way.capture.utils.RxSchedulers;
import com.way.capture.utils.RxScreenshot;
import com.way.capture.utils.RxShake;
import com.way.capture.widget.FloatMenuDialog;

import java.lang.ref.WeakReference;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;


/**
 * ChatHead Service
 */
public class ShakeService extends Service {
    private static final String TAG = "ShakeService";

    private static final int NOTIFICATION_ID = 9083150;
    private static final int DELAY_ONCLICK_MESSAGE = 0x120;
    private static final long DELAY_TIME = 400L;
    private static final String EXTRA_RESULT_CODE = "result-code";
    private static final String EXTRA_DATA = "data";
    private Vibrator mVibrator;
    private KeyguardManager mKeyguardManager;
    private FloatMenuDialog mFloatMenuDialog;
    private MainHandler mHandler;
    private CompositeDisposable mSubscriptions;
    private BaseModule mCurrentModule;
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

    public static Intent newIntent(Context context, String action, int resultCode, Intent data) {
        Intent intent = new Intent(context, ShakeService.class);
        intent.setAction(action);
        intent.putExtra(EXTRA_RESULT_CODE, resultCode);
        intent.putExtra(EXTRA_DATA, data);
        return intent;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initData();
    }

    public void initData() {
        mHandler = new MainHandler(Looper.getMainLooper(), this);
        mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(RxScreenshot.DISPLAY_NAME,
                    RxScreenshot.DISPLAY_NAME, NotificationManager.IMPORTANCE_MIN);
            notificationManager.createNotificationChannel(channel);
        }
        //注册Home监听广播
        registerReceiver(mHomeKeyEventReceiver, new IntentFilter(
                Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        startForeground(NOTIFICATION_ID, createNotification());
        mSubscriptions = new CompositeDisposable();
        DisposableObserver<Boolean> observer = new DisposableObserver<Boolean>() {
            @Override
            public void onNext(Boolean shake) {
                if (shake) {
                    showDialog();
                }
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        };
        new RxShake(getApplication()).compose(RxSchedulers.<Boolean>io_main()).subscribe(observer);
        mSubscriptions.add(observer);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String action = intent == null ? null : intent.getAction();
        if (!TextUtils.isEmpty(action)) {
            switch (action) {
                case Action.ACTION_SHOW_MENU:
                    showDialog();
                    break;
                case Action.ACTION_SCREENSHOT:
                case Action.ACTION_FREE_CROP:
                case Action.ACTION_RECT_CROP:
                case Action.ACTION_RECORD:
                    if (mCurrentModule != null && mCurrentModule.isRunning()) {
                        Log.d(TAG, "onStartCommand: module is running...");
                        return START_NOT_STICKY;
                    }
                    if (!AppUtil.hasAvailableSpace()) {
                        Toast.makeText(this, R.string.not_enough_storage, Toast.LENGTH_LONG).show();
                        return START_NOT_STICKY;
                    }
                    int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
                    Intent data = intent.getParcelableExtra(EXTRA_DATA);
                    if (resultCode == 0 || data == null) {
                        Toast.makeText(this, R.string.screenshot_null_toast, Toast.LENGTH_LONG).show();
                        return START_NOT_STICKY;
                    }
                    if (TextUtils.equals(action, Action.ACTION_RECORD)) {
                        mCurrentModule = new ScreenRecordModule();
                    } else {
                        mCurrentModule = new ScreenshotModule();
                    }
                    mCurrentModule.onStart(this, action, resultCode, data);
                    break;
                default:
                    break;

            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //注销Home键监听广播
        unregisterReceiver(mHomeKeyEventReceiver);
        stopForeground(true);
        mSubscriptions.clear();
        if (mCurrentModule != null) {
            mCurrentModule.onDestroy();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private Notification createNotification() {
        final Notification.Builder builder = new Notification.Builder(this);
        builder.setWhen(System.currentTimeMillis());
        builder.setSmallIcon(R.drawable.ic_screenshot_rect);
        builder.setContentTitle(getString(R.string.chathead_content_title));
        builder.setContentText(getString(R.string.chathead_content_text));
        builder.setOngoing(true);
        builder.setPriority(Notification.PRIORITY_MIN);
        builder.setCategory(Notification.CATEGORY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(RxScreenshot.DISPLAY_NAME);
        }

        // PendingIntent
        final Intent notifyIntent = new Intent(this, ShakeService.class);
        notifyIntent.setAction(Action.ACTION_SHOW_MENU);
        PendingIntent notifyPendingIntent = PendingIntent.getService(this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(notifyPendingIntent);

        return builder.build();
    }

    private void showDialog() {
        if (mCurrentModule != null && mCurrentModule.isRunning()) {
            Log.d(TAG, "showDialog: module is running...");
            return;
        }
        if (isShowDialog() || mKeyguardManager.isKeyguardLocked()) {
            Log.d(TAG, "showDialog: menu is showing or Keyguard is Locked...");
            return;
        }
        if (mFloatMenuDialog == null)
            mFloatMenuDialog = new FloatMenuDialog(this, R.style.Theme_Dialog);
        mFloatMenuDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandler.removeMessages(DELAY_ONCLICK_MESSAGE);
                mHandler.sendMessageDelayed(mHandler.obtainMessage(DELAY_ONCLICK_MESSAGE, v.getId(), -1), DELAY_TIME);
            }
        });
        mVibrator.vibrate(300);
        mFloatMenuDialog.show();
    }

    private void dismissDialog() {
        if (isShowDialog())
            mFloatMenuDialog.dismiss();
    }

    public boolean isShowDialog() {
        return mFloatMenuDialog != null && mFloatMenuDialog.isShowing();
    }

    public static final class Action {
        public static final String ACTION_SHOW_MENU = BuildConfig.APPLICATION_ID + ".ACTION.SHOW_MENU";
        public static final String ACTION_SCREENSHOT = BuildConfig.APPLICATION_ID + ".ACTION.SCREENSHOT";
        public static final String ACTION_RECORD = BuildConfig.APPLICATION_ID + ".ACTION.RECORD";
        public static final String ACTION_FREE_CROP = BuildConfig.APPLICATION_ID + ".ACTION.FREE_CROP";
        public static final String ACTION_RECT_CROP = BuildConfig.APPLICATION_ID + ".ACTION.RECT_CROP";
    }

    private static class MainHandler extends Handler {
        private WeakReference<Context> mContextWeakReference;

        MainHandler(Looper looper, Context context) {
            super(looper);
            mContextWeakReference = new WeakReference<>(context);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            final Context context = mContextWeakReference.get();
            switch (msg.what) {
                case DELAY_ONCLICK_MESSAGE:
                    switch (msg.arg1) {
                        case R.id.menu_normal_screenshot:
                            LauncherActivity.startCaptureActivity(context, Action.ACTION_SCREENSHOT);
                            break;
                        case R.id.menu_screenrecord:
                            LauncherActivity.startCaptureActivity(context, Action.ACTION_RECORD);
                            break;
                        case R.id.menu_free_screenshot:
                            LauncherActivity.startCaptureActivity(context, Action.ACTION_FREE_CROP);
                            break;
                        case R.id.menu_rect_screenshot:
                            LauncherActivity.startCaptureActivity(context, Action.ACTION_RECT_CROP);
                            break;
                        default:
                            break;
                    }
                    break;
            }
        }
    }
}
