package com.way.captain.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.way.captain.App;
import com.way.captain.R;
import com.way.captain.fragment.BaseFragment;
import com.way.captain.fragment.GifFragment;
import com.way.captain.fragment.MainFragment;
import com.way.captain.fragment.VideoFragment;
import com.way.captain.utils.OsUtil;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "MainActivity";
    Runnable navigateShare = new Runnable() {
        public void run() {
            String url = "http://fir.im/captain";
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
            sharingIntent.putExtra(Intent.EXTRA_TEXT, url);
            sharingIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            Intent chooserIntent = Intent.createChooser(sharingIntent, null);
            startActivity(chooserIntent);
        }
    };
    Runnable navigateFeedback = new Runnable() {
        public void run() {
            startActivity(new Intent(MainActivity.this, FeedbackActivity.class));
        }
    };
    Runnable navigateSettings = new Runnable() {
        public void run() {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        }
    };
    Runnable navigateChangeMode = new Runnable() {
        public void run() {
            int uiMode = getResources().getConfiguration().uiMode;
            boolean isCurrentNightMode = (uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            Log.i("way", "isCurrentNightMode = " + isCurrentNightMode);
            getDelegate().setLocalNightMode(isCurrentNightMode ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES);
            PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putBoolean(App.KEY_NIGHT_MODE, !isCurrentNightMode).apply();
            recreate();
        }
    };
    private NavigationView mNavigationView;
    private DrawerLayout mDrawerLayout;
    private BaseFragment mFragment;
    Runnable navigateGifs = new Runnable() {
        public void run() {
            mNavigationView.getMenu().findItem(R.id.nav_gifs).setChecked(true);
            mFragment = new GifFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, mFragment).commitAllowingStateLoss();
        }
    };
    Runnable navigateVideos = new Runnable() {
        public void run() {
            mNavigationView.getMenu().findItem(R.id.nav_videos).setChecked(true);
            mFragment = new VideoFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.hide(getSupportFragmentManager().findFragmentById(R.id.fragment_container));
            transaction.replace(R.id.fragment_container, mFragment).commit();
        }
    };
    private long mLastPressTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (OsUtil.redirectToPermissionCheckIfNeeded(this)) {
            return;
        }
        setContentView(R.layout.activity_main);


        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        if (mNavigationView != null)
            mNavigationView.setNavigationItemSelectedListener(this);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, new MainFragment()).commitAllowingStateLoss();

        //mNavigationView.post(navigateGifs);

        MenuItem item = mNavigationView.getMenu().findItem(R.id.nav_night_mode);
        int uiMode = getResources().getConfiguration().uiMode;
        boolean isCurrentNightMode = (uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        item.setTitle(isCurrentNightMode ? R.string.nav_day_mode : R.string.nav_night_mode);
        Log.i("way", "onCreate isCurrentNightMode = " + isCurrentNightMode);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
//            if (mFragment != null && mFragment.onBackPressed())
//                return;
//            long time = System.currentTimeMillis();
//            if (time - mLastPressTime > 3000) {
//                Snackbar.make(mDrawerLayout, R.string.twice_press_exit, Snackbar.LENGTH_SHORT).show();
//                mLastPressTime = time;
//            } else
                super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                if (isNavigatingMain()) {
                    mDrawerLayout.openDrawer(GravityCompat.START);
                } else {
                    super.onBackPressed();
                }
                return true;
            case R.id.action_settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isNavigatingMain() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        return (currentFragment instanceof MainFragment);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        switch (id) {
            case R.id.nav_screenshot:
                item.setChecked(true);
                break;
            case R.id.nav_gifs:
                item.setChecked(true);
                break;
            case R.id.nav_videos:
                item.setChecked(true);
                break;
            case R.id.nav_night_mode:
                mNavigationView.post(navigateChangeMode);
                break;
            case R.id.nav_share:
                mNavigationView.post(navigateShare);
                break;
            case R.id.nav_feedback:
                mNavigationView.post(navigateFeedback);
                break;
            case R.id.nav_settings:
                mNavigationView.post(navigateSettings);
                break;
            default:
                break;
        }
        mDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}
