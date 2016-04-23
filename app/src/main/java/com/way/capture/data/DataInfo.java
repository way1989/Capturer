package com.way.capture.data;

import android.support.annotation.Nullable;
import android.text.TextUtils;

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

    private static final String PNG = ".png";
    private static final String JPG = ".jpg";
    private static final String GIF = ".gif";
    private static final String MP4 = ".mp4";

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


}
