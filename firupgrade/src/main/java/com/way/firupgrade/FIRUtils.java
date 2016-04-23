package com.way.firupgrade;

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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class FIRUtils {
    private static final String mBaseUrl = "http://api.fir.im/apps/latest/%1$s?api_token=%2$s";

    public final static void checkForUpdate(final Activity context, final boolean isShowToast) {
        if (context == null)
            return;
        if (Preferences.getLastCheckTime(context) - System.currentTimeMillis() < 12 * 60 * 60 * 1000 && !isShowToast) {
            return;
        }
        final String api_token = context.getResources().getString(R.string.api_token);
        final String app_id = context.getResources().getString(R.string.app_id);

        if (TextUtils.isEmpty(api_token) || TextUtils.isEmpty(app_id))
            throw new NullPointerException("api_token or app_id must not null");
        final String url = String.format(mBaseUrl, app_id, api_token);
        if (TextUtils.isEmpty(url))
            throw new NullPointerException("url must not null");
        Log.i("broncho", "url = " + url);
        final Resources resources = context.getResources();
        final ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(true);
        progressDialog.setTitle(R.string.progress_dialog_title);
        progressDialog.setMessage(resources.getString(R.string.progress_dialog_message));
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        JsonObjectRequest jr = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.i("broncho", "response = " + response);
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                Preferences.setLastCheckTime(context, System.currentTimeMillis());

                final AppVersion appVersion = getAppVersion(response);
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


        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i("broncho", "error = " + error);
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                if (isShowToast)
                    Toast.makeText(context, R.string.net_error, Toast.LENGTH_SHORT).show();
            }
        });
        requestQueue.add(jr);
        if (isShowToast && progressDialog != null && !progressDialog.isShowing())
            progressDialog.show();

    }

    private static AppVersion getAppVersion(JSONObject versionJson) {

        try {
            String versionName = versionJson.getString("versionShort");
            int versionCode = versionJson.getInt("version");
            String changeLog = versionJson.getString("changelog");
            String updateUrl = versionJson.getString("install_url");
            long fileSize = versionJson.getJSONObject("binary").getInt("fsize");
            long updateTime = versionJson.getLong("updated_at");
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
