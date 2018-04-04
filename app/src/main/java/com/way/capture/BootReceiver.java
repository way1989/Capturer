package com.way.capture;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.way.capture.service.ShakeService;

/**
 * Created by android on 16-2-4.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (TextUtils.equals(Intent.ACTION_BOOT_COMPLETED, intent.getAction())) {
            context.startService(new Intent(context, ShakeService.class));
        }

    }
}
