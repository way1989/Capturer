package com.way.captain.data;

import android.text.TextUtils;
import android.text.format.Formatter;

import com.way.captain.App;
import com.way.captain.utils.AppUtils;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created by way on 16/2/1.
 */
public class ScreenshotInfos {
    public static final String TYPE_SCREENSHOT = "PNG";
    private static final String PNG = ".png";
    private static final Comparator<File> mComparator = new Comparator<File>() {
//        private final Collator sCollator = Collator.getInstance();

        @Override
        public int compare(File lhs, File rhs) {
            // return sCollator.compare(lhs.lastModified(), rhs.lastModified());
            return new Long(rhs.lastModified()).compareTo(new Long(lhs.lastModified()));
        }
    };
    private String mScreenshotName;
    private String mScreenshotPath;
    private String mLastModifyTime;
    private String mFileSize;

    public ScreenshotInfos(String gifPath) {
        parseFromPath(gifPath);
        mScreenshotPath = gifPath;
    }

    public static ArrayList<ScreenshotInfos> getScreenshotInfos() {
        ArrayList<ScreenshotInfos> screenshotInfoses = new ArrayList<>();
        List<File> gifFiles = getFiles(AppUtils.SCREENSHOT_FOLDER_PATH, new ArrayList<File>());

        if (!gifFiles.isEmpty()) {
            Collections.sort(gifFiles, mComparator);
            for (File file : gifFiles) {
                screenshotInfoses.add(new ScreenshotInfos(file.getAbsolutePath()));
            }
        }
        return screenshotInfoses;
    }

    /*
     * 获取目录下所有文件
     */
    public static List<File> getFiles(String realpath, List<File> files) {
        if (TextUtils.isEmpty(realpath))
            return files;

        File realFile = new File(realpath);
        if (!realFile.isDirectory())
            return files;

        File[] subfiles = realFile.listFiles();
        for (File file : subfiles) {
            if (file.isDirectory()) {
                getFiles(file.getAbsolutePath(), files);
            } else {
                String name = file.getName();

                int i = name.indexOf('.');
                if (i != -1) {
                    name = name.substring(i);
                    if (name.equalsIgnoreCase(PNG)) {
                        files.add(file);
                    }
                }
            }
        }

        return files;
    }

    public String getName() {
        return mScreenshotName;
    }

    public String getPath() {
        return mScreenshotPath;
    }

    public String getLastModifyTime() {
        return mLastModifyTime;
    }

    public String getFileSize() {
        return mFileSize;
    }

    private void parseFromPath(String gifPath) {
        File f = new File(gifPath);
        String name = f.getName();
        int length = name.length();
        mScreenshotName = f.getName().substring(0, length - 4);
        DateFormat formatter = new SimpleDateFormat("yyyyMMdd-HH:mm:ss");
        Date date = new Date(f.lastModified());
        mLastModifyTime = formatter.format(date);
        mFileSize = formatFileSize(f.length());
    }

    private String formatFileSize(long filesize) {
        return Formatter.formatFileSize(App.getContext(), filesize);
    }
}
