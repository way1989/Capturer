package com.way.captain.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.way.captain.App;
import com.way.captain.R;
import com.way.captain.fragment.BaseFragment;
import com.way.captain.fragment.GifFragment;
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
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.GONE);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // if (mFragment != null && mFragment.onFloatButtonClick())
                //     return;
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
            }
        });
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.setDrawerListener(toggle);
        toggle.syncState();

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);
        mNavigationView.post(navigateGifs);

        MenuItem item = mNavigationView.getMenu().findItem(R.id.nav_night_mode);
        int uiMode = getResources().getConfiguration().uiMode;
        boolean isCurrentNightMode = (uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        item.setTitle(isCurrentNightMode ? R.string.nav_day_mode : R.string.nav_night_mode);
        Log.i("way", "onCreate isCurrentNightMode = " + isCurrentNightMode);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            if (mFragment != null && mFragment.onBackPressed())
                return;
            long time = System.currentTimeMillis();
            if (time - mLastPressTime > 3000) {
                Snackbar.make(mDrawerLayout, R.string.twice_press_exit, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                mLastPressTime = time;
            } else
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_gifs) {
            if (mFragment == null || !(mFragment instanceof GifFragment))
                mNavigationView.postDelayed(navigateGifs, 350);
        } else if (id == R.id.nav_videos) {
            if (mFragment == null || !(mFragment instanceof VideoFragment))
                mNavigationView.postDelayed(navigateVideos, 350);
        } else if(id == R.id. nav_night_mode) {
            int uiMode = getResources().getConfiguration().uiMode;
            boolean isCurrentNightMode = (uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            Log.i("way", "isCurrentNightMode = " + isCurrentNightMode);
            getDelegate().setLocalNightMode(isCurrentNightMode ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES);
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(App.KEY_NIGHT_MODE, !isCurrentNightMode).apply();
            recreate();
        } else if (id == R.id.nav_share) {
            mNavigationView.postDelayed(navigateShare, 350);
        } else if (id == R.id.nav_feedback) {
            mNavigationView.postDelayed(navigateFeedback, 350);
        } else if (id == R.id.nav_settings) {
            mNavigationView.postDelayed(navigateSettings, 350);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
