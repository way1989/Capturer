package com.way.capture.module;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.os.StatFs;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.way.capture.BuildConfig;
import com.way.capture.R;
import com.way.capture.module.screenrecord.ScreenRecordModule;
import com.way.capture.module.screenshot.ScreenshotModule;

public class ModuleService extends Service {
    private static final String TAG = "ModuleService";
    private static final String EXTRA_RESULT_CODE = "result-code";
    private static final String EXTRA_DATA = "data";
    private boolean mIsRunning;
    private BaseModule mCurrentModulel;

    public static Intent newIntent(Context context, String action, int resultCode, Intent data) {
        Intent intent = new Intent(context, ModuleService.class);
        intent.setAction(action);
        intent.putExtra(EXTRA_RESULT_CODE, resultCode);
        intent.putExtra(EXTRA_DATA, data);
        return intent;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    private boolean hasAvailableSpace() {
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long bytesAvailable = stat.getBlockSizeLong() * stat.getAvailableBlocksLong();
        long megAvailable = bytesAvailable / 1048576;
        return megAvailable >= 100;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mIsRunning) {
            Log.d(TAG, "Already running! Ignoring...");
            return START_NOT_STICKY;
        }
        Log.d(TAG, "Starting up!");
        mIsRunning = true;

        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!hasAvailableSpace()) {
            Toast.makeText(this, R.string.not_enough_storage, Toast.LENGTH_LONG).show();
            stopSelf();
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            throw new NullPointerException("action must not be null");
        }

        int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
        Intent data = intent.getParcelableExtra(EXTRA_DATA);
        if (resultCode == 0 || data == null) {
            throw new IllegalStateException("Result code or data missing.");
        }

        mCurrentModulel = getModuleByAction(action);
        mCurrentModulel.onStart(getApplicationContext(), action, resultCode, data);
        return START_NOT_STICKY;
    }

    private BaseModule getModuleByAction(String action) {
        BaseModule module;
        switch (action) {
            case Action.ACTION_SCREENSHOT:
            case Action.ACTION_FREE_CROP:
            case Action.ACTION_RECT_CROP:
                module = new ScreenshotModule();
                break;
            case Action.ACTION_RECORD:
                module = new ScreenRecordModule();
                break;
            default:
                module = new ScreenshotModule();
                break;
        }
        return module;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCurrentModulel != null)
            mCurrentModulel.onDestroy();
        mCurrentModulel = null;
    }

    public static final class Action {
        public static final String ACTION_SCREENSHOT = BuildConfig.APPLICATION_ID + ".SCREENSHOT";
        public static final String ACTION_RECORD = BuildConfig.APPLICATION_ID + ".RECORD";
        public static final String ACTION_FREE_CROP = BuildConfig.APPLICATION_ID + ".FREE_CROP";
        public static final String ACTION_RECT_CROP = BuildConfig.APPLICATION_ID + ".RECT_CROP";
    }
}
