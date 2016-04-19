package com.way.captain.activity;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.way.captain.R;
import com.way.captain.utils.AppUtils;
import com.way.captain.utils.GifUtils;
import com.way.captain.widget.FastVideoView;
import com.way.captain.widget.UpdateDownloadListener;
import com.way.captain.widget.trim.ControllerOverlay;
import com.way.captain.widget.trim.TrimControllerOverlay;
import com.way.downloadlibrary.DownloadManager;
import com.way.downloadlibrary.DownloadRequest;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class VideoActivity extends BaseActivity implements MediaPlayer.OnCompletionListener,
        ControllerOverlay.Listener {
    private static final String TAG = "VideoActivity";
    private static final String ARG_IMAGE_PATH = "arg_image_path";
    private final static int PROGRESS_CHANGED = 0;
    private final DateFormat fileFormat = new SimpleDateFormat("'Gif_'yyyy-MM-dd-HH-mm-ss'.gif'");
    private FastVideoView mVideoView;
    private Handler mHandler = new Handler();
    private TrimControllerOverlay mController;
    private boolean mIsInProgressCheck = false;
    private int mTrimStartTime = 0;
    private int mTrimEndTime = 0;
    private int mVideoPosition = 0;
    private final Runnable mProgressChecker = new Runnable() {
        @Override
        public void run() {
            int pos = setProgress();
            mIsInProgressCheck = true;
            mHandler.postDelayed(mProgressChecker, 200 - (pos % 200));
        }
    };
    private boolean mDragging;

    public static void startVideoActivity(Activity context, String path, View imageView) {
        Intent i = new Intent(context, VideoActivity.class);
        i.putExtra(ARG_IMAGE_PATH, path);
//        context.startActivity(i, ActivityOptions.makeSceneTransitionAnimation(context, imageView,
//                context.getString(R.string.image_transition)).toBundle());
        context.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        initToolbar();
        setStatusBarColor();
        initPlayControlerView();
    }

    private void initToolbar() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            final String path = getIntent().getStringExtra(ARG_IMAGE_PATH);
            setActionBarTitle(path);
        }
    }

    private void setActionBarTitle(String path) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null)
            return;
        if (path.contains(File.separator) && path.contains(".")) {
            String title = path.substring(path.lastIndexOf(File.separatorChar) + 1, path.lastIndexOf('.'));
            actionBar.setTitle(title);
        }
    }

    private void setStatusBarColor() {
        final android.view.Window window = getWindow();
        ObjectAnimator animator = ObjectAnimator.ofInt(window,
                "statusBarColor", window.getStatusBarColor(), Color.BLACK);
        animator.setEvaluator(new ArgbEvaluator());
        animator.setDuration(200L);
        animator.start();
    }

    //private MediaControlDrawable controlDrawable;
    private void initPlayControlerView() {
        final String path = getIntent().getStringExtra(ARG_IMAGE_PATH);

        mVideoView = (FastVideoView) findViewById(R.id.video_view);
        if (mVideoView != null) {
            mVideoView.setOnCompletionListener(this);
            mVideoView.setTag(path);
            mVideoView.setVideoPath(path);
        }
//        controlDrawable =
//                new MediaControlDrawable.Builder(this)
//                        .setColor(Color.WHITE)
//                        .setPadding(DensityUtil.px2dip(this, 8))
//                        .setInitialState(MediaControlDrawable.State.PLAY)
//                        .build();

        mController = new TrimControllerOverlay(this);
        View rootView = findViewById(R.id.trim_view_root);
        if (rootView != null)
            ((ViewGroup) rootView).addView(mController.getView());
        mController.setListener(this);
        mController.setCanReplay(true);
        playVideo();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_video_edit, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finishAfterTransition();
                break;
            case R.id.video_menu_to_gif:
                toGif();
                break;
            case R.id.video_menu_frame:
                GifUtils.framePicker(this);
                break;
            //case R.id.video_menu_length:
            //    GifUtils.lengthPicker(this);
            //    break;
            case R.id.video_menu_scale:
                GifUtils.sizePicker(this);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        //controlDrawable.setMediaControlState(MediaControlDrawable.State.PLAY);
        mVideoView.seekTo(mTrimEndTime);
        mController.showEnded();
    }

    private void toGif() {
        if (!isModified()) {
            Snackbar.make(mVideoView, R.string.gif_length_error, Snackbar.LENGTH_SHORT).show();
            return;
        }
        mVideoView.pause();
        if (!loadFFmpeg()) return;

        String outputFile = getOutputFileName();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int maxGifLength = GifUtils.MAX_GIF_LENGTH;
        int customGifFrame = prefs.getInt(GifUtils.KEY_GIF_FRAME, GifUtils.DEFAULT_GIF_FRAME);
        int customGifScale = prefs.getInt(GifUtils.KEY_GIF_SIZE, GifUtils.DEFAULT_GIF_SIZE);

        String path = (String) mVideoView.getTag();

        mTrimStartTime = mTrimStartTime < 0 ? 0 : mTrimStartTime;
        mTrimEndTime = mTrimEndTime < 0 ? mVideoView.getDuration() : mTrimEndTime;
        int start = mTrimStartTime / 1000;
        int gifLength = (mTrimEndTime - mTrimStartTime) / 1000;
        if (gifLength > maxGifLength) {
            gifLength = start + maxGifLength;
        }
        Log.i("broncho", "to gif start = " + start + ", length = " + gifLength
                + ", frame = " + customGifFrame + ", scale = " + customGifScale);
        Pair<Integer, Integer> pair = AppUtils.getVideoWidthHeight(path);
        String[] command = GifUtils.getVideo2gifCommand(start, gifLength, customGifFrame, path,
                outputFile, (int) (pair.first / Math.sqrt(customGifScale)), (int) (pair.second / Math.sqrt(customGifScale)));
        if (command.length != 0) {
            execFFmpegBinary(command);
        } else {
            Snackbar.make(mVideoView, R.string.video_to_gif_failed, Snackbar.LENGTH_SHORT).show();
        }
    }

    @NonNull
    private String getOutputFileName() {
        File outputRoot = new File(AppUtils.GIF_PRODUCTS_FOLDER_PATH);
        if (!outputRoot.exists()) {
            outputRoot.mkdir();
        }
        String outputName = fileFormat.format(new Date());
        return new File(outputRoot, outputName).getAbsolutePath();
    }

    private boolean loadFFmpeg() {
        if (!FFmpeg.getInstance(this).hasLibrary()) {
            String platform = FFmpeg.getInstance(this).getLibraryPlatform();
            if (TextUtils.isEmpty(platform)) {
                Snackbar.make(mVideoView, R.string.not_support_devices, Snackbar.LENGTH_LONG).show();
                return false;
            }
            showDownloadDialog(platform);
            return false;
        }
        loadFFMpegBinary();
        return true;
    }

    private void loadFFMpegBinary() {
        try {
            FFmpeg.getInstance(this.getApplicationContext()).loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                    Snackbar.make(mVideoView, R.string.not_support_devices, Snackbar.LENGTH_SHORT).show();
                }
            });
        } catch (FFmpegNotSupportedException e) {
            Snackbar.make(mVideoView, R.string.not_support_devices, Snackbar.LENGTH_SHORT).show();
        }

    }

    private void execFFmpegBinary(final String[] command) {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(null);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        try {
            FFmpeg.getInstance(this).execute(command, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String s) {
                    Snackbar.make(mVideoView, R.string.video_to_gif_failed, Snackbar.LENGTH_SHORT).show();
                }

                @Override
                public void onSuccess(String s) {
                    Snackbar.make(mVideoView, R.string.video_to_gif_success, Snackbar.LENGTH_SHORT).show();
                }

                @Override
                public void onProgress(String s) {
                    Log.d(TAG, "Started command : ffmpeg " + command);
                    progressDialog.setMessage(getString(R.string.processing) + "\n" + s);
                }

                @Override
                public void onStart() {
                    Log.d(TAG, "Started command : ffmpeg " + command);
                    progressDialog.setMessage(getString(R.string.processing));
                    progressDialog.show();
                }

                @Override
                public void onFinish() {
                    Log.d(TAG, "Finished command : ffmpeg " + command);
                    progressDialog.dismiss();
                    //onBackPressed();
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            Snackbar.make(mVideoView, R.string.video_to_gif_failed, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void showDownloadDialog(String platform) {
        //platform = "armeabi-v7a";
        final DownloadRequest request = new DownloadRequest(AppUtils.BASE_URL, AppUtils.FFMPEG_FILE_NAME,
                getFilesDir().getAbsolutePath(), String.format(AppUtils.BASE_URL, platform));
        Log.i("liweiping", "download url = " + String.format(AppUtils.BASE_URL, platform));
        final DownloadManager downloadManager = DownloadManager.instance();
        downloadManager.registerListener(AppUtils.BASE_URL, new UpdateDownloadListener(VideoActivity.this));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.video_to_gif_need_so_title)
                .setMessage(R.string.video_to_gif_need_so_text)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        downloadManager.start(request);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null).create().show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDragging = false;
        mHandler.removeCallbacks(mProgressChecker);
        mHandler.post(mProgressChecker);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mController.showPaused();
        mHandler.removeCallbacks(mProgressChecker);
    }

    @Override
    public void onPlayPause() {
        if (mVideoView.isPlaying()) {
            pauseVideo();
        } else {
            playVideo();
        }
    }

    @Override
    public void onSeekStart() {
        mDragging = true;
        pauseVideo();
    }

    @Override
    public void onSeekMove(int time) {
        if (!mDragging) {
            mVideoView.seekTo(time);
        }
    }

    @Override
    public void onSeekEnd(int time, int trimStartTime, int trimEndTime) {
        time = time < 0 ? 0 : time;
        trimStartTime = trimStartTime < 0 ? 0 : trimStartTime;
        trimEndTime = trimEndTime < 0 ? mVideoView.getDuration() : trimEndTime;

        mDragging = false;
        mVideoView.seekTo(time);
        int maxLength = GifUtils.MAX_GIF_LENGTH * 1000;
        Log.i("broncho1", "mTrimStartTime = " + mTrimStartTime / 1000 + ", trimStartTime = " + trimStartTime / 1000);
        mTrimEndTime = trimEndTime;//结束时间
        if (mTrimStartTime / 1000 != trimStartTime / 1000) {//拖动了起始位置
            mTrimStartTime = trimStartTime;
            if (mTrimEndTime - mTrimStartTime > maxLength)
                mTrimEndTime = mTrimStartTime + maxLength;
        } else {//两个都拖动或者值拖动了后面
            mTrimStartTime = trimStartTime;
            if (mTrimEndTime - mTrimStartTime > maxLength)
                mTrimStartTime = (mTrimEndTime - maxLength) < 0 ? 0 : (mTrimEndTime - maxLength);
        }

        mIsInProgressCheck = false;
        // If the position is bigger than the end point of trimming, show the
        setProgress();
    }

    @Override
    public void onShown() {
        getSupportActionBar().show();
    }

    @Override
    public void onHidden() {
        getSupportActionBar().hide();
    }

    @Override
    public void onReplay() {
        mVideoView.seekTo(mTrimStartTime);
        playVideo();
    }

    private int setProgress() {
        mVideoPosition = mVideoView.getCurrentPosition();

        if (!mIsInProgressCheck && mVideoPosition < mTrimStartTime) {
            mVideoView.seekTo(mTrimStartTime);
            mVideoPosition = mTrimStartTime;
        }
        // If the position is bigger than the end point of trimming, show the
        // replay button and pause.
        if (mVideoPosition >= mTrimEndTime && mTrimEndTime > 0) {
            if (mVideoPosition > mTrimEndTime) {
                mVideoView.seekTo(mTrimEndTime);
                mVideoPosition = mTrimEndTime;
            }
            mController.showEnded();
            mVideoView.pause();
        }

        int duration = mVideoView.getDuration();
        if (duration > 0 && mTrimEndTime <= 0) {
            mTrimEndTime = duration;
            int max = GifUtils.MAX_GIF_LENGTH * 1000;
            if (mTrimEndTime - mTrimStartTime > max)
                mTrimEndTime = mTrimStartTime + max;
        }

        mController.setTimes(mVideoPosition, duration, mTrimStartTime, mTrimEndTime);
        // Enable save if there's modifications
        //mSaveVideoTextView.setEnabled(isModified());
        return mVideoPosition;
    }

    private boolean isModified() {
        int delta = mTrimEndTime - mTrimStartTime;

        // Considering that we only trim at sync frame, we don't want to trim
        // when the time interval is too short or too close to the origin.
        if (delta < 100 /*|| Math.abs(mVideoView.getDuration() - delta) < 100*/) {
            return false;
        } else {
            return true;
        }
    }

    private void playVideo() {
        if (mVideoPosition < mTrimStartTime) {
            mVideoView.seekTo(mTrimStartTime);
        }
        mVideoView.start();
        mController.showPlaying();
        mHandler.removeCallbacks(mProgressChecker);
        mHandler.post(mProgressChecker);
    }

    private void pauseVideo() {
        mVideoView.pause();
        mHandler.removeCallbacks(mProgressChecker);
        mController.showPaused();
    }
}
