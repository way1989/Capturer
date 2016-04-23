/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.way.capture.activity;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.way.capture.R;
import com.way.capture.utils.OsUtil;

/**
 * Activity to check if the user has required permissions. If not, it will try to prompt the user
 * to grant permissions. However, the OS may not actually prompt the user if the user had
 * previously checked the "Never ask again" checkbox while denying the required permissions.
 */
public class PermissionCheckActivity extends BaseActivity implements OnClickListener {
    private static final int REQUIRED_PERMISSIONS_REQUEST_CODE = 1;
    private static final long AUTOMATED_RESULT_THRESHOLD_MILLLIS = 250;
    private static final String PACKAGE_URI_PREFIX = "package:";
    private long mRequestTimeMillis;
    private TextView mNextView;
    private TextView mSettingsView;
    private TextView mEnablePermissionView;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (redirectIfNeeded()) {
            return;
        }

        setContentView(R.layout.permission_check_activity);
        View exit = findViewById(R.id.exit);
        if (exit != null) exit.setOnClickListener(this);

        mNextView = (TextView) findViewById(R.id.next);
        if (mNextView != null) mNextView.setOnClickListener(this);

        mSettingsView = (TextView) findViewById(R.id.settings);
        if (mSettingsView != null) mSettingsView.setOnClickListener(this);
        mEnablePermissionView = (TextView) findViewById(R.id.enable_permission_procedure);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (redirectIfNeeded()) {
            return;
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void tryRequestPermission() {
        final String[] missingPermissions = OsUtil.getMissingRequiredPermissions();
        if (missingPermissions.length == 0) {
            redirect();
            return;
        }

        mRequestTimeMillis = SystemClock.elapsedRealtime();
        requestPermissions(missingPermissions, REQUIRED_PERMISSIONS_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(
            final int requestCode, final String permissions[], final int[] grantResults) {
        if (requestCode == REQUIRED_PERMISSIONS_REQUEST_CODE) {
            // We do not use grantResults as some of the granted permissions might have been
            // revoked while the permissions dialog box was being shown for the missing permissions.
            if (OsUtil.hasRequiredPermissions()) {
                //Factory.get().onRequiredPermissionsAcquired();
                redirect();
            } else {
                final long currentTimeMillis = SystemClock.elapsedRealtime();
                // If the permission request completes very quickly, it must be because the system
                // automatically denied. This can happen if the user had previously denied it
                // and checked the "Never ask again" check box.
                if ((currentTimeMillis - mRequestTimeMillis) < AUTOMATED_RESULT_THRESHOLD_MILLLIS) {
                    mNextView.setVisibility(View.GONE);

                    mSettingsView.setVisibility(View.VISIBLE);
                    mEnablePermissionView.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    /**
     * Returns true if the redirecting was performed
     */
    private boolean redirectIfNeeded() {
        if (!OsUtil.hasRequiredPermissions()) {
            return false;
        }

        redirect();
        return true;
    }

    private void redirect() {
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
        finish();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.exit:
                finish();
                break;
            case R.id.next:
                tryRequestPermission();
                break;
            case R.id.settings:
                final Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse(PACKAGE_URI_PREFIX + getPackageName()));
                startActivity(intent);
                break;
        }
    }
}
