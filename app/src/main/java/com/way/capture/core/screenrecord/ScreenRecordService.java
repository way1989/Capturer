package com.way.capture.core.screenrecord;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.StatFs;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

import com.way.capture.R;
import com.way.capture.fragment.SettingsFragment;
import com.way.capture.utils.AppUtils;

import java.util.Timer;
import java.util.TimerTask;

public final class ScreenRecordService extends Service {
    public static final String ACTION_STOP_SCREENRECORD = "com.way.ACTION_STOP_SCREENRECORD";
    private static final String EXTRA_RESULT_CODE = "result-code";
    private static final String EXTRA_DATA = "data";
    private static final int NOTIFICATION_ID = 99118822;
    private static final String SHOW_TOUCHES = "show_touches";
    private boolean mIsShowTouchesr;
    private ContentResolver mContentResolver;
    private long mStartTime;
    private Notification.Builder mBuilder;
    private final RecordingSession.Listener mListener = new RecordingSession.Listener() {
        private int showTouch = 0;

        @TargetApi(Build.VERSION_CODES.M)
        @Override
        public void onStart() {
            if (mIsShowTouchesr) {
                showTouch = Settings.System.getInt(mContentResolver, SHOW_TOUCHES, 0);
                if (!AppUtils.isMarshmallow())
                    Settings.System.putInt(mContentResolver, SHOW_TOUCHES, 1);
            }
            if (!PreferenceManager.getDefaultSharedPreferences(ScreenRecordService.this)
                    .getBoolean(SettingsFragment.VIDEO_STOP_METHOD_KEY, true)) {
                mStartTime = SystemClock.elapsedRealtime();
                mBuilder = createNotificationBuilder();
                new Timer().scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        updateNotification(ScreenRecordService.this);
                    }
                }, 100, 1000);
            } else {
                Context context = getApplicationContext();
                String title = context.getString(R.string.notification_recording_title);
                //String subtitle = context.getString(R.string.notification_recording_subtitle);
                Notification.Builder builder = new Notification.Builder(context) //
                        .setContentTitle(title)/*.setContentText(subtitle)*/.setSmallIcon(R.drawable.ic_videocam)
                        .setColor(context.getResources().getColor(R.color.colorPrimary)).setAutoCancel(true)
                        .setPriority(Notification.PRIORITY_MIN);
                Intent stopIntent = new Intent("com.way.stop");
                stopIntent.putExtra("id", NOTIFICATION_ID);
                builder.addAction(R.drawable.ic_clear, context.getResources()
                        .getString(R.string.share), PendingIntent.getBroadcast(context, 0, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT));
                Notification notification = builder.build();
                Log.d("way", "Moving service into the foreground with recording notification.");
                startForeground(NOTIFICATION_ID, notification);
            }
        }

        @Override
        public void onStop() {
            if (mIsShowTouchesr) {
                if (!AppUtils.isMarshmallow())
                    Settings.System.putInt(mContentResolver, SHOW_TOUCHES, showTouch);
            }
            stopForeground(true /* remove notification */);
        }

        @Override
        public void onEnd() {
            Log.d("way", "Shutting down.");
            stopSelf();
        }
    };
    private SharedPreferences mSharedPreferences;
    private boolean mIsRunning;
    private RecordingSession mRecordingSession;

    public static Intent newIntent(Context context, int resultCode, Intent data) {
        Intent intent = new Intent(context, ScreenRecordService.class);
        intent.putExtra(EXTRA_RESULT_CODE, resultCode);
        intent.putExtra(EXTRA_DATA, data);
        return intent;
    }

    private boolean hasAvailableSpace() {
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long bytesAvailable = stat.getBlockSizeLong() * stat.getAvailableBlocksLong();
        long megAvailable = bytesAvailable / 1048576;
        return megAvailable >= 100;
    }

    private Notification.Builder createNotificationBuilder() {
        Notification.Builder builder = new Notification.Builder(this)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_videocam)
                .setContentTitle(getString(R.string.notification_recording_title));
        Intent stopRecording = new Intent(ACTION_STOP_SCREENRECORD);
        stopRecording.putExtra("id", NOTIFICATION_ID);
        builder.addAction(R.drawable.ic_stop, getString(R.string.stop),
                PendingIntent.getBroadcast(this, 0, stopRecording, PendingIntent.FLAG_CANCEL_CURRENT));
        return builder;
    }

    public void updateNotification(Context context) {
        long timeElapsed = SystemClock.elapsedRealtime() - mStartTime;
        mBuilder.setContentText(getString(R.string.video_length,
                DateUtils.formatElapsedTime(timeElapsed / 1000)));
        startForeground(NOTIFICATION_ID, mBuilder.build());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContentResolver = getContentResolver();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mIsRunning) {
            Log.d("way", "Already running! Ignoring...");
            return START_NOT_STICKY;
        }
        Log.d("way", "Starting up!");
        mIsRunning = true;

        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!hasAvailableSpace()) {
            Toast.makeText(this, R.string.not_enough_storage, Toast.LENGTH_LONG).show();
            stopSelf();
            return START_NOT_STICKY;
        }

        int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
        Intent data = intent.getParcelableExtra(EXTRA_DATA);
        if (resultCode == 0 || data == null) {
            throw new IllegalStateException("Result code or data missing.");
        }
        mIsShowTouchesr = mSharedPreferences.getBoolean(SettingsFragment.SHOW_TOUCHES_KEY, true);
        mRecordingSession = new RecordingSession(this, mListener, resultCode, data);
        mRecordingSession.showOverlay();

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        mRecordingSession.destroy();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new AssertionError("Not supported.");
    }
}
