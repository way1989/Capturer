package com.way.capture.activity;

import com.way.capture.base.BaseModel;
import com.way.capture.base.BasePresenter;
import com.way.capture.base.BaseView;
import com.way.downloadlibrary.DownloadRequest;
import com.way.downloadlibrary.net.exception.DataErrorEnum;

import rx.Observable;


/**
 * Created by android on 16-12-2.
 */

public class VideoContract {
    interface Model extends BaseModel {
        Observable<Boolean> loadLocalLibrary();

        String[] getCommand(String path, String outputFile, int quality, int start, int end, int duration);

        String getOutputFileName();
    }

    interface View extends BaseView {
        void showCheckQuality();

        void showError(String msg);

        void showDownloadDialog(String platform);

        void onDownloadError(DownloadRequest downloadRequest, DataErrorEnum error);

        void onDownloadProgress(DownloadRequest downloadRequest, int downloadProgress);

        void onDownloadFinish(DownloadRequest downloadRequest);

        void onDownloadStart(DownloadRequest downloadRequest);

        void onGifStart();

        void onGifProgress(String message);

        void onGifSuccess();

        void onGifFailure();

        void onGifFinish();
    }

    abstract static class Presenter extends BasePresenter<VideoContract.View> {
        public abstract void loadFFmpeg();

        public abstract void toGif(String path, int quality, int start, int end, int duration);

        public abstract void downloadFFmpegLibrary(String platform);
    }
}
