package com.way.capture.core.screenrecord;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.hardware.display.VirtualDisplay;
import android.media.CamcorderProfile;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import com.way.capture.R;
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
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.content.Context.MEDIA_PROJECTION_SERVICE;
import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.Context.WINDOW_SERVICE;
import static android.content.Intent.ACTION_SEND;
import static android.content.Intent.ACTION_VIEW;
import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION;
import static android.media.MediaRecorder.OutputFormat.MPEG_4;
import static android.media.MediaRecorder.VideoEncoder.H264;
import static android.media.MediaRecorder.VideoSource.SURFACE;
import static android.os.Environment.DIRECTORY_MOVIES;
import static com.thefinestartist.utils.content.ContextUtil.sendBroadcast;

public final class RecordingSession implements MediaRecorder.OnErrorListener, MediaRecorder.OnInfoListener {
    private static final int NOTIFICATION_ID = 789;
    private static final String TAG = "RecordingSession";
    private static final String DISPLAY_NAME = "ScreenRecord";
    private static final String MIME_TYPE = "video/mp4";
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Context mContext;
    private final Listener mListener;
    private final int mResultCode;
    private final Intent mIntentData;
    private final File mOutputDir;
    private final DateFormat mFileFormat = new SimpleDateFormat("'ScreenRecord_'yyyy-MM-dd-HH-mm-ss'.mp4'");
    private final NotificationManager mNotificationManager;
    private final WindowManager mWindowManager;
    private final MediaProjectionManager mProjectionManager;
    private OverlayView mOverlayView;
    private MediaProjection mMediaProjection;
    private boolean mIsRunning;
    private BroadcastReceiver mStopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopRecording();
        }
    };


    RecordingSession(Context context, Listener listener, int resultCode, Intent data) {
        this.mContext = context;
        this.mListener = listener;
        this.mResultCode = resultCode;
        this.mIntentData = data;

        File picturesDir = Environment.getExternalStoragePublicDirectory(DIRECTORY_MOVIES);
        mOutputDir = new File(picturesDir, DISPLAY_NAME);

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
            IntentFilter intentFilter = new IntentFilter(ScreenRecordService.ACTION_STOP_SCREENRECORD);
            mContext.registerReceiver(mStopReceiver, intentFilter);
        }

        if (!mOutputDir.mkdirs()) {
            Log.e(TAG, "Unable to create output directory '" + mOutputDir.getAbsolutePath());
            // We're probably about to crash, but at least the log will indicate as to why.
        }
        mMediaProjection = mProjectionManager.getMediaProjection(mResultCode, mIntentData);
        VideoEncodeConfig video = createVideoConfig();
        AudioEncodeConfig audio = createAudioConfig(); // audio can be null

        String outputName = mFileFormat.format(new Date());
        File file = new File(mOutputDir, outputName);
        mRecorder = newRecorder(mMediaProjection, video, audio, file);
        mRecorder.start();
        mContext.registerReceiver(mStopActionReceiver, new IntentFilter(Notifications.ACTION_STOP));

        /*RecordingInfo recordingInfo = getRecordingInfo();
        Log.d(TAG, "Recording: " + recordingInfo.width + " x "
                + recordingInfo.height + " @ " + recordingInfo.density);

        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setVideoSource(SURFACE);
        mMediaRecorder.setOutputFormat(MPEG_4);
        mMediaRecorder.setVideoFrameRate(25);
        mMediaRecorder.setVideoEncoder(H264);
        mMediaRecorder.setVideoSize(recordingInfo.width, recordingInfo.height);
        mMediaRecorder.setVideoEncodingBitRate(8 * 1000 * 1000);

        String outputName = mFileFormat.format(new Date());
        mOutputFile = new File(mOutputDir, outputName).getAbsolutePath();
        Log.i(TAG, "Output file '" + mOutputFile);
        mMediaRecorder.setOutputFile(mOutputFile);

        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            throw new RuntimeException("Unable to prepare MediaRecorder.", e);
        }
        mMediaRecorder.setOnErrorListener(this);
        mMediaRecorder.setOnInfoListener(this);
        mMediaProjection = mProjectionManager.getMediaProjection(mResultCode, mIntentData);

        Surface surface = mMediaRecorder.getSurface();
        mVirtualDisplay = mMediaProjection.createVirtualDisplay(DISPLAY_NAME, recordingInfo.width, recordingInfo.height,
                recordingInfo.density, VIRTUAL_DISPLAY_FLAG_PRESENTATION, surface, null, null);

        mMediaRecorder.start();*/
        mIsRunning = true;
        mListener.onStart();

        Log.d(TAG, "Screen recording started.");

    }

    private void stopRecording() {
        Log.d(TAG, "Stopping screen recording...");
        if (!mIsRunning) {
            throw new IllegalStateException("Not mIsRunning.");
        }
        boolean fail = false;
        mIsRunning = false;
        if (!PreferenceManager.getDefaultSharedPreferences(mContext)
                .getBoolean(SettingsFragment.VIDEO_STOP_METHOD_KEY, true)) {
            mContext.unregisterReceiver(mStopReceiver);
        } else {
            hideOverlay();
        }
        mMediaProjection.stop();
        stopRecorder();
/*        // Stop the mMediaProjection in order to flush everything to the mMediaRecorder.
        mMediaProjection.stop();

        //设置后不会崩
        mMediaRecorder.setOnErrorListener(null);
        mMediaRecorder.setOnInfoListener(null);
        // Stop the mMediaRecorder which writes the contents to the file.
        try {
            mMediaRecorder.stop();
        } catch (Exception e) {
            Toast.makeText(mContext, "stop record failed...", Toast.LENGTH_SHORT).show();
            fail = true;
        }
        if (fail && mOutputFile != null) {
            deleteVideoFile(mOutputFile);
        }

        mMediaRecorder.release();
        mVirtualDisplay.release();*/

        mListener.onStop();

        Log.d(TAG, "Screen recording stopped. Notifying media scanner of new video.");
       /* if (!fail)
            MediaScannerConnection.scanFile(mContext, new String[]{mOutputFile}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        @Override
                        public void onScanCompleted(String path, final Uri uri) {
                            Log.d(TAG, "Media scanner completed.");
                            if (uri != null)
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        RxBus.getInstance().post(new RxEvent.NewPathEvent(DataInfo.TYPE_SCREEN_RECORD, mOutputFile));
                                        showNotification(uri, null);
                                    }
                                });
                        }
                    });*/
    }

    private void deleteVideoFile(String fileName) {
        Log.v(TAG, "Deleting video " + fileName);
        File f = new File(fileName);
        if (!f.delete()) {
            Log.v(TAG, "Could not delete " + fileName);
        }
    }

    private void showNotification(final Uri uri, Bitmap bitmap) {
        Intent viewIntent = new Intent(ACTION_VIEW, uri);
        PendingIntent pendingViewIntent = PendingIntent.getActivity(mContext, 0, viewIntent, 0);

        Intent shareIntent = new Intent(ACTION_SEND);
        shareIntent.setType(MIME_TYPE);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent = Intent.createChooser(shareIntent, null);
        PendingIntent pendingShareIntent = PendingIntent.getActivity(mContext, 0, shareIntent, 0);

        Intent deleteIntent = new Intent(mContext, DeleteRecordingBroadcastReceiver.class);
        deleteIntent.setData(uri);
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

        mNotificationManager.notify(NOTIFICATION_ID, builder.build());

        if (bitmap != null) {
            mListener.onEnd();
            return;
        }

        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... none) {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(mContext, uri);
                return retriever.getFrameAtTime();
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap != null) {
                    showNotification(uri, bitmap);
                } else {
                    mListener.onEnd();
                }
            }
        }.execute();
    }

    void destroy() {
        if (mIsRunning) {
            Log.w(TAG, "Destroyed while mIsRunning!");
            stopRecording();
        }
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        Log.e(TAG, "MediaRecorder error. what=" + what + ". extra=" + extra);
        if (what == MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN) {
            // We may have run out of space on the sdcard.
            if (mIsRunning)
                stopRecording();
        }
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
            Log.v(TAG, "media recoder reached max duration");
            if (mIsRunning)
                stopRecording();
        } else if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
            Log.v(TAG, "media recoder reached max size");
            if (mIsRunning)
                stopRecording();
            // Show the toast.
            Toast.makeText(mContext.getApplicationContext(), R.string.video_reach_size_limit,
                    Toast.LENGTH_LONG).show();
        }
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

    public static final class DeleteRecordingBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            NotificationManager notificationManager = (NotificationManager) context
                    .getSystemService(NOTIFICATION_SERVICE);
            notificationManager.cancel(NOTIFICATION_ID);
            final Uri uri = intent.getData();
            final ContentResolver contentResolver = context.getContentResolver();
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... none) {
                    int rowsDeleted = contentResolver.delete(uri, null, null);
                    if (rowsDeleted == 1) {
                        Log.i(TAG, "Deleted recording.");
                    } else {
                        Log.e(TAG, "Error deleting recording.");
                    }
                    return null;
                }
            }.execute();
        }
    }


    private Notifications mNotifications;
    private ScreenRecorder newRecorder(MediaProjection mediaProjection, VideoEncodeConfig video,
                                       AudioEncodeConfig audio, final File output) {
        ScreenRecorder r = new ScreenRecorder(video, audio,
                1, mediaProjection, output.getAbsolutePath());
        r.setCallback(new ScreenRecorder.Callback() {
            long startTime = 0;

            @Override
            public void onStop(Throwable error) {
                //runOnUiThread(() -> stopRecorder());
                if (error != null) {
                    //toast("Recorder error ! See logcat for more details");
                    error.printStackTrace();
                    output.delete();
                } else {
                    /*Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                            .addCategory(Intent.CATEGORY_DEFAULT)
                            .setData(Uri.fromFile(output));
                    sendBroadcast(intent);*/
                    MediaScannerConnection.scanFile(mContext, new String[]{output.getAbsolutePath()}, null,
                            new MediaScannerConnection.OnScanCompletedListener() {
                                @Override
                                public void onScanCompleted(String path, final Uri uri) {
                                    Log.d(TAG, "Media scanner completed.");
                                    if (uri != null)
                                        mHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                RxBus.getInstance().post(new RxEvent.NewPathEvent(DataInfo.TYPE_SCREEN_RECORD, output.getAbsolutePath()));
                                                showNotification(uri, null);
                                            }
                                        });
                                }
                            });
                }
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
                long time = (presentationTimeUs - startTime) / 1000;
                mNotifications.recording(time);
            }
        });
        return r;
    }

    private ScreenRecorder mRecorder;
    private AudioEncodeConfig createAudioConfig() {
        //if (!mAudioToggle.isChecked()) return null;
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
        final String codec = "OMX.google.h264.encoder";
        if (codec == null) {
            // no selected codec ??
            return null;
        }
        // video size
        RecordingInfo recordingInfo = getRecordingInfo();
        Log.d(TAG, "Recording: " + recordingInfo.width + " x "
                + recordingInfo.height + " @ " + recordingInfo.density);
        int[] selectedWithHeight = new int[]{recordingInfo.width, recordingInfo.height};
        Configuration configuration = mContext.getResources().getConfiguration();
        boolean isLandscape = configuration.orientation == ORIENTATION_LANDSCAPE;
        int width = selectedWithHeight[isLandscape ? 0 : 1];
        int height = selectedWithHeight[isLandscape ? 1 : 0];
        int framerate = 24;
        int iframe = 5;
        int bitrate = 800 * 1000;
        MediaCodecInfo.CodecProfileLevel profileLevel = null;
        return new VideoEncodeConfig(width, height, bitrate,
                framerate, iframe, codec, MediaFormat.MIMETYPE_VIDEO_AVC, profileLevel);
    }
    private void stopRecorder() {
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
    }
    private BroadcastReceiver mStopActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            File file = new File(mRecorder.getSavedPath());
            if (Notifications.ACTION_STOP.equals(intent.getAction())) {
                stopRecorder();
            }
            Toast.makeText(context, "Recorder stopped!\n Saved file " + file, Toast.LENGTH_LONG).show();
            StrictMode.VmPolicy vmPolicy = StrictMode.getVmPolicy();
            try {
                // disable detecting FileUriExposure on public file
                StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());
                viewResult(file);
            } finally {
                StrictMode.setVmPolicy(vmPolicy);
            }
        }

        private void viewResult(File file) {
            Intent view = new Intent(Intent.ACTION_VIEW);
            view.addCategory(Intent.CATEGORY_DEFAULT);
            view.setDataAndType(Uri.fromFile(file), MediaFormat.MIMETYPE_VIDEO_AVC);
            view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                mContext.startActivity(view);
            } catch (ActivityNotFoundException e) {
                // no activity can open this video
            }
        }
    };
}
