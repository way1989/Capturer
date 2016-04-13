package com.way.captain.activity;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.way.captain.R;
import com.way.captain.utils.GifUtils;
import com.way.captain.widget.DownloadProgressBar;
import com.way.captain.widget.FastVideoView;
import com.way.downloadlibrary.DownloadManager;
import com.way.downloadlibrary.DownloadRequest;
import com.way.downloadlibrary.IDownloadListener;
import com.way.downloadlibrary.net.exception.DataErrorEnum;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;

import static android.os.Environment.DIRECTORY_MOVIES;

public class VideoActivity extends BaseActivity {
    private static final String ARG_IMAGE_PATH = "arg_image_path";
    private final static int PROGRESS_CHANGED = 0;
    private static final String TAG = "VideoActivity";
    private static final String FFMPEG = "ffmpeg";
    private final DateFormat fileFormat = new SimpleDateFormat("'Gif_'yyyy-MM-dd-HH-mm-ss'.gif'", Locale.US);
    StringBuilder mFormatBuilder;
    Formatter mFormatter;
    private FastVideoView mVideoView;
    private View mPlayControlerView;
    private ImageButton mPlayPauseBtn;
    private SeekBar mSeekBar;
    private TextView mDurationTextView;
    private TextView mPlayedTextView = null;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PROGRESS_CHANGED:

                    int position = mVideoView.getCurrentPosition();
                    mSeekBar.setProgress(position);

                    mPlayedTextView.setText(stringForTime(position));
                    msg = obtainMessage(PROGRESS_CHANGED);
                    //sendMessageDelayed(msg, 1000 - (position % 1000));
                    sendMessageDelayed(msg, 200L);
                    break;
            }

            super.handleMessage(msg);
        }
    };
    private ProgressDialog mProgressDialog;
    private String url = "http://7xrpr9.com1.z0.glb.clouddn.com/ffmpeg/armeabi-v7a-neon/ffmpeg";
    //private String url = "http://7xrpr9.com1.z0.glb.clouddn.com/ffmpeg/x86/ffmpeg";
    //private String url = "http://7xrpr9.com1.z0.glb.clouddn.com/ffmpeg/armeabi-v7a/ffmpeg";
    private Dialog mDownloadDialog;
    private DownloadProgressBar mDownloadProgressBar;

    public static void startVideoActivity(Activity context, String path, ImageView imageView) {
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
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        final android.view.Window window = getWindow();
        ObjectAnimator animator = ObjectAnimator.ofInt(window,
                "statusBarColor", window.getStatusBarColor(), Color.BLACK);
        animator.setEvaluator(new ArgbEvaluator());
        animator.setDuration(200L);
        animator.start();

        loadFFMpegBinary();
        showDownloadDialog();

        initPlayControlerView();
        initProgressDialog();
        final String path = getIntent().getStringExtra(ARG_IMAGE_PATH);
        mVideoView.setTag(path);
        mVideoView.setVideoPath(path);
        mVideoView.start();
        mPlayControlerView.setVisibility(View.VISIBLE);
    }

    private void showDownloadDialog() {
        final DownloadRequest request = new DownloadRequest(TAG, FFMPEG,
                getFilesDir().getAbsolutePath(), url);
        final DownloadManager downloadManager = DownloadManager.instance();
        downloadManager.registerListener(TAG, new UpdateDownloadListener());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(R.layout.download_dialog_layout)
                .setTitle(R.string.video_to_gif_need_so_title)
                .setMessage(R.string.video_to_gif_need_so_text)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        downloadManager.cancel(TAG);
                    }
                });
        mDownloadDialog = builder.create();
        mDownloadDialog.show();
        mDownloadProgressBar = (DownloadProgressBar) mDownloadDialog.findViewById(R.id.download_progressbar);
        mDownloadProgressBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadManager.start(request);
                mDownloadDialog.setCancelable(false);
            }
        });
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
                onGifSettingsClick();
                break;
            case R.id.video_menu_frame:
                GifUtils.framePicker(this);
                break;
            case R.id.video_menu_length:
                GifUtils.lengthPicker(this);
                break;
            case R.id.video_menu_scale:
                GifUtils.sizePicker(this);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initPlayControlerView() {
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
        mVideoView = (FastVideoView) findViewById(R.id.video_view);
        mPlayControlerView = findViewById(R.id.video_control_view);
        mPlayPauseBtn = (ImageButton) findViewById(R.id.pause);
        mSeekBar = (SeekBar) findViewById(R.id.mediacontroller_progress);
        mDurationTextView = (TextView) findViewById(R.id.time);
        mPlayedTextView = (TextView) findViewById(R.id.time_current);

        mPlayPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mVideoView.isPlaying()) {
                    mVideoView.pause();
                    mPlayPauseBtn.setImageResource(R.drawable.ic_play_arrow_white_36dp);
                } else {
                    mVideoView.start();
                    mPlayPauseBtn.setImageResource(R.drawable.ic_pause_white_36dp);
                }
            }
        });
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser)
                    mVideoView.seekTo(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mHandler.removeMessages(PROGRESS_CHANGED);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mHandler.sendEmptyMessage(PROGRESS_CHANGED);
            }
        });
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                int duration = mVideoView.getDuration();
                Log.d("onCompletion", "" + duration);
                mSeekBar.setMax(duration);
                mDurationTextView.setText(stringForTime(duration));
                mVideoView.start();
                mPlayPauseBtn.setImageResource(R.drawable.ic_pause_white_36dp);
                mHandler.sendEmptyMessage(PROGRESS_CHANGED);
            }
        });
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mPlayPauseBtn.setImageResource(R.drawable.ic_play_arrow_white_36dp);
            }
        });
    }


    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private boolean onGifSettingsClick() {
        if (mVideoView.getVisibility() != View.VISIBLE)
            return false;
        mVideoView.pause();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        File outputRoot = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_MOVIES), "Gifs");
        if (!outputRoot.exists()) {
            outputRoot.mkdir();
        }
        String outputName = fileFormat.format(new Date());
        String outputFile = new File(outputRoot, outputName).getAbsolutePath();

        int customGifLength = prefs.getInt(GifUtils.KEY_GIF_LENGTH, GifUtils.DEFAULT_GIF_LENGTH);
        int customGifFrame = prefs.getInt(GifUtils.KEY_GIF_FRAME, GifUtils.DEFAULT_GIF_FRAME);
        int customGifScale = prefs.getInt(GifUtils.KEY_GIF_SIZE, GifUtils.DEFAULT_GIF_SIZE);

        String info = (String) mVideoView.getTag();
        int videoDuration = mVideoView.getDuration() / 1000;
        int videoCurrenPosition = mVideoView.getCurrentPosition() / 1000;
        int gifLength = videoDuration - videoCurrenPosition;
        if (gifLength <= GifUtils.MIN_GIF_LENGTH) {
            gifLength = GifUtils.MIN_GIF_LENGTH;
            videoCurrenPosition = videoDuration - GifUtils.DEFAULT_GIF_LENGTH < 0 ? 0 : (videoDuration - GifUtils.DEFAULT_GIF_LENGTH);
        } else if (gifLength > customGifLength) {
            gifLength = customGifLength;
        }
        Log.i("broncho", "Video length = " + videoDuration + ", curLength = " + videoCurrenPosition
                + ", length = " + gifLength + ", frame = " + customGifFrame + ", scale = " + customGifScale);
        int[] size = formatSize(info);
        String cmd = GifUtils.getVideo2gifCommand(videoCurrenPosition, gifLength, customGifFrame, info,
                outputFile, (int) (size[0] / Math.sqrt(customGifScale)), (int) (size[1] / Math.sqrt(customGifScale)));
        String[] command = cmd.split(" ");
        if (command.length != 0) {
            execFFmpegBinary(command);
        } else {
            Toast.makeText(this, "command == null", Toast.LENGTH_LONG).show();
        }
        return true;
    }

    private int[] formatSize(String path) {
        int size[] = new int[]{0, 0};
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
            String height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT); // 视频高度
            String width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH); // 视频宽度
//            String rotation = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION); // 视频旋转方向
            size[0] = Integer.valueOf(width);
            size[1] = Integer.valueOf(height);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            retriever.release();
        }
        return size;
    }

    private void loadFFMpegBinary() {
        try {
            FFmpeg.getInstance(this.getApplicationContext()).loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                    Toast.makeText(VideoActivity.this, "load ffmpeg err...", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (FFmpegNotSupportedException e) {
        }
    }

    private void initProgressDialog() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(null);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
    }

    private void execFFmpegBinary(final String[] command) {
        try {
            FFmpeg.getInstance(this).execute(command, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String s) {
                    Snackbar.make(mVideoView, "FAILED with output : " + s, Snackbar.LENGTH_SHORT).show();
                }

                @Override
                public void onSuccess(String s) {
                    Snackbar.make(mVideoView, "SUCCESS with output : " + s, Snackbar.LENGTH_SHORT).show();
                }

                @Override
                public void onProgress(String s) {
                    Log.d(TAG, "Started command : ffmpeg " + command);
                    mProgressDialog.setMessage("Processing\n" + s);
                }

                @Override
                public void onStart() {
                    Log.d(TAG, "Started command : ffmpeg " + command);
                    mProgressDialog.setMessage("Processing...");
                    mProgressDialog.show();
                }

                @Override
                public void onFinish() {
                    Log.d(TAG, "Finished command : ffmpeg " + command);
                    mProgressDialog.dismiss();
                    onBackPressed();
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // do nothing for now
        }
    }

    private class UpdateDownloadListener implements IDownloadListener {

        @Override
        public void downloadWait(DownloadRequest downloadRequest) {
            Log.i("liweiping", "UpdateDownloadListener downloadWait...");
            mDownloadProgressBar.post(new Runnable() {
                @Override
                public void run() {
                    mDownloadProgressBar.setEnabled(false);
                }
            });
        }

        @Override
        public void downloadStart(DownloadRequest downloadRequest) {
            Log.i("liweiping", "UpdateDownloadListener downloadWait...");
            mDownloadProgressBar.post(new Runnable() {
                @Override
                public void run() {
                    mDownloadProgressBar.setEnabled(false);
                    mDownloadProgressBar.playManualProgressAnimation();
                }
            });
        }

        @Override
        public void downloadFinish(final DownloadRequest downloadRequest) {
            Log.i("liweiping", "UpdateDownloadListener downloadFinish..." + downloadRequest.getFileName());
            File file = new File(downloadRequest.getFilePath(), downloadRequest.getFileName());
            if(!file.canExecute())
                file.setExecutable(true);
            mDownloadProgressBar.post(new Runnable() {
                @Override
                public void run() {
                    mDownloadProgressBar.setSuccessResultState();
                    mDownloadProgressBar.setEnabled(false);
                    mDownloadDialog.setCancelable(true);
                }
            });
        }

        @Override
        public void downloadCancel(DownloadRequest downloadRequest) {
            Log.i("liweiping", "UpdateDownloadListener downloadCancel...");
            mDownloadProgressBar.post(new Runnable() {
                @Override
                public void run() {
                    mDownloadProgressBar.abortDownload();
                    mDownloadDialog.setCancelable(true);
                    mDownloadProgressBar.setEnabled(true);
                }
            });
        }

        @Override
        public void downloadPause(DownloadRequest downloadRequest) {
            Log.i("liweiping", "UpdateDownloadListener downloadPause...");
        }

        @Override
        public void downloadProgress(DownloadRequest downloadRequest, final int downloadProgress) {
            Log.i("liweiping", "UpdateDownloadListener downloadProgress... downloadProgress = " + downloadProgress);
            mDownloadProgressBar.post(new Runnable() {
                @Override
                public void run() {
                    mDownloadProgressBar.setProgress(downloadProgress);
                }
            });
        }

        @Override
        public void downloadError(DownloadRequest downloadRequest, DataErrorEnum error) {
            Log.i("liweiping", "UpdateDownloadListener downloadError... error = " + error);
            mDownloadProgressBar.post(new Runnable() {
                @Override
                public void run() {
                    mDownloadProgressBar.setErrorResultState();
                    mDownloadProgressBar.setEnabled(true);
                    mDownloadDialog.setCancelable(true);
                }
            });
        }
    }
}
