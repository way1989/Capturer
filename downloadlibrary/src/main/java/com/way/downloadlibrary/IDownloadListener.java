package com.way.downloadlibrary;

import com.way.downloadlibrary.net.exception.DataErrorEnum;

public interface IDownloadListener {
    void downloadWait(DownloadRequest downloadRequest);

    void downloadStart(DownloadRequest downloadRequest);

    void downloadFinish(DownloadRequest downloadRequest);

    void downloadCancel(DownloadRequest downloadRequest);

    void downloadPause(DownloadRequest downloadRequest);

    void downloadProgress(DownloadRequest downloadRequest, int downloadProgress);

    void downloadError(DownloadRequest downloadRequest, DataErrorEnum error);

}
