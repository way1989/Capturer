package com.way.captain;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatDelegate;

import com.bugtags.library.Bugtags;
import com.way.captain.fragment.SettingsFragment;
import com.way.captain.service.ShakeService;

//import com.squareup.leakcanary.LeakCanary;

/**
 * Created by android on 16-2-4.
 */
public class App extends Application {
    public static final String KEY_NIGHT_MODE = "night_mode_key";
    private static Context mContext;

    public static Context getContext() {
        return mContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
//        BTGInvocationEventNone    // 静默模式，只收集 Crash 信息（如果允许）
//        BTGInvocationEventShake   // 通过摇一摇呼出 Bugtags
//        BTGInvocationEventBubble  // 通过悬浮小球呼出 Bugtags
        //BugtagsOptions options = new BugtagsOptions.Builder().trackingCrashLog(true).build();
        if (!BuildConfig.DEBUG)
            Bugtags.start(getString(R.string.bugtag_app_key), this, Bugtags.BTGInvocationEventNone);
        mContext = this;
        boolean isNightMode = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(KEY_NIGHT_MODE, false);
        AppCompatDelegate.setDefaultNightMode(isNightMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
//        LeakCanary.install(this);
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsFragment.ATOUCH_KEY, true))
            startService(new Intent(this, ShakeService.class));
    }
}
