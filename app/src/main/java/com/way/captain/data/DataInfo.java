package com.way.captain.data;

import android.text.TextUtils;
import android.util.Log;

import com.way.captain.utils.AppUtils;

import java.io.File;
import java.util.ArrayList;

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
    public static ArrayList<String> getFiles(String dir, String fileType) {

        if (TextUtils.isEmpty(dir))
            return null;

        File realFile = new File(dir);
        if (!realFile.isDirectory())
            return null;

        ArrayList<String> files = new ArrayList<>();
        File[] subfiles = realFile.listFiles();
        for (File file : subfiles) {
            if (file.isDirectory())
                continue;
            String name = file.getName();
            int i = name.indexOf('.');
            if (i == -1)
                continue;
            name = name.substring(i);
            if (name.equalsIgnoreCase(fileType)) {
                files.add(file.getAbsolutePath());
            }
        }
        return files;
    }


}
