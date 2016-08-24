package com.way.capture.module;

import android.content.Context;
import android.content.Intent;

/**
 * Created by android on 16-8-22.
 */
public interface BaseModule {
    void onStart(Context context, int resultCode, Intent data);
    void onDestroy();
}
