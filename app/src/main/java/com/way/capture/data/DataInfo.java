package com.way.capture.data;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.way.capture.App;
import com.way.capture.utils.AppUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by way on 16/2/1.
 */
public class DataInfo {
    public static final int TYPE_SCREEN_SHOT = 1;
    public static final int TYPE_SCREEN_GIF = 2;
    public static final int TYPE_SCREEN_RECORD = 3;

    public static final int COLUMN_ID = 0;
    public static final int COLUMN_PATH = 1;
    public static final int COLUMN_SIZE = 2;
    public static final int COLUMN_DATE = 3;

    private static final String PNG = ".png";
    private static final String GIF = ".gif";
    private static final String MP4 = ".mp4";

    private static final String[] PROJECTIONS = new String[]{
            MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE, MediaStore.Files.FileColumns.DATE_MODIFIED
    };

    @Nullable
    public static ArrayList<String> getDataInfos(int type) {
        switch (type) {
            case TYPE_SCREEN_SHOT:
                return getFiles(AppUtils.SCREENSHOT_FOLDER_PATH, PNG);
            case TYPE_SCREEN_GIF:
                return getFiles(AppUtils.GIF_PRODUCTS_FOLDER_PATH, GIF);
            case TYPE_SCREEN_RECORD:
                return getFiles(AppUtils.VIDEOS_FOLDER_PATH, MP4);
            default:
                break;
        }
        return null;
    }

    /*
     * 获取目录下所有文件
     */
    @Nullable
    public static ArrayList<String> getFiles(String dir, String fileType) {
        if (TextUtils.isEmpty(dir))
            return null;

        File realFile = new File(dir);
        if (!realFile.isDirectory())
            return null;

        ArrayList<File> files = new ArrayList<>();
        File[] subfiles = realFile.listFiles();
        for (File file : subfiles) {
            if (file.isDirectory())
                continue;
            if (file.length() < 1)
                continue;
            String name = file.getName();
            if (name.endsWith(fileType.toLowerCase())) {
                files.add(file);
            }
        }
        Collections.sort(files, new Comparator<File>() {
            @Override
            public int compare(File lhs, File rhs) {
                return ((Long) rhs.lastModified()).compareTo(lhs.lastModified());
            }
        });
        ArrayList<String> results = new ArrayList<>();
        for (File file : files)
            results.add(file.getAbsolutePath());
        return results;
    }

    private static ArrayList<DataInfo> getDatas(String dir, String fileType) {
        ArrayList<DataInfo> dataInfos = new ArrayList<>();
        if (TextUtils.isEmpty(dir))
            return null;

        File realFile = new File(dir);
        if (!realFile.isDirectory())
            return null;
        ContentResolver contentResolver = App.getContext().getContentResolver();
        dir = "%" + dir.subSequence(0, dir.lastIndexOf(File.separator)) + "%";
        String selections = "(" + MediaStore.Files.FileColumns.DATA + " LIKE '" + dir + "')"
                + " and (" + MediaStore.Files.FileColumns.DATA + " LIKE '%" + fileType + "')";
        Cursor cursor = contentResolver.query(getContentUriByCategory(fileType), PROJECTIONS,
                selections, null, MediaStore.Files.FileColumns.DATE_MODIFIED + " desc");

        return dataInfos;
    }

    public static Uri getContentUriByCategory(String cat) {
        Uri uri;
        String volumeName = "external";
        switch (cat) {

            case MP4:
                uri = MediaStore.Video.Media.getContentUri(volumeName);
                break;
            case PNG:
            case GIF:
                uri = MediaStore.Images.Media.getContentUri(volumeName);
                break;
            default:
                uri = null;
        }
        return uri;
    }
}
