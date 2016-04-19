package com.way.captain.utils;

import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Environment;
import android.util.Pair;

import java.io.File;
import java.util.Formatter;
import java.util.Locale;

public class AppUtils {
    public static final String BASE_URL = "http://7xrpr9.com1.z0.glb.clouddn.com/ffmpeg/%s/ffmpeg.zip";
    public static final String FFMPEG_FILE_NAME = "ffmpeg";

    public static final String APP_ABSOLUTE_ROOT_PATH = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_MOVIES).getAbsolutePath();
    public static final String APP_SCREENSHOT_ABSOLUTE_ROOT_PATH = Environment
            .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
    //screenshot
    public static final String SCREENSHOT_FOLDER_NAME = "Screenshots";
    public static final String SCREENSHOT_FOLDER_PATH = new File(APP_SCREENSHOT_ABSOLUTE_ROOT_PATH,
            SCREENSHOT_FOLDER_NAME).getAbsolutePath();
    // gif 作品库
    public static final String GIF_PRODUCTS_FOLDER_NAME = "Gifs";
    public static final String GIF_PRODUCTS_FOLDER_PATH = new File(APP_ABSOLUTE_ROOT_PATH,
            GIF_PRODUCTS_FOLDER_NAME).getAbsolutePath();

    // videos folder, maybe not be needed
    public static final String VIDEOS_FOLDER_NAME = "ScreenRecord";
    public static final String VIDEOS_FOLDER_PATH = new File(APP_ABSOLUTE_ROOT_PATH,
            VIDEOS_FOLDER_NAME).getAbsolutePath();

    public static boolean isMarshmallow() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    public static boolean isLollipop() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static Pair<Integer, Integer> getVideoWidthHeight(String path) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
            String height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT); // 视频高度
            String width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH); // 视频宽度
//            String rotation = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION); // 视频旋转方向
            return new Pair<>(Integer.valueOf(width), Integer.valueOf(height));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            retriever.release();
        }
        return new Pair<>(Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    public static String getVideoDuration(String path) {
        String totalDuration = "00:00";
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
            // 取得视频的长度(单位为毫秒)
            String time = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            totalDuration = getVideoFormatTime(Integer.valueOf(time));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            retriever.release();
        }
        return totalDuration;
    }

    public static String getVideoFormatTime(int timeMs) {
        StringBuilder formatBuilder = new StringBuilder();
        Formatter formatter = new Formatter(formatBuilder, Locale.getDefault());
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        formatBuilder.setLength(0);
        if (hours > 0) {
            return formatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return formatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    public static String getVideoRecordTime(long milliSeconds, boolean displayCentiSeconds) {
        long seconds = milliSeconds / 1000; // round down to compute seconds
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long remainderMinutes = minutes - (hours * 60);
        long remainderSeconds = seconds - (minutes * 60);

        StringBuilder timeStringBuilder = new StringBuilder();

        // Hours
        if (hours > 0) {
            if (hours < 10) {
                timeStringBuilder.append('0');
            }
            timeStringBuilder.append(hours);

            timeStringBuilder.append(':');
        } else {
            //timeStringBuilder.append('0');
            //timeStringBuilder.append('0');
            //timeStringBuilder.append(':');
        }

        // Minutes
        if (remainderMinutes < 10) {
            timeStringBuilder.append('0');
        }
        timeStringBuilder.append(remainderMinutes);
        timeStringBuilder.append(':');

        // Seconds
        if (remainderSeconds < 10) {
            timeStringBuilder.append('0');
        }
        timeStringBuilder.append(remainderSeconds);

        // Centi seconds
        if (displayCentiSeconds) {
            timeStringBuilder.append('.');
            long remainderCentiSeconds = (milliSeconds - seconds * 1000) / 10;
            if (remainderCentiSeconds < 10) {
                timeStringBuilder.append('0');
            }
            timeStringBuilder.append(remainderCentiSeconds);
        }

        return timeStringBuilder.toString();
    }
    // Throws NullPointerException if the input is null.
    public static <T> T checkNotNull(T object) {
        if (object == null) throw new NullPointerException();
        return object;
    }
}
