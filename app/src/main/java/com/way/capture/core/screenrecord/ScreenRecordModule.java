package com.way.capture.core.screenrecord;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.way.capture.core.BaseModule;
import com.way.capture.service.ModuleService;

/**
 * Created by android on 16-8-22.
 */
public class ScreenRecordModule implements BaseModule, RecordingSession.Listener {
    private static final String TAG = "ScreenRecordModule";
    private RecordingSession mRecordingSession;
    private Context mContext;

    @Override
    public void onStart(Context context, String action, int resultCode, Intent data) {
        mContext = context;
        mRecordingSession = new RecordingSession(mContext, this, resultCode, data);
        mRecordingSession.showOverlay();
    }

    @Override
    public void onDestroy() {
        mRecordingSession.destroy();
    }

    @Override
    public void onStart() {

    }

    @Override
    public void onStop() {

    }

    @Override
    public void onEnd() {
        Log.d(TAG, "Shutting down.");
        ModuleService context = (ModuleService) mContext;
        context.stopSelf();
    }
}
