package com.way.capture.activity;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.view.MotionEvent;

import com.trello.rxlifecycle.components.support.RxAppCompatActivity;
import com.way.capture.App;
import com.way.capture.BuildConfig;

import butterknife.ButterKnife;
//import com.way.firupgrade.FIRUtils;

/**
 * Created by android on 16-3-5.
 */
public abstract class BaseActivity extends RxAppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean isNightMode = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(App.KEY_NIGHT_MODE, false);
        AppCompatDelegate.setDefaultNightMode(isNightMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
//        if (!BuildConfig.DEBUG)
//            FIRUtils.checkForUpdate(this, false);
        setContentView(getContentView());

        initWindow();
        ButterKnife.bind(this);
        initWidget();
        initData();
    }

    protected abstract int getContentView();

    protected void initWindow() {
    }

    protected void initWidget() {
    }

    protected void initData() {
    }
}
