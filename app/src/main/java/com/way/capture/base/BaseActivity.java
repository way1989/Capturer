package com.way.capture.base;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDelegate;

import com.trello.rxlifecycle.components.support.RxAppCompatActivity;
import com.way.capture.App;
import com.way.capture.utils.OsUtil;

import butterknife.ButterKnife;

/**
 * Created by android on 16-3-5.
 */
public abstract class BaseActivity extends RxAppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean isNightMode = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(App.KEY_NIGHT_MODE, false);
        AppCompatDelegate.setDefaultNightMode(isNightMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        if (OsUtil.redirectToPermissionCheckIfNeeded(this)) return;

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
