package com.way.captain.utils;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class FilesOptHelper {

    private static final String TAG = "FilesOptHelper";
    private static FilesOptHelper mHelper;

    private FilesOptHelper() {

    }

    public static FilesOptHelper getInstance() {
        if (mHelper == null) {
            mHelper = new FilesOptHelper();
        }
        return mHelper;
    }

    public void unCompressFile(String source, String target) throws Exception {
        synchronized (this) {
            Log.d(TAG, "unCompressFile(String source, String target)");
            Log.d(TAG, "source --> " + source);
            Log.d(TAG, "target --> " + target);

            ZipFile zip = new ZipFile(source);
            Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    String name = entry.getName();
                    Log.d(TAG, "entry.getName() --> " + name);
                    name = name.substring(0, name.length() - 1);
                    File f = new File(target + name);
                    f.mkdir();
                } else {
                    File f = new File(target + entry.getName());
                    Log.d(TAG, "entry.getName() --> " + entry.getName());
                    boolean cg = f.getParentFile().mkdir();
                    Log.d(TAG, "cg -->" + cg);
                    if (!f.createNewFile()) {
                        if (f.delete()) {
                            f.createNewFile();
                        }
                    }
                    InputStream is = zip.getInputStream(entry);
                    FileOutputStream fos = new FileOutputStream(f);
                    int lenght = 0;
                    byte[] b = new byte[1024];
                    while ((lenght = is.read(b, 0, 1024)) != -1) {
                        fos.write(b, 0, lenght);
                    }
                    is.close();
                    fos.close();
                }
            }
            zip.close();
        }
    }

    public boolean clearFolder(String path) throws Exception {
        File f = new File(path);
        if (!f.exists()) {
            boolean success = f.mkdir();
            Log.d(TAG, "make new folder(" + path + ") --> " + success);
            return success;
        }

        for (File item : f.listFiles()) {
            doDeleteFiles(item.getAbsolutePath());
        }

        return true;
    }

    /***
     * If it is a folder, delete all files and directories in folder.
     * or delete it.
     *
     * @param path
     * @return success
     */
    private boolean doDeleteFiles(String path) throws Exception {
        synchronized (this) {
            Log.d(TAG, "doDeleteFiles(String path)");
            Log.d(TAG, "path --> " + path);
            File dir = new File(path);
            if (dir.isDirectory()) {
                String[] files = dir.list();
                if (files != null && files.length != 0) {
                    for (int i = 0; i < files.length; i++) {
                        Log.d(TAG, "files[" + i + "]" + " --> " + files[i]);
                        File f = new File(path, files[i]);
                        boolean success = doDeleteFiles(f.getAbsolutePath());
                        if (!success) {
                            return false;
                        }
                    }
                    dir.delete();
                }
                return true;
            } else {
                return dir.delete();
            }
        }
    }

}
