package com.way.capture.data;

import android.media.MediaScannerConnection;
import android.net.Uri;
import android.util.Log;

import com.way.capture.App;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by android on 16-2-16.
 */
public class DataProvider {
    private ArrayList<String> mData;
    private ArrayList<String> mDeleteData;
    private String mLastRemovedData;
    private int mLastRemovedPosition = -1;

    public DataProvider() {
        mData = new ArrayList<>();
        mDeleteData = new ArrayList<>();
    }

    public ArrayList<String> getData() {
        return mData;
    }

    public void setData(ArrayList<String> datas) {
        if (datas == null || datas.isEmpty())
            return;
        mData.clear();
        mData.addAll(datas);
    }

    public int getCount() {
        return mData.size();
    }

    public String getItem(int index) {
        if (index < 0 || index >= getCount()) {
            throw new IndexOutOfBoundsException("index = " + index);
        }

        return mData.get(index);
    }

    public int undoLastRemoval() {
        if (mLastRemovedData != null) {
            int insertedPosition;
            if (mLastRemovedPosition >= 0 && mLastRemovedPosition < mData.size()) {
                insertedPosition = mLastRemovedPosition;
            } else {
                insertedPosition = mData.size();
            }

            mData.add(insertedPosition, mLastRemovedData);
            if (mDeleteData.contains(mLastRemovedData))
                mDeleteData.remove(mLastRemovedData);

            mLastRemovedData = null;
            mLastRemovedPosition = -1;

            return insertedPosition;
        } else {
            return -1;
        }
    }

    public boolean deleteLastRemoval() {
        if (mLastRemovedData != null && !mDeleteData.isEmpty()) {
            for (String info : mDeleteData) {
                File file = new File(info);
                if (file.exists())
                    file.delete();
                MediaScannerConnection.scanFile(App.getContext(), new String[]{info},
                        null, new MediaScannerConnection.OnScanCompletedListener() {
                            @Override
                            public void onScanCompleted(String path, Uri uri) {
                                Log.d("way", "Media scanner completed.");
                            }
                        });
            }
            mLastRemovedData = null;
            mLastRemovedPosition = -1;
            mDeleteData.clear();
            return true;
        } else {
            return false;
        }
    }

    public void moveItem(int fromPosition, int toPosition) {
        if (fromPosition == toPosition) {
            return;
        }

        final String item = mData.remove(fromPosition);

        mData.add(toPosition, item);
        mLastRemovedPosition = -1;
    }

    public void removeItem(int position) {
        //noinspection UnnecessaryLocalVariable
        final String removedItem = mData.remove(position);
        mDeleteData.add(removedItem);
        mLastRemovedData = removedItem;
        mLastRemovedPosition = position;
    }

    public void clear() {
        mData.clear();
    }

    public void addData(String path) {
        final List<String> tmps = new ArrayList<>(mData);
        mData.clear();
        mData.add(path);
        mData.addAll(tmps);
    }
}
