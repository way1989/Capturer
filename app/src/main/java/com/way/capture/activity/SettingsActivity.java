package com.way.capture.activity;

import android.os.Bundle;
import android.view.MenuItem;

import com.way.capture.R;
import com.way.capture.base.BaseActivity;
import com.way.capture.fragment.SettingsFragment;
import com.way.capture.utils.ViewUtils;

/**
 * Created by android on 16-2-4.
 */
public class SettingsActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewUtils.setStatusBarStyle(this, false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().replace(R.id.settings_fragment, new SettingsFragment()).commit();
        }
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_settings_layout;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
