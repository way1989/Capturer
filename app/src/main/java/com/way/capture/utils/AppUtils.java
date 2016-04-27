package com.way.capture.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.util.Pair;

import com.way.capture.R;
import com.way.capture.data.DataInfo;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
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
    public static final String APP_FIRST_RUN = "app_first_run";
    public static final String SCREENSHOT_SHARE_SUBJECT_TEMPLATE = "Screenshot (%s)";
    public static final String GIF_SHARE_SUBJECT_TEMPLATE = "Gif (%s)";
    public static final String SCREEN_RECORD_SHARE_SUBJECT_TEMPLATE = "Screen record (%s)";

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

    public static void showDetails(Activity activity, String path, int type) {
        String message = getDetails(activity, path, type);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.image_info).setMessage(message).setPositiveButton(android.R.string.ok, null);
        builder.create().show();
    }

    private static String getDetails(Activity activity, String path, int type) {
        File file = new File(path);
        String length = android.text.format.Formatter.formatFileSize(activity, file.length());
        length = activity.getString(R.string.image_info_length, length);
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(file.lastModified()));
        time = activity.getString(R.string.image_info_time, time);
        String location = activity.getString(R.string.image_info_path, path);
        switch (type) {
            case DataInfo.TYPE_SCREEN_SHOT:
            case DataInfo.TYPE_SCREEN_GIF:
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(path, options);
                int height = options.outHeight;
                int width = options.outWidth;
                String size = activity.getString(R.string.image_info_size, width, height);

                return size + "\n" + length + "\n" + time + "\n" + location;
            case DataInfo.TYPE_SCREEN_RECORD:
                Pair<Integer, Integer> pair = AppUtils.getVideoWidthHeight(path);
                String sizeVideo = activity.getString(R.string.image_info_size, pair.first, pair.second);
                String duration = AppUtils.getVideoDuration(path);
                duration = activity.getString(R.string.image_info_duration, duration);
                return sizeVideo + "\n" + duration + "\n" + length + "\n" + time + "\n" + location;

        }
        return "";
    }

    public static void shareScreenshot(Context context, String path, int type) {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(path)));
        String subjectDate = DateFormat.getDateTimeInstance().format(new Date(System.currentTimeMillis()));
        switch (type) {
            case DataInfo.TYPE_SCREEN_SHOT:
                sharingIntent.setType("image/png");
                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, String.format(SCREENSHOT_SHARE_SUBJECT_TEMPLATE, subjectDate));
                break;
            case DataInfo.TYPE_SCREEN_GIF:
                sharingIntent.setType("image/gif");
                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, String.format(GIF_SHARE_SUBJECT_TEMPLATE, subjectDate));
                break;
            case DataInfo.TYPE_SCREEN_RECORD:
                sharingIntent.setType("video/mp4");
                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, String.format(SCREEN_RECORD_SHARE_SUBJECT_TEMPLATE, subjectDate));
                break;
        }
        Intent chooserIntent = Intent.createChooser(sharingIntent, null);
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(chooserIntent);
    }
}
