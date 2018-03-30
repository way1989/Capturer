package com.way.capture.core.screenrecord;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.way.capture.core.BaseModule;

/**
 * Created by android on 16-8-22.
 */
public class ScreenRecordModule implements BaseModule, RecordingSession.Listener {
    private static final String TAG = "ScreenRecordModule";
    private RecordingSession mRecordingSession;
    private Context mContext;
    private boolean mIsRunning;

    @Override
    public boolean isRunning() {
        return mIsRunning;
    }

    @Override
    public void onStart(Context context, String action, int resultCode, Intent data) {
        mContext = context;
        mRecordingSession = new RecordingSession(mContext, this, resultCode, data);
        mRecordingSession.showOverlay();
    }

    @Override
    public void onDestroy() {
        mRecordingSession.destroy();
        mIsRunning = false;
    }

    @Override
    public void onStart() {
        mIsRunning = true;
    }

    @Override
    public void onStop() {
        mIsRunning = false;
    }

    @Override
    public void onEnd() {
        Log.d(TAG, "Shutting down.");
        mIsRunning = false;
    }
}
