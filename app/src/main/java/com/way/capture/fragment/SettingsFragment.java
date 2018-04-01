package com.way.capture.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;

import com.thefinestartist.finestwebview.FinestWebView;
import com.way.capture.R;
import com.way.capture.service.ShakeService;
import com.way.capture.utils.AppUtil;
import com.way.capture.utils.ScrollUtils;
//import com.way.firupgrade.FIRUtils;

/**
 * Created by android on 16-2-4.
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String VIDEO_SIZE_KEY = "key_video_size_percentage";
    public static final String VIDEO_QUALITY_KEY = "key_video_quality";
    public static final String VIDEO_FRAME_KEY = "key_video_frame";
    public static final String VIDEO_AUDIO_KEY = "key_record_audio";
    public static final String VIDEO_STOP_METHOD_KEY = "key_video_stop_method";
    public static final String SHOW_COUNTDOWN_KEY = "key_three_second_countdown";
    public static final String SHOW_TOUCHES_KEY = "key_show_touches";
    public static final String SHAKE_KEY = "key_use_atouch";
    public static final String BOOT_AUTO_KEY = "key_boot_atuo";
    public static final String SCREENSHOT_SOUND = "key_screenshot_sound";
    public static final String LONG_SCREENSHOT_AUTO = "key_long_screenshot_auto";
    private static final String VERSION_KEY = "key_version";
    private Activity mContext;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        mContext = getActivity();
        PackageManager packageManager = mContext.getPackageManager();
        String packageName = mContext.getPackageName();
        // Update the version number
        try {
            final PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            findPreference(VERSION_KEY).setSummary(packageInfo.versionName);
        } catch (final PackageManager.NameNotFoundException e) {
            findPreference(VERSION_KEY).setSummary("?");
        }
        ListPreference sizeListPreference = (ListPreference) findPreference(VIDEO_SIZE_KEY);
        sizeListPreference.setSummary(sizeListPreference.getEntry());
        if (AppUtil.isMarshmallow()) {
            PreferenceCategory preferenceScreen = (PreferenceCategory) findPreference("key_advance_category");
            preferenceScreen.removePreference(findPreference(SHOW_TOUCHES_KEY));
        }
        if (!ScrollUtils.isDeviceRoot()) {
            PreferenceCategory preferenceScreen = (PreferenceCategory) findPreference("key_advance_category");
            preferenceScreen.removePreference(findPreference(LONG_SCREENSHOT_AUTO));
        }
        PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();
        switch (key) {
            case SHAKE_KEY:
                SwitchPreference switchPreference = (SwitchPreference) preference;
                if (switchPreference.isChecked())
                    mContext.startService(new Intent(mContext, ShakeService.class));
                else
                    mContext.stopService(new Intent(mContext, ShakeService.class));
                break;
            case "key_version":
                //if(true) throw new NullPointerException("test");
//                FIRUtils.checkForUpdate(getActivity(), true);
                break;
            case "about_author":
                new FinestWebView.Builder(getActivity())
                        .titleDefault(getString(R.string.settings_self_title))
                        .show("https://github.com/way1989");
                break;
            default:
                break;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case VIDEO_SIZE_KEY:
                ListPreference listPreference = (ListPreference) findPreference(key);
                listPreference.setSummary(listPreference.getEntry());
                break;
        }
    }
}
