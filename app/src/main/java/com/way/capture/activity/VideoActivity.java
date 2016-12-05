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
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;

import com.way.capture.R;
import com.way.capture.base.BaseActivity;
import com.way.capture.utils.AppUtils;
import com.way.capture.utils.GifUtils;
import com.way.capture.widget.FastVideoView;
import com.way.capture.widget.trim.ControllerOverlay;
import com.way.capture.widget.trim.TrimControllerOverlay;
import com.way.downloadlibrary.DownloadManager;
import com.way.downloadlibrary.DownloadRequest;
import com.way.downloadlibrary.net.exception.DataErrorEnum;

import java.io.File;

public class VideoActivity extends BaseActivity implements MediaPlayer.OnCompletionListener,
        ControllerOverlay.Listener, VideoContract.View {
    private static final String TAG = "VideoActivity";
    private static final String ARG_IMAGE_PATH = "arg_image_path";
    private ProgressDialog mProgressDialog;
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
    private Toolbar mToolbar;
    private boolean mIsFullscreen;
    private VideoPresenter mPresenter;

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
    protected void initData() {
        super.initData();
        mPresenter = new VideoPresenter(this);
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_video;
    }

    private void initToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
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
                return true;
            case R.id.video_menu_to_gif:
                if (!isModified()) {
                    Snackbar.make(mVideoView, R.string.gif_length_error, Snackbar.LENGTH_SHORT).show();
                    return true;
                }
                mVideoView.pause();
                mPresenter.loadFFmpeg();
                return true;
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
        if (mProgressDialog != null && mProgressDialog.isShowing())
            mProgressDialog.dismiss();
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
        Log.d(TAG, "mTrimStartTime = " + mTrimStartTime / 1000 + ", trimStartTime = " + trimStartTime / 1000);
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
        return delta >= 100;
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
        mToolbar.animate().translationY(AppUtils.getStatusBarHeight(getResources())).setInterpolator(new DecelerateInterpolator())
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
        if (mIsFullscreen) showControls();
        else hideControls();
    }

    private void hideControls() {
        if (mIsFullscreen) return;
        //mController.hide();
        mToolbar.animate().translationY(-mToolbar.getHeight())
                .setInterpolator(new AccelerateInterpolator()).setDuration(200);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
        mIsFullscreen = true;
    }

    private void showControls() {
        if (!mIsFullscreen) return;
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
        mToolbar.animate().translationY(AppUtils.getStatusBarHeight(getResources()))
                .setInterpolator(new DecelerateInterpolator()).setDuration(240);
        mIsFullscreen = false;

    }

    @Override
    public void showCheckQuality() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.gif_quality_title)
                .setSingleChoiceItems(R.array.gif_quality_items, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mPresenter.toGif((String) mVideoView.getTag(), which, mTrimStartTime,
                                mTrimEndTime, mVideoView.getDuration());
                    }
                }).show();
    }

    @Override
    public void showError(String msg) {
        Snackbar.make(mVideoView, msg, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void showDownloadDialog(final String platform) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.video_to_gif_need_so_title)
                .setMessage(R.string.video_to_gif_need_so_text)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mPresenter.downloadFFmpegLibrary("x86");
                    }
                })
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    @Override
    public void onDownloadError(DownloadRequest downloadRequest, DataErrorEnum error) {
        showError(error.getMessage());
        mProgressDialog.dismiss();
    }

    @Override
    public void onDownloadProgress(DownloadRequest downloadRequest, int downloadProgress) {
        mProgressDialog.setProgress(downloadProgress);
    }

    @Override
    public void onDownloadFinish(DownloadRequest downloadRequest) {
        Log.d(TAG, "onDownloadFinish..." + downloadRequest.getFileName());
        mProgressDialog.dismiss();
    }

    @Override
    public void onDownloadStart(DownloadRequest downloadRequest) {
        mProgressDialog = new ProgressDialog(VideoActivity.this);
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setTitle(R.string.downloading);
        mProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                DownloadManager.instance().cancel(AppUtils.BASE_URL);
            }
        });
    }

    @Override
    public void onGifStart() {
        Log.d(TAG, "onStart...");
        mProgressDialog = new ProgressDialog(VideoActivity.this);
        mProgressDialog.setTitle(null);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setMessage(getString(R.string.processing));
        mProgressDialog.show();
    }

    @Override
    public void onGifProgress(String message) {
        Log.d(TAG, "onProgress...");
        mProgressDialog.setMessage(getString(R.string.processing) + "\n" + message);
    }

    @Override
    public void onGifSuccess() {
        Toast.makeText(VideoActivity.this, R.string.video_to_gif_success, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onGifFailure() {
        Toast.makeText(VideoActivity.this, R.string.video_to_gif_failed, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onGifFinish() {
        Log.d(TAG, "onFinish...");
        mProgressDialog.dismiss();
        onBackPressed();
    }
}
