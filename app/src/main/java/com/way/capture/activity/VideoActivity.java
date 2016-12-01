package com.way.capture.activity;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;

import com.way.capture.App;
import com.way.capture.R;
import com.way.capture.data.DataInfo;
import com.way.capture.utils.AppUtils;
import com.way.capture.utils.FilesOptHelper;
import com.way.capture.utils.GifUtils;
import com.way.capture.utils.RxBus;
import com.way.capture.utils.RxEvent;
import com.way.capture.utils.ffmpeg.ExecuteBinaryResponseHandler;
import com.way.capture.utils.ffmpeg.FFmpeg;
import com.way.capture.utils.ffmpeg.LoadBinaryResponseHandler;
import com.way.capture.utils.ffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.way.capture.utils.ffmpeg.exceptions.FFmpegNotSupportedException;
import com.way.capture.widget.FastVideoView;
import com.way.capture.widget.UpdateDownloadListener;
import com.way.capture.widget.trim.ControllerOverlay;
import com.way.capture.widget.trim.TrimControllerOverlay;
import com.way.downloadlibrary.DownloadManager;
import com.way.downloadlibrary.DownloadRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
    private int mChoiceQualityItem;
    private Toolbar toolbar;
    private boolean fullscreen;

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
    }

    @Override
    protected void initWidget() {
        super.initWidget();
        initToolbar();
        setStatusBarColor();
        initPlayControlerView();
        setupSystemUI();
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_video;
    }

    private void initToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
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
            case R.id.video_menu_rotate:
                int rotation = (((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay()).getRotation();
                if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270)
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                else
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                showControls();
                return true;
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

        showGifQualityDialog();
    }

    private void showGifQualityDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.gif_quality_title)
                .setSingleChoiceItems(R.array.gif_quality_items, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //toGif(which);
                        mChoiceQualityItem = which;
                    }
                }).setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        toGif(mChoiceQualityItem);
                    }
                })
                .create().show();
    }

    private void toGif(int which) {
        Log.i("broncho1", "toGif dialog onClick which = " + which);
        int minSize = 480;
        int frame = 12;
        switch (which) {
            case 0:
                minSize = 480;
                frame = 12;
                break;
            case 1:
                minSize = 360;
                frame = 10;
                break;
            case 2:
                minSize = 240;
                frame = 8;
                break;
        }
        String outputFile = getOutputFileName();
        String path = (String) mVideoView.getTag();

        int maxGifLength = GifUtils.MAX_GIF_LENGTH;
        mTrimStartTime = mTrimStartTime < 0 ? 0 : mTrimStartTime;
        mTrimEndTime = mTrimEndTime < 0 ? mVideoView.getDuration() : mTrimEndTime;
        int start = mTrimStartTime / 1000;
        int gifLength = (mTrimEndTime - mTrimStartTime) / 1000;
        if (gifLength > maxGifLength) {
            gifLength = start + maxGifLength;
        }
        Pair<Integer, Integer> pair = AppUtils.getVideoWidthHeight(path);
        int width = pair.first;
        int height = pair.second;

        if (Math.min(width, height) > minSize) {
            Log.i("broncho1", "width or height > minSize");
            if (width < height) {
                float scale = ((minSize * 1.00f) / width);
                width = minSize;
                height = (int) (height * scale);
                Log.i("broncho1", "width < height width = " + width + ", height = " + height + ", scale = " + scale);
            } else {
                float scale = ((minSize * 1.00f) / height);
                height = minSize;
                width = (int) (width * scale);
                Log.i("broncho1", "width > height width = " + width + ", height = " + height + ", scale = " + scale);
            }
        }

        Log.i("broncho1", "to gif start = " + start + ", length = " + gifLength
                + ", frame = " + frame + ", width = " + width + ", height = " + height);

        String[] command = GifUtils.getVideo2gifCommand(start, gifLength, frame, path,
                outputFile, width, height);
        if (command.length != 0) {
            execFFmpegBinary(command, outputFile);
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
            if(platform.startsWith("armeabi")){
                File tagetFile = new File(getFilesDir().getAbsolutePath(), AppUtils.FFMPEG_FILE_NAME);
                try {
                    InputStream is = getAssets().open("ffmpeg.zip");
                    FileOutputStream fos = new FileOutputStream(tagetFile);
                    byte[] buffer = new byte[is.available()];// 本地文件读写可用此方法
                    is.read(buffer);
                    fos.write(buffer);
                    fos.close();
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    FilesOptHelper.getInstance().unCompressFile(tagetFile.getAbsolutePath(), getFilesDir().getAbsolutePath());
                    tagetFile = new File(getFilesDir().getAbsolutePath(), AppUtils.FFMPEG_FILE_NAME);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (!tagetFile.canExecute())
                    tagetFile.setExecutable(true);
                return true;
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

    private void execFFmpegBinary(final String[] command, final String outputFile) {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(null);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        try {
            FFmpeg.getInstance(this).execute(command, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String s) {
                    //Snackbar.make(mVideoView, R.string.video_to_gif_failed, Snackbar.LENGTH_SHORT).show();
                    Toast.makeText(VideoActivity.this, R.string.video_to_gif_failed, Toast.LENGTH_SHORT).show();

                }

                @Override
                public void onSuccess(String s) {
                    //Snackbar.make(mVideoView, R.string.video_to_gif_success, Snackbar.LENGTH_SHORT).show();
                    Toast.makeText(VideoActivity.this, R.string.video_to_gif_success, Toast.LENGTH_SHORT).show();
                    MediaScannerConnection.scanFile(App.getContext(), new String[]{outputFile},
                            null, new MediaScannerConnection.OnScanCompletedListener() {
                                @Override
                                public void onScanCompleted(String path, Uri uri) {
                                    Log.d("way", "Media scanner completed.");
                                    RxBus.getInstance().post(new RxEvent.NewPathEvent(DataInfo.TYPE_SCREEN_GIF, outputFile));
                                }
                            });
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
                    onBackPressed();
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
        showControls();
    }

    @Override
    public void onHidden() {
        hideControls();
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


    private void setupSystemUI() {
        toolbar.animate().translationY(AppUtils.getStatusBarHeight(getResources())).setInterpolator(new DecelerateInterpolator())
                .setDuration(0).start();
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) showControls();
                        else hideControls();
                    }
                });
    }

    private void toggleControlsVisibility() {
        if (fullscreen) showControls();
        else hideControls();
    }

    private void hideControls() {
        if (fullscreen) return;
        //mController.hide();
        toolbar.animate().translationY(-toolbar.getHeight())
                .setInterpolator(new AccelerateInterpolator()).setDuration(200);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
        fullscreen = true;
    }

    private void showControls() {
        if (!fullscreen) return;
        int rotation = (((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay()).getRotation();
        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) { //Landscape
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            mController.setPaddingRelative(0, 0, 0, 0);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            mController.setPaddingRelative(0, 0, 0, AppUtils.getNavBarHeight(getApplicationContext()));
        }

        // mController.show();
        toolbar.animate().translationY(AppUtils.getStatusBarHeight(getResources()))
                .setInterpolator(new DecelerateInterpolator()).setDuration(240);
        fullscreen = false;

    }
}
