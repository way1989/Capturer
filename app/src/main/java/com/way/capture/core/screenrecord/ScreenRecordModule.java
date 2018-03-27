package com.way.capture.core.screenrecord;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.format.DateUtils;
import android.util.Log;

import com.way.capture.R;
import com.way.capture.core.BaseModule;
import com.way.capture.fragment.SettingsFragment;
import com.way.capture.service.ModuleService;
import com.way.capture.utils.AppUtils;
import com.way.capture.utils.RxScreenshot;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by android on 16-8-22.
 */
public class ScreenRecordModule implements BaseModule {
    public static final String ACTION_STOP_SCREENRECORD = "com.way.ACTION_STOP_SCREENRECORD";
    private static final String TAG = "ScreenRecordModule";
    private static final int NOTIFICATION_ID = 99118822;
    private static final String SHOW_TOUCHES = "show_touches";
    private boolean mIsShowTouchesr;
    private ContentResolver mContentResolver;
    private long mStartTime;
    private Notification.Builder mBuilder;
    private RecordingSession mRecordingSession;
    private Context mContext;
    private final RecordingSession.Listener mListener = new RecordingSession.Listener() {
        private int showTouch = 0;

        @Override
        public void onStart() {
            final ModuleService context = (ModuleService) mContext;

            if (mIsShowTouchesr) {
                showTouch = Settings.System.getInt(mContentResolver, SHOW_TOUCHES, 0);
                if (!AppUtils.isMarshmallow())
                    Settings.System.putInt(mContentResolver, SHOW_TOUCHES, 1);
            }
            if (!PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean(SettingsFragment.VIDEO_STOP_METHOD_KEY, true)) {
                mStartTime = SystemClock.elapsedRealtime();
                mBuilder = createNotificationBuilder();
                new Timer().scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        updateNotification(context);
                    }
                }, 100, 1000);
            } else {
                String title = context.getString(R.string.notification_recording_title);
                //String subtitle = context.getString(R.string.notification_recording_subtitle);
                Notification.Builder builder = new Notification.Builder(context) //
                        .setContentTitle(title)/*.setContentText(subtitle)*/.setSmallIcon(R.drawable.ic_videocam)
                        .setColor(context.getResources().getColor(R.color.colorPrimary)).setAutoCancel(true)
                        .setPriority(Notification.PRIORITY_MIN);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    builder.setChannelId(RxScreenshot.DISPLAY_NAME);
                }
                Intent stopIntent = new Intent("com.way.stop");
                stopIntent.putExtra("id", NOTIFICATION_ID);
                builder.addAction(R.drawable.ic_clear, context.getResources()
                        .getString(R.string.share), PendingIntent.getBroadcast(context, 0, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT));
                Notification notification = builder.build();
                Log.d(TAG, "Moving service into the foreground with recording notification.");
                context.startForeground(NOTIFICATION_ID, notification);
            }
        }

        @Override
        public void onStop() {
            if (mIsShowTouchesr) {
                if (!AppUtils.isMarshmallow())
                    Settings.System.putInt(mContentResolver, SHOW_TOUCHES, showTouch);
            }
            ModuleService context = (ModuleService) mContext;
            context.stopForeground(true /* remove notification */);
        }

        @Override
        public void onEnd() {
            Log.d(TAG, "Shutting down.");
            ModuleService context = (ModuleService) mContext;
            context.stopSelf();
        }
    };

    private Notification.Builder createNotificationBuilder() {
        Notification.Builder builder = new Notification.Builder(mContext)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_videocam)
                .setContentTitle(mContext.getString(R.string.notification_recording_title));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(RxScreenshot.DISPLAY_NAME);
        }
        Intent stopRecording = new Intent(ACTION_STOP_SCREENRECORD);
        stopRecording.putExtra("id", NOTIFICATION_ID);
        builder.addAction(R.drawable.ic_stop, mContext.getString(R.string.stop),
                PendingIntent.getBroadcast(mContext, 0, stopRecording, PendingIntent.FLAG_CANCEL_CURRENT));
        return builder;
    }

    public void updateNotification(ModuleService context) {
        long timeElapsed = SystemClock.elapsedRealtime() - mStartTime;
        mBuilder.setContentText(mContext.getString(R.string.video_length,
                DateUtils.formatElapsedTime(timeElapsed / 1000)));
        context.startForeground(NOTIFICATION_ID, mBuilder.build());
    }

    @Override
    public void onStart(Context context, String action, int resultCode, Intent data) {
        mContext = context;
        mContentResolver = mContext.getContentResolver();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        mIsShowTouchesr = sharedPreferences.getBoolean(SettingsFragment.SHOW_TOUCHES_KEY, true);
        mRecordingSession = new RecordingSession(mContext, mListener, resultCode, data);
        mRecordingSession.showOverlay();
    }

    @Override
    public void onDestroy() {
        mRecordingSession.destroy();
    }
}
