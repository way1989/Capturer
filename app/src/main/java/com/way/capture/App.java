package com.way.capture;

import android.app.Application;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatDelegate;

import com.bumptech.glide.Glide;
import com.squareup.leakcanary.LeakCanary;
import com.tencent.bugly.crashreport.CrashReport;
import com.way.capture.service.ShakeService;
import com.way.downloadlibrary.WDMSharPre;

/**
 * Created by android on 16-2-4.
 */
public class App extends Application {
    public static final String KEY_NIGHT_MODE = "night_mode_key";
    private static Application mApplication;

    public static Application getContext() {
        return mApplication;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mApplication = this;
        //Bugly
        CrashReport.initCrashReport(this, BuildConfig.BUGLY_APPID, false);

        //LeakCanary
        LeakCanary.install(this);

        //night mode
        boolean isNightMode = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(KEY_NIGHT_MODE, false);
        AppCompatDelegate.setDefaultNightMode(isNightMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        //Download library
        WDMSharPre.init(getApplicationContext());
        startService(new Intent(this, ShakeService.class));
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Glide.with(this).onTrimMemory(level);
    }
}
