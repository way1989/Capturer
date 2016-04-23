package com.way.capture;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

import com.way.capture.fragment.SettingsFragment;
import com.way.capture.service.ShakeService;

/**
 * Created by android on 16-2-4.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SettingsFragment.SHAKE_KEY, true)
                    && PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SettingsFragment.BOOT_AUTO_KEY, true))
                context.startService(new Intent(context, ShakeService.class));
        }

    }
}
