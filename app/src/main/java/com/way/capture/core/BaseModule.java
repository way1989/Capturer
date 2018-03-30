package com.way.capture.core;

import android.content.Context;
import android.content.Intent;

/**
 * Created by android on 16-8-22.
 */
public interface BaseModule {

    boolean isRunning();

    void onStart(Context context, String action, int resultCode, Intent data);

    void onDestroy();
}
