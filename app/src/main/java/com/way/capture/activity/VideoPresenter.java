package com.way.capture.activity;

import android.media.MediaScannerConnection;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.way.capture.App;
import com.way.capture.R;
import com.way.capture.data.DataInfo;
import com.way.capture.utils.AppUtils;
import com.way.capture.utils.FilesOptHelper;
import com.way.capture.utils.RxBus;
import com.way.capture.utils.RxEvent;
import com.way.capture.utils.ffmpeg.ExecuteBinaryResponseHandler;
import com.way.capture.utils.ffmpeg.FFmpeg;
import com.way.capture.utils.ffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.way.downloadlibrary.DownloadManager;
import com.way.downloadlibrary.DownloadRequest;
import com.way.downloadlibrary.IDownloadListener;
import com.way.downloadlibrary.net.exception.DataErrorEnum;

import java.io.File;

import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by android on 16-12-2.
 */

public class VideoPresenter extends VideoContract.Presenter {
    private static final String TAG = "VideoPresenter";
    @NonNull
    private final CompositeSubscription mSubscriptions;
    private VideoContract.View mView;
    private VideoContract.Model mModel;
    private IDownloadListener mDownloadListener = new IDownloadListener() {
        @Override
        public void downloadWait(DownloadRequest downloadRequest) {

        }

        @Override
        public void downloadStart(DownloadRequest downloadRequest) {

        }

        @Override
        public void downloadFinish(DownloadRequest downloadRequest) {

        }

        @Override
        public void downloadCancel(DownloadRequest downloadRequest) {

        }

        @Override
        public void downloadPause(DownloadRequest downloadRequest) {

        }

        @Override
        public void downloadProgress(DownloadRequest downloadRequest, int downloadProgress) {

        }

        @Override
        public void downloadError(DownloadRequest downloadRequest, DataErrorEnum error) {

        }
    };

    public VideoPresenter(VideoContract.View view) {
        mView = view;
        mSubscriptions = new CompositeSubscription();
        mModel = new VideoModel();
    }

    @Override
    public void loadFFmpeg() {
        FFmpeg ffmpeg = FFmpeg.getInstance(App.getContext());
        if (ffmpeg.hasLibrary()) {
            mView.showCheckQuality();
            return;
        }
        String platform = ffmpeg.getLibraryPlatform();
        if (TextUtils.isEmpty(platform)) {
            mView.showError(App.getContext().getString(R.string.not_support_devices));
            return;
        }
        if (platform.startsWith("armeabi")) {
            mSubscriptions.clear();
            mSubscriptions.add(mModel.loadLocalLibrary().subscribe(new Observer<Boolean>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {

                }

                @Override
                public void onNext(Boolean result) {
                    if (result) mView.showCheckQuality();
                    else mView.showError(App.getContext().getString(R.string.not_support_devices));
                }
            }));
            return;
        }
        //if all not return, begin to download library
        mView.showDownloadDialog(platform);
    }

    @Override
    public void toGif(String path, int quality, int start, int end, int duration) {
        final String outputFile = mModel.getOutputFileName();
        String[] command = mModel.getCommand(path, outputFile, quality, start, end, duration);
        try {
            FFmpeg.getInstance(App.getContext()).execute(command, new ExecuteBinaryResponseHandler() {
                @Override
                public void onSuccess(String message) {
                    super.onSuccess(message);
                    MediaScannerConnection.scanFile(App.getContext(), new String[]{outputFile},
                            null, new MediaScannerConnection.OnScanCompletedListener() {
                                @Override
                                public void onScanCompleted(String path, Uri uri) {
                                    Log.d("way", "Media scanner completed.");
                                    RxBus.getInstance().post(new RxEvent.NewPathEvent(DataInfo.TYPE_SCREEN_GIF, outputFile));
                                }
                            });
                    mView.onGifSuccess();
                }

                @Override
                public void onProgress(String message) {
                    super.onProgress(message);
                    mView.onGifProgress(message);
                }

                @Override
                public void onFailure(String message) {
                    super.onFailure(message);
                    mView.onGifFailure();
                }

                @Override
                public void onStart() {
                    super.onStart();
                    mView.onGifStart();
                }

                @Override
                public void onFinish() {
                    super.onFinish();
                    mView.onGifFinish();
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            e.printStackTrace();
            mView.showError(App.getContext().getString(R.string.video_to_gif_failed));
        }
    }

    @Override
    public void downloadFFmpegLibrary(String platform) {
        final DownloadRequest request = new DownloadRequest(AppUtils.BASE_URL,
                AppUtils.FFMPEG_FILE_NAME, App.getContext().getFilesDir().getAbsolutePath(),
                String.format(AppUtils.BASE_URL, platform));
        Log.d(TAG, "download url = " + String.format(AppUtils.BASE_URL, platform));
        final DownloadManager downloadManager = DownloadManager.instance();
        downloadManager.registerListener(AppUtils.BASE_URL, new IDownloadListener() {
            @Override
            public void downloadWait(DownloadRequest downloadRequest) {
            }

            @Override
            public void downloadStart(final DownloadRequest downloadRequest) {
                AndroidSchedulers.mainThread().createWorker().schedule(new Action0() {
                    @Override
                    public void call() {
                        mView.onDownloadStart(downloadRequest);
                    }
                });
            }

            @Override
            public void downloadFinish(final DownloadRequest downloadRequest) {
                File file = new File(downloadRequest.getFilePath(), downloadRequest.getFileName());
                try {
                    FilesOptHelper.getInstance().unCompressFile(file.getAbsolutePath(), downloadRequest.getFilePath());
                    file = new File(downloadRequest.getFilePath(), AppUtils.FFMPEG_FILE_NAME);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (!file.canExecute())
                    file.setExecutable(true);

                AndroidSchedulers.mainThread().createWorker().schedule(new Action0() {
                    @Override
                    public void call() {
                        mView.onDownloadFinish(downloadRequest);
                    }
                });
            }

            @Override
            public void downloadCancel(DownloadRequest downloadRequest) {

            }

            @Override
            public void downloadPause(DownloadRequest downloadRequest) {

            }

            @Override
            public void downloadProgress(final DownloadRequest downloadRequest, final int downloadProgress) {
                AndroidSchedulers.mainThread().createWorker().schedule(new Action0() {
                    @Override
                    public void call() {
                        mView.onDownloadProgress(downloadRequest, downloadProgress);
                    }
                });
            }

            @Override
            public void downloadError(final DownloadRequest downloadRequest, final DataErrorEnum error) {
                AndroidSchedulers.mainThread().createWorker().schedule(new Action0() {
                    @Override
                    public void call() {
                        mView.onDownloadError(downloadRequest, error);
                    }
                });
            }
        });
        downloadManager.start(request);
    }

    @Override
    public void unSubscribe() {
        mSubscriptions.clear();
    }

}