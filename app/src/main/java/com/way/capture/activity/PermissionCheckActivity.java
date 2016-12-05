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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import com.way.capture.R;
import com.way.capture.utils.OsUtil;

/**
 * Activity to check if the user has required permissions. If not, it will try to prompt the user
 * to grant permissions. However, the OS may not actually prompt the user if the user had
 * previously checked the "Never ask again" checkbox while denying the required permissions.
 */
public class PermissionCheckActivity extends AppCompatActivity implements OnClickListener {
    private static final int REQUIRED_PERMISSIONS_REQUEST_CODE = 1;
    private static final long AUTOMATED_RESULT_THRESHOLD_MILLLIS = 250;
    private static final String PACKAGE_URI_PREFIX = "package:";
    private static final String TAG = "PermissionCheckActivity";
    private static final int REQUEST_CODE = 2;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (redirectIfNeeded()) {
            return;
        }
        //showRequestPermissionDialog();
        tryRequestPermission();
    }

    private void showRequestPermissionDialog() {
        new AlertDialog.Builder(this).setTitle(R.string.permission_title)
                .setMessage(R.string.required_permissions_promo)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        tryRequestPermission();
                    }
                }).show();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (redirectIfNeeded()) {
            return;
        }
    }

    private void tryRequestPermission() {
        final String[] missingPermissions = OsUtil.getMissingRequiredPermissions();
        if (missingPermissions.length == 0) {
            checkcanDrawOverlays();
            return;
        }

        ActivityCompat.requestPermissions(this, missingPermissions, REQUIRED_PERMISSIONS_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(
            final int requestCode, final String permissions[], final int[] grantResults) {
        if (requestCode == REQUIRED_PERMISSIONS_REQUEST_CODE) {
            final int length = grantResults.length;
            for (int i = 0; i < length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    // Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[i])) {
                        //Show permission explanation dialog...
                        finish();
                        return;
                    } else {
                        //Never ask again selected, or device policy prohibits the app from having that permission.
                        //So, disable that feature, or fall back to another situation...
                        gotoSettings();
                        return;
                    }
                }
            }
            Log.v(TAG, "request permission success");
            checkcanDrawOverlays();
        }
    }

    private void checkcanDrawOverlays() {
        if (OsUtil.canDrawOverlays(this)) {
            redirect();
        } else {
            requestAlertWindowPermission();
        }
    }


    private void requestAlertWindowPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (OsUtil.canDrawOverlays(this)) {
                Log.i(TAG, "onActivityResult granted");
                redirect();
            } else {
                finish();
            }
        }
    }

    private void gotoSettings() {
        new AlertDialog.Builder(PermissionCheckActivity.this).setTitle(R.string.permission_title)
                .setMessage(R.string.enable_permission_procedure)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse(PACKAGE_URI_PREFIX + getPackageName()));
                        startActivity(intent);
                    }
                }).show();
    }

    /**
     * Returns true if the redirecting was performed
     */
    private boolean redirectIfNeeded() {
        if (!OsUtil.canDrawOverlays(this))
            return false;
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
