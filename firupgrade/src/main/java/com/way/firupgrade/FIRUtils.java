package com.way.firupgrade;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import im.fir.sdk.FIR;
import im.fir.sdk.VersionCheckCallback;

public class FIRUtils {

    public final static void checkForUpdate(final Activity context, final boolean isShowToast) {
        if (context == null)
            return;
        String api_token = context.getResources().getString(R.string.api_token);
        if (TextUtils.isEmpty(api_token))
            throw new NullPointerException("api_token must not null");

        final Resources resources = context.getResources();
        final ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(true);
        progressDialog.setTitle(R.string.progress_dialog_title);
        progressDialog.setMessage(resources.getString(R.string.progress_dialog_message));

        FIR.checkForUpdateInFIR(api_token, new VersionCheckCallback() {

            @SuppressLint("StringFormatInvalid")
            @Override
            public void onSuccess(String versionJson) {
                final AppVersion appVersion = getAppVersion(versionJson);
                if (appVersion == null) {
                    Toast.makeText(context, R.string.latest_version, Toast.LENGTH_SHORT).show();
                    return;
                }
                int appVersionCode = getVersionCode(context);
                String appVersionName = getVersionName(context);
                if (appVersionCode != appVersion.getVersionCode()
                        && !TextUtils.equals(appVersionName, appVersion.getVersionName())) {
                    new AlertDialog.Builder(context).setTitle(resources.getString(R.string.new_version_dialog_title, appVersion.getVersionName()))
                            .setMessage(resources.getString(R.string.new_version_dialog_message, appVersion.getChangeLog()))
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    // when download complete, broadcast will be sent to
                                    // receiver
                                    DownloadUtils.DownloadApkWithProgress(context, appVersion);
                                }
                            }).setNegativeButton(android.R.string.cancel, null).create().show();
                } else {
                    if (isShowToast)
                        Toast.makeText(context, R.string.latest_version, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onStart() {
                if (isShowToast && progressDialog != null && !progressDialog.isShowing())
                    progressDialog.show();
            }

            @Override
            public void onFinish() {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }

            @Override
            public void onFail(Exception exception) {
                if (isShowToast)
                    Toast.makeText(context, R.string.net_error, Toast.LENGTH_SHORT).show();
            }

        });
    }

    private static AppVersion getAppVersion(String versionJson) {
        if (TextUtils.isEmpty(versionJson))
            return null;
        try {
            JSONObject jsonObject = new JSONObject(versionJson);
            String versionName = jsonObject.getString("versionShort");
            int versionCode = jsonObject.getInt("version");
            String changeLog = jsonObject.getString("changelog");
            String updateUrl = jsonObject.getString("install_url");
            long fileSize = jsonObject.getJSONObject("binary").getInt("fsize");
            long updateTime = jsonObject.getLong("updated_at");
            AppVersion appVersion = new AppVersion(versionCode, versionName, changeLog,
                    updateUrl, fileSize, updateTime);
            return appVersion;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取版本号
     *
     * @param context
     * @return
     */
    public static int getVersionCode(Context context) {
        int versionCode = -1;
        try {
            PackageInfo info = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            versionCode = info.versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionCode;
    }

    /**
     * 获取版本信息
     *
     * @param context
     * @return
     */
    public static String getVersionName(Context context) {
        String versionName = null;
        try {
            PackageInfo info = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            versionName = info.versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionName;
    }

    /**
     * 获取版本信息
     *
     * @param context
     * @return
     */
    public static String getAppName(Context context) {
        String appName = null;
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = packageManager
                    .getApplicationInfo(context.getPackageName(), 0);
            appName = packageManager.getApplicationLabel(applicationInfo).toString();
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return appName;
    }
}
