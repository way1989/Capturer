package com.way.capture.core.screenrecord;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.media.CamcorderProfile;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.way.capture.R;
import com.way.capture.core.DeleteScreenshot;
import com.way.capture.core.screenrecord.record.AudioEncodeConfig;
import com.way.capture.core.screenrecord.record.Notifications;
import com.way.capture.core.screenrecord.record.ScreenRecorder;
import com.way.capture.core.screenrecord.record.VideoEncodeConfig;
import com.way.capture.data.DataInfo;
import com.way.capture.fragment.SettingsFragment;
import com.way.capture.utils.RxBus;
import com.way.capture.utils.RxEvent;
import com.way.capture.utils.RxScreenshot;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.content.Context.MEDIA_PROJECTION_SERVICE;
import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.Context.WINDOW_SERVICE;
import static android.content.Intent.ACTION_SEND;
import static android.content.Intent.ACTION_VIEW;
import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.os.Environment.DIRECTORY_MOVIES;
import static com.way.capture.core.screenshot.ScreenshotModule.SCREENSHOT_NOTIFICATION_ID;

public final class RecordingSession {
    private static final String TAG = "RecordingSession";
    private static final String DISPLAY_NAME = "ScreenRecord";
    private static final String MIME_TYPE = "video/mp4";
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Context mContext;
    private final Listener mListener;
    private final int mResultCode;
    private final Intent mIntentData;
    private final File mOutputDir;
    private final DateFormat mFileFormat = new SimpleDateFormat("'ScreenRecord_'yyyyMMddHHmmss'.mp4'", Locale.getDefault());
    private final NotificationManager mNotificationManager;
    private final WindowManager mWindowManager;
    private final MediaProjectionManager mProjectionManager;
    private OverlayView mOverlayView;
    private boolean mIsRunning;
    private Notifications mNotifications;
    private ScreenRecorder mRecorder;
    private BroadcastReceiver mStopActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Notifications.ACTION_STOP.equals(intent.getAction())) {
                stopRecording();
            }
            Toast.makeText(context, "Recorder stopped...", Toast.LENGTH_LONG).show();
        }
    };

    RecordingSession(Context context, Listener listener, int resultCode, Intent data) {
        this.mContext = context;
        this.mListener = listener;
        this.mResultCode = resultCode;
        this.mIntentData = data;

        File picturesDir = Environment.getExternalStoragePublicDirectory(DIRECTORY_MOVIES);
        mOutputDir = new File(picturesDir, DISPLAY_NAME);
        if (!mOutputDir.exists() || !mOutputDir.isDirectory()) {
            mOutputDir.mkdir();
        }

        mNotificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        mWindowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        mProjectionManager = (MediaProjectionManager) context.getSystemService(MEDIA_PROJECTION_SERVICE);
        mNotifications = new Notifications(context);
    }

    private static RecordingInfo calculateRecordingInfo(int displayWidth, int displayHeight,
                                                        int displayDensity, boolean isLandscapeDevice,
                                                        int cameraWidth, int cameraHeight, int sizePercentage) {
        // Scale the mVirtualDisplay size before any maximum size calculations.
        displayWidth = displayWidth * sizePercentage / 100;
        displayHeight = displayHeight * sizePercentage / 100;

        if (cameraWidth == -1 && cameraHeight == -1) {
            // No cameras. Fall back to the mVirtualDisplay size.
            return new RecordingInfo(displayWidth, displayHeight, displayDensity);
        }

        int frameWidth = isLandscapeDevice ? cameraWidth : cameraHeight;
        int frameHeight = isLandscapeDevice ? cameraHeight : cameraWidth;
        if (frameWidth >= displayWidth && frameHeight >= displayHeight) {
            // Frame can hold the entire mVirtualDisplay. Use exact values.
            return new RecordingInfo(displayWidth, displayHeight, displayDensity);
        }

        // Calculate new width or height to preserve aspect ratio.
        if (isLandscapeDevice) {
            frameWidth = displayWidth * frameHeight / displayHeight;
        } else {
            frameHeight = displayHeight * frameWidth / displayWidth;
        }
        return new RecordingInfo(frameWidth, frameHeight, displayDensity);
    }

    private static Bitmap createSquareBitmap(Bitmap bitmap) {
        int x = 0;
        int y = 0;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width > height) {
            x = (width - height) / 2;
            // noinspection SuspiciousNameCombination
            width = height;
        } else {
            y = (height - width) / 2;
            // noinspection SuspiciousNameCombination
            height = width;
        }
        return Bitmap.createBitmap(bitmap, x, y, width, height, null, true);
    }

    void showOverlay() {
        Log.d(TAG, "Adding overlay view to window.");

        OverlayView.Listener overlayListener = new OverlayView.Listener() {
            @Override
            public void onCancel() {
                cancelOverlay();
            }

            @Override
            public void onStart() {
                startRecording();
            }

            @Override
            public void onStop() {
                stopRecording();
            }
        };
        mOverlayView = OverlayView.create(mContext, overlayListener);
        mWindowManager.addView(mOverlayView, OverlayView.createLayoutParams(mContext));
    }

    private void hideOverlay() {
        if (mOverlayView != null) {
            Log.d(TAG, "Removing overlay view from window.");
            mWindowManager.removeView(mOverlayView);
            mOverlayView = null;
        }
    }

    private void cancelOverlay() {
        hideOverlay();
        mListener.onEnd();
    }

    private RecordingInfo getRecordingInfo() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) mContext.getSystemService(WINDOW_SERVICE);
        if (wm == null) throw new NullPointerException("WindowManager is null...");
        wm.getDefaultDisplay().getRealMetrics(displayMetrics);
        int displayWidth = displayMetrics.widthPixels;
        int displayHeight = displayMetrics.heightPixels;
        int displayDensity = displayMetrics.densityDpi;
        Log.d(TAG, "Display size: " + displayWidth + " x " + displayHeight + " @ " + displayDensity);

        Configuration configuration = mContext.getResources().getConfiguration();
        boolean isLandscape = configuration.orientation == ORIENTATION_LANDSCAPE;
        Log.d(TAG, "Display landscape: " + isLandscape);

        // Get the best camera profile available. We assume MediaRecorder
        // supports the highest.
        CamcorderProfile camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        int cameraWidth = camcorderProfile != null ? camcorderProfile.videoFrameWidth : -1;
        int cameraHeight = camcorderProfile != null ? camcorderProfile.videoFrameHeight : -1;
        Log.d(TAG, "Camera size: " + cameraWidth + " x " + cameraHeight);

        int sizePercentage = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(mContext)
                .getString(SettingsFragment.VIDEO_SIZE_KEY, "100"));
        Log.d(TAG, "Size percentage: " + sizePercentage);

        return calculateRecordingInfo(displayWidth, displayHeight, displayDensity, isLandscape,
                cameraWidth, cameraHeight, sizePercentage);
    }

    private void startRecording() {
        Log.d(TAG, "Starting screen recording...");
        if (!PreferenceManager.getDefaultSharedPreferences(mContext)
                .getBoolean(SettingsFragment.VIDEO_STOP_METHOD_KEY, true)) {
            hideOverlay();
        }

        MediaProjection mediaProjection = mProjectionManager.getMediaProjection(mResultCode, mIntentData);
        VideoEncodeConfig video = createVideoConfig();
        AudioEncodeConfig audio = createAudioConfig(); // audio can be null

        String outputName = mFileFormat.format(new Date());
        File file = new File(mOutputDir, outputName);
        mRecorder = newRecorder(mediaProjection, video, audio, file);
        mRecorder.start();
        mContext.registerReceiver(mStopActionReceiver, new IntentFilter(Notifications.ACTION_STOP));

        mNotificationManager.cancel(SCREENSHOT_NOTIFICATION_ID);
        mIsRunning = true;
        mListener.onStart();
        Log.d(TAG, "Screen recording started.");
    }

    private void stopRecording() {
        Log.d(TAG, "Stopping screen recording...");
        if (!mIsRunning) {
            return;
        }
        mIsRunning = false;
        if (PreferenceManager.getDefaultSharedPreferences(mContext)
                .getBoolean(SettingsFragment.VIDEO_STOP_METHOD_KEY, true)) {
            hideOverlay();
        }

        mNotifications.clear();
        if (mRecorder != null) {
            mRecorder.quit();
        }
        mRecorder = null;
        try {
            mContext.unregisterReceiver(mStopActionReceiver);
        } catch (Exception e) {
            //ignored
        }

        mListener.onStop();

        Log.d(TAG, "Screen recording stopped. Notifying media scanner of new video.");
    }

    private void showNotification(final Uri uri, Bitmap bitmap) {
        Intent viewIntent = new Intent(ACTION_VIEW, uri);
        PendingIntent pendingViewIntent = PendingIntent.getActivity(mContext, 0, viewIntent, 0);

        Intent shareIntent = new Intent(ACTION_SEND);
        shareIntent.setType(MIME_TYPE);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent = Intent.createChooser(shareIntent, null);
        PendingIntent pendingShareIntent = PendingIntent.getActivity(mContext, 0, shareIntent, 0);

        Intent deleteIntent = new Intent(mContext, DeleteScreenshot.class);
        deleteIntent.putExtra(DeleteScreenshot.SCREENSHOT_URI, uri.toString());
        PendingIntent pendingDeleteIntent = PendingIntent.getBroadcast(mContext, 0, deleteIntent, 0);

        CharSequence title = mContext.getText(R.string.notification_captured_title);
        CharSequence subtitle = mContext.getText(R.string.notification_captured_subtitle);
        CharSequence share = mContext.getText(R.string.notification_captured_share);
        CharSequence delete = mContext.getText(R.string.notification_captured_delete);
        Notification.Builder builder = new Notification.Builder(mContext) //
                .setContentTitle(title).setContentText(subtitle).setWhen(System.currentTimeMillis()).setShowWhen(true)
                .setSmallIcon(R.drawable.ic_videocam)
                .setColor(mContext.getResources().getColor(R.color.colorPrimary)).setContentIntent(pendingViewIntent)
                .setAutoCancel(true).addAction(R.drawable.ic_menu_share, share, pendingShareIntent)
                .addAction(R.drawable.ic_menu_delete, delete, pendingDeleteIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(RxScreenshot.DISPLAY_NAME);
        }

        if (bitmap != null) {
            builder.setLargeIcon(createSquareBitmap(bitmap))
                    .setStyle(new Notification.BigPictureStyle() //
                            .setBigContentTitle(title) //
                            .setSummaryText(subtitle) //
                            .bigPicture(bitmap));
        }

        mNotificationManager.notify(SCREENSHOT_NOTIFICATION_ID, builder.build());

        mListener.onEnd();
    }

    void destroy() {
        if (mIsRunning) {
            Log.w(TAG, "Destroyed while mIsRunning!");
            stopRecording();
        }
    }

    private ScreenRecorder newRecorder(MediaProjection mediaProjection, VideoEncodeConfig video,
                                       AudioEncodeConfig audio, final File output) {
        ScreenRecorder r = new ScreenRecorder(video, audio,
                1, mediaProjection, output.getAbsolutePath());
        r.setCallback(new ScreenRecorder.Callback() {
            long startTime = 0;

            @Override
            public void onStop(final Throwable error) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        stopRecording();
                        if (error != null) {
                            output.delete();
                            //toast("Recorder error ! See logcat for more details");
                            error.printStackTrace();
                        } else {
                            MediaScannerConnection.scanFile(mContext, new String[]{output.getAbsolutePath()}, null,
                                    new MediaScannerConnection.OnScanCompletedListener() {
                                        @Override
                                        public void onScanCompleted(String path, final Uri uri) {
                                            Log.d(TAG, "Media scanner completed.");
                                            if (uri != null) {
                                                RxBus.getInstance().post(new RxEvent.NewPathEvent(DataInfo.TYPE_SCREEN_RECORD, output.getAbsolutePath()));
                                                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                                                retriever.setDataSource(mContext, uri);
                                                showNotification(uri, retriever.getFrameAtTime());
                                            }

                                        }
                                    });
                        }
                    }
                });

            }

            @Override
            public void onStart() {
                mNotifications.recording(0);
            }

            @Override
            public void onRecording(long presentationTimeUs) {
                if (startTime <= 0) {
                    startTime = presentationTimeUs;
                }
                final long time = (presentationTimeUs - startTime) / 1000;
                mNotifications.recording(time);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mOverlayView != null)
                            mOverlayView.updateRecordingTime(time);
                    }
                });
            }
        });
        return r;
    }

    private AudioEncodeConfig createAudioConfig() {
        //if (true) return null;
        String codec = "OMX.google.aac.encoder";
        if (codec == null) {
            return null;
        }
        int bitrate = 80 * 1000;
        int samplerate = 44100;
        int channelCount = 1;
        int profile = 1;

        return new AudioEncodeConfig(codec, MediaFormat.MIMETYPE_AUDIO_AAC, bitrate, samplerate, channelCount, profile);
    }

    private VideoEncodeConfig createVideoConfig() {
        final String codec = "OMX.qcom.video.encoder.avc";
        if (codec == null) {
            // no selected codec ??
            return null;
        }
        // video size
        RecordingInfo recordingInfo = getRecordingInfo();
        Log.d(TAG, "Recording: " + recordingInfo.width + " x "
                + recordingInfo.height + " @ " + recordingInfo.density);
        int width = recordingInfo.width;
        int height = recordingInfo.height;
        int framerate = 30;
        int iframe = 30;
        int bitrate = 8 * 1000 * 1000;
        MediaCodecInfo.CodecProfileLevel profileLevel = null;
        return new VideoEncodeConfig(width, height, bitrate,
                framerate, iframe, codec, MediaFormat.MIMETYPE_VIDEO_AVC, profileLevel);
    }

    public interface Listener {
        /**
         * Invoked immediately prior to the start of recording.
         */
        void onStart();

        /**
         * Invoked immediately after the end of recording.
         */
        void onStop();

        /**
         * Invoked after all work for this session has completed.
         */
        void onEnd();
    }

    private static final class RecordingInfo {
        final int width;
        final int height;
        final int density;

        RecordingInfo(int width, int height, int density) {
            this.width = width;
            this.height = height;
            this.density = density;
        }
    }

}
