package com.way.capture.activity;

import android.util.Log;
import android.util.Pair;

import com.way.capture.App;
import com.way.capture.utils.AppUtil;
import com.way.capture.utils.FfmpegUtil;
import com.way.capture.utils.FilesOptHelper;
import com.way.capture.utils.OsUtil;
import com.way.capture.utils.RxSchedulers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;


/**
 * Created by android on 16-12-2.
 */

public class VideoModel implements VideoContract.Model {
    private static final String TAG = "VideoModel";
    private static final String ZIP_LIBRARY_NAME = "ffmpeg.zip";
    private static final DateFormat FILE_FORMAT = new SimpleDateFormat("'Gif_'yyyyMMddHHmmss'.gif'", Locale.getDefault());

    @Override
    public Observable<Boolean> loadLocalLibrary() {
        return Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> e) throws Exception {
                e.onNext(copyAndLoadLibrary());
                e.onComplete();
            }
        }).compose(RxSchedulers.<Boolean>io_main());
    }

    @Override
    public String[] getCommand(String path, String outputFile, int quality, int mTrimStartTime, int mTrimEndTime, int duration) {
        Log.d(TAG, "toGif dialog onClick quality = " + quality);
        int minSize = 480;
        int frame = 12;
        switch (quality) {
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

        int maxGifLength = FfmpegUtil.MAX_GIF_LENGTH;
        mTrimStartTime = mTrimStartTime < 0 ? 0 : mTrimStartTime;
        mTrimEndTime = mTrimEndTime < 0 ? duration : mTrimEndTime;
        int start = mTrimStartTime / 1000;
        int gifLength = (mTrimEndTime - mTrimStartTime) / 1000;
        if (gifLength > maxGifLength) {
            gifLength = start + maxGifLength;
        }
        Pair<Integer, Integer> pair = AppUtil.getVideoWidthHeight(path);
        int width = pair.first;
        int height = pair.second;

        if (Math.min(width, height) > minSize) {
            Log.d(TAG, "width or height > minSize");
            if (width < height) {
                float scale = ((minSize * 1.00f) / width);
                width = minSize;
                height = (int) (height * scale);
                Log.d(TAG, "width < height width = " + width + ", height = " + height + ", scale = " + scale);
            } else {
                float scale = ((minSize * 1.00f) / height);
                height = minSize;
                width = (int) (width * scale);
                Log.d(TAG, "width > height width = " + width + ", height = " + height + ", scale = " + scale);
            }
        }

        Log.d(TAG, "to gif start = " + start + ", length = " + gifLength + ", frame = " + frame
                + ", width = " + width + ", height = " + height);

        String[] command = FfmpegUtil.getVideo2gifCommand(start, gifLength, frame, path,
                outputFile, width, height);
        if (command.length != 0)
            return command;
        return new String[0];
    }

    private boolean copyAndLoadLibrary() {
        File targetFile = new File(App.getContext().getFilesDir().getAbsolutePath(), AppUtil.FFMPEG_FILE_NAME);
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            //copy zip file to file dir
            is = App.getContext().getAssets().open(ZIP_LIBRARY_NAME);
            fos = new FileOutputStream(targetFile);
            byte[] buffer = new byte[is.available()];// 本地文件读写可用此方法
            is.read(buffer);
            fos.write(buffer);

            //un compress library
            FilesOptHelper.getInstance().unCompressFile(targetFile.getAbsolutePath(),
                    App.getContext().getFilesDir().getAbsolutePath());
            //return true if can execute
            return targetFile.canExecute() || targetFile.setExecutable(true);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            OsUtil.closeSilently(is);
            OsUtil.closeSilently(fos);
        }
        return false;
    }

    @Override
    public String getOutputFileName() {
        File outputRoot = new File(AppUtil.GIF_PRODUCTS_FOLDER_PATH);
        if (!outputRoot.exists()) {
            outputRoot.mkdir();
        }
        String outputName = FILE_FORMAT.format(new Date());
        return new File(outputRoot, outputName).getAbsolutePath();
    }
}
