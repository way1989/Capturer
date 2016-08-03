package com.way.capture.widget;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.way.capture.R;
import com.way.capture.utils.AppUtils;
import com.way.capture.utils.FilesOptHelper;
import com.way.downloadlibrary.DownloadManager;
import com.way.downloadlibrary.DownloadRequest;
import com.way.downloadlibrary.IDownloadListener;
import com.way.downloadlibrary.net.exception.DataErrorEnum;

import java.io.File;

/**
 * Created by way on 16/4/14.
 */
public class UpdateDownloadListener implements IDownloadListener {
    private Dialog downloadDialog;
    private DownloadProgressBar downloadProgressBar;
    private TextView textView;
    private Handler handler = new Handler(Looper.getMainLooper());

    public UpdateDownloadListener(Activity activity) {
        if (activity == null || activity.isFinishing())
            return;
        AlertDialog.Builder downloadBuiler = new AlertDialog.Builder(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.download_dialog_layout, null);
        downloadBuiler.setTitle(R.string.downloading)
                .setView(view).setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DownloadManager.instance().cancel(AppUtils.BASE_URL);
                    }
                });
        downloadDialog = downloadBuiler.create();
        downloadDialog.setCancelable(false);
        downloadDialog.setCanceledOnTouchOutside(false);
        downloadProgressBar = (DownloadProgressBar) view.findViewById(R.id.download_progressbar);
        textView = (TextView) view.findViewById(R.id.download_text);
    }

    @Override
    public void downloadWait(DownloadRequest downloadRequest) {
        Log.i("liweiping", "UpdateDownloadListener downloadWait...");
        handler.post(new Runnable() {
            @Override
            public void run() {
                downloadDialog.show();
                textView.setText(R.string.wait);
            }
        });
    }

    @Override
    public void downloadStart(DownloadRequest downloadRequest) {
        Log.i("liweiping", "UpdateDownloadListener downloadWait...");
        handler.post(new Runnable() {
            @Override
            public void run() {
                downloadProgressBar.playManualProgressAnimation();
                textView.setText(R.string.start);
            }
        });
    }

    @Override
    public void downloadFinish(final DownloadRequest downloadRequest) {
        Log.i("liweiping", "UpdateDownloadListener downloadFinish..." + downloadRequest.getFileName());
        File file = new File(downloadRequest.getFilePath(), downloadRequest.getFileName());
        try {
            FilesOptHelper.getInstance().unCompressFile(file.getAbsolutePath(), downloadRequest.getFilePath());
            file = new File(downloadRequest.getFilePath(), AppUtils.FFMPEG_FILE_NAME);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!file.canExecute())
            file.setExecutable(true);
        handler.post(new Runnable() {
            @Override
            public void run() {
                downloadProgressBar.setSuccessResultState();
                textView.setText(R.string.success);
            }
        });
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                downloadDialog.dismiss();
            }
        }, 3500L);
    }

    @Override
    public void downloadCancel(DownloadRequest downloadRequest) {
        Log.i("liweiping", "UpdateDownloadListener downloadCancel...");
        handler.post(new Runnable() {
            @Override
            public void run() {
                downloadProgressBar.abortDownload();
                textView.setText(R.string.abort);
            }
        });
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                downloadDialog.dismiss();
            }
        }, 3500L);
    }

    @Override
    public void downloadPause(DownloadRequest downloadRequest) {
        Log.i("liweiping", "UpdateDownloadListener downloadPause...");
    }

    @Override
    public void downloadProgress(DownloadRequest downloadRequest, final int downloadProgress) {
        Log.i("liweiping", "UpdateDownloadListener downloadProgress... downloadProgress = " + downloadProgress);
        handler.post(new Runnable() {
            @Override
            public void run() {
                downloadProgressBar.setProgress(downloadProgress);
                textView.setText(downloadProgress + "%");
            }
        });
    }

    @Override
    public void downloadError(DownloadRequest downloadRequest, DataErrorEnum error) {
        Log.i("liweiping", "UpdateDownloadListener downloadError... error = " + error);
        handler.post(new Runnable() {
            @Override
            public void run() {
                downloadProgressBar.setErrorResultState();
                textView.setText(R.string.error);
            }
        });
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                downloadDialog.dismiss();
            }
        }, 3500L);
    }
}
