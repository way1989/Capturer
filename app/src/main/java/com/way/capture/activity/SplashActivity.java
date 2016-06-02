package com.way.capture.activity;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.way.capture.R;
import com.way.capture.utils.permission.Nammu;
import com.way.capture.utils.permission.PermissionCallback;
import com.way.capture.utils.permission.PermissionListener;

import java.util.ArrayList;
import java.util.List;

public class SplashActivity extends AppCompatActivity implements PermissionCallback {
    private static final String TAG = "SplashActivity";
    private static final ArgbEvaluator ARGB_EVALUATOR = new ArgbEvaluator();
    private static final String PACKAGE_URI_PREFIX = "package:";
    private static final int HANDLER_MESSAGE_ANIMATION = 0;
    private static final int HANDLER_MESSAGE_NEXT_ACTIVITY = 1;
    ImageView image;
    TextView title;
    View foreMask;
    View logo;
    ColorDrawable colorDrawable;
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == HANDLER_MESSAGE_ANIMATION) {
                //playAnimator();
                playColorAnimator();
            } else if (msg.what == HANDLER_MESSAGE_NEXT_ACTIVITY) {
                next();
            }
        }
    };
    private long mRequestTimeMillis;


    private void next() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.scroll_in, R.anim.scroll_out);
        finish();
    }

    private void playColorAnimator() {
        //foreMask.setAlpha(0f);//rest the foreMask alpha to 0
        foreMask.animate().alpha(0f);
        final List<Animator> animList = new ArrayList<>();
        final int toColor = getResources().getColor(R.color.colorPrimaryDark);
        //final int toColor = Color.parseColor("#1F1F1F");
        final android.view.Window window = getWindow();
        ObjectAnimator statusBarColor = ObjectAnimator.ofInt(window,
                "statusBarColor", window.getStatusBarColor(), toColor);
        statusBarColor.setEvaluator(ARGB_EVALUATOR);
        animList.add(statusBarColor);

        ObjectAnimator navigationBarColor = ObjectAnimator.ofInt(window,
                "navigationBarColor", window.getNavigationBarColor(), toColor);
        navigationBarColor.setEvaluator(ARGB_EVALUATOR);
        animList.add(navigationBarColor);

        ObjectAnimator backgroundColor = ObjectAnimator.ofObject(colorDrawable, "color", ARGB_EVALUATOR, toColor);
        animList.add(backgroundColor);

        final AnimatorSet animSet = new AnimatorSet();
        animSet.setDuration(1000L);
        animSet.playTogether(animList);
        animSet.start();

        animSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (Nammu.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                    mHandler.sendEmptyMessageDelayed(HANDLER_MESSAGE_NEXT_ACTIVITY, 500L);
                else
                    checkPermissionAndThenLoad();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        initViews();
    }
    @Override
    public void onResume() {
        super.onResume();
        Nammu.permissionCompare(new PermissionListener() {
            @Override
            public void permissionsChanged(String permissionRevoke) {
                //Toast is not needed as always either permissionsGranted() or permissionsRemoved() will be called
                //Toast.makeText(MainActivity.this, "Access revoked = " + permissionRevoke, Toast.LENGTH_SHORT).show();
                checkPermissionAndThenLoad();
            }

            @Override
            public void permissionsGranted(String permissionGranted) {
                //Toast.makeText(getContext(), "Access granted = " + permissionGranted, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void permissionsRemoved(String permissionRemoved) {
                //Toast.makeText(getContext(), "Access removed = " + permissionRemoved, Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeMessages(HANDLER_MESSAGE_ANIMATION);
        mHandler.removeMessages(HANDLER_MESSAGE_NEXT_ACTIVITY);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void initViews() {
        image = (ImageView) findViewById(R.id.image);
        title = (TextView) findViewById(R.id.title);
        foreMask = findViewById(R.id.foreMask);
        logo = findViewById(R.id.logo);
        colorDrawable = new ColorDrawable(Color.BLACK);
        image.setBackground(colorDrawable);
        mHandler.sendEmptyMessageDelayed(HANDLER_MESSAGE_ANIMATION, 900L);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Nammu.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void checkPermissionAndThenLoad() {
        //check for permission
        if (Nammu.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Log.d(TAG, "checkPermissionAndThenLoad has permission...");
            mHandler.sendEmptyMessageDelayed(HANDLER_MESSAGE_NEXT_ACTIVITY, 500L);
        } else {
            if (Nammu.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Log.d(TAG, "checkPermissionAndThenLoad shouldShowRequestPermissionRationale...");
                new AlertDialog.Builder(this).setMessage(R.string.required_permissions_promo).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        tryRequestPermission();
                    }
                }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).create().show();
            } else {
                Log.d(TAG, "checkPermissionAndThenLoad askForPermission...");
                tryRequestPermission();
            }
        }
    }

    private void tryRequestPermission() {
        Nammu.askForPermission(SplashActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE, this);
        mRequestTimeMillis = SystemClock.elapsedRealtime();
    }

    private void startSettingsPermission() {
        final Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse(PACKAGE_URI_PREFIX + getPackageName()));
        startActivity(intent);
    }
    @Override
    public void permissionGranted() {
        mHandler.sendEmptyMessageDelayed(HANDLER_MESSAGE_NEXT_ACTIVITY, 200L);
    }

    @Override
    public void permissionRefused() {
        final long currentTimeMillis = SystemClock.elapsedRealtime();
        // If the permission request completes very quickly, it must be because the system
        // automatically denied. This can happen if the user had previously denied it
        // and checked the "Never ask again" check box.
        if ((currentTimeMillis - mRequestTimeMillis) < 250L) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.enable_permission_procedure).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startSettingsPermission();
                }
            }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            }).create().show();

        } else {
            finish();
        }
    }
}
