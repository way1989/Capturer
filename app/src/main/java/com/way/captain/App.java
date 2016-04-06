package com.way.captain;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

import com.bugtags.library.Bugtags;
import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.fonts.MaterialModule;
import com.squareup.leakcanary.LeakCanary;
import com.way.captain.fragment.SettingsFragment;
import com.way.captain.service.ShakeService;

/**
 * Created by android on 16-2-4.
 */
public class App extends Application {
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
        if (BuildConfig.BUGTAG_ENABLED)
            Bugtags.start(getString(R.string.bugtag_app_key), this, BuildConfig.DEBUG ? Bugtags.BTGInvocationEventBubble : Bugtags.BTGInvocationEventNone);
        mContext = this;
        LeakCanary.install(this);
        Iconify.with(new MaterialModule());
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsFragment.ATOUCH_KEY, true))
            startService(new Intent(this, ShakeService.class));
    }
}
