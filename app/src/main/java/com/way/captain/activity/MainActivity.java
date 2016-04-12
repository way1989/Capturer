package com.way.captain.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.way.captain.App;
import com.way.captain.R;
import com.way.captain.data.DataInfo;
import com.way.captain.fragment.BaseFragment;
import com.way.captain.fragment.GifFragment;
import com.way.captain.fragment.MainFragment;
import com.way.captain.fragment.ScreenshotFragment;
import com.way.captain.fragment.VideoFragment;
import com.way.captain.service.ShakeService;
import com.way.captain.utils.OsUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener,
        ViewPager.OnPageChangeListener {
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
    ViewPager mViewPager;
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
    private Bundle mTmpReenterState;
    private Adapter mAdapter;
    private final SharedElementCallback mCallback = new SharedElementCallback() {
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            Log.i("way", "SharedElementCallback-->onMapSharedElements mTmpReenterState = " + mTmpReenterState);
            if (mTmpReenterState != null) {
                int startingPosition = mTmpReenterState.getInt(ScreenshotFragment.EXTRA_STARTING_POSITION);
                int currentPosition = mTmpReenterState.getInt(ScreenshotFragment.EXTRA_CURRENT_POSITION);
                if (startingPosition != currentPosition) {
                    // If startingPosition != currentPosition the user must have swiped to a
                    // different page in the DetailsActivity. We must update the shared element
                    // so that the correct one falls into place.
                    BaseFragment fragment = mAdapter.getItem(mViewPager.getCurrentItem());
                    fragment.changeSharedElements(names, sharedElements, currentPosition);
                }

                mTmpReenterState = null;
            } else {
                // If mTmpReenterState is null, then the activity is exiting.
                View navigationBar = findViewById(android.R.id.navigationBarBackground);
                View statusBar = findViewById(android.R.id.statusBarBackground);
                if (navigationBar != null) {
                    names.add(navigationBar.getTransitionName());
                    sharedElements.put(navigationBar.getTransitionName(), navigationBar);
                }
                if (statusBar != null) {
                    names.add(statusBar.getTransitionName());
                    sharedElements.put(statusBar.getTransitionName(), statusBar);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (OsUtil.redirectToPermissionCheckIfNeeded(this)) {
            return;
        }
        setContentView(R.layout.activity_main);
        setExitSharedElementCallback(mCallback);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null)
            fab.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    startService(new Intent(MainActivity.this, ShakeService.class).setAction("com.way.action.SHOW_MENU"));
                }
            });


        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.setDrawerListener(toggle);
        toggle.syncState();

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        if (mNavigationView != null)
            mNavigationView.setNavigationItemSelectedListener(this);

        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        if (mViewPager != null) {
            setupViewPager(mViewPager);
            mViewPager.setOffscreenPageLimit(2);
            mViewPager.addOnPageChangeListener(this);
        }

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        if (tabLayout != null)
            tabLayout.setupWithViewPager(mViewPager);
        //FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        //transaction.replace(R.id.fragment_container, new MainFragment()).commitAllowingStateLoss();

        //mNavigationView.post(navigateGifs);

        MenuItem item = mNavigationView.getMenu().findItem(R.id.nav_night_mode);
        int uiMode = getResources().getConfiguration().uiMode;
        boolean isCurrentNightMode = (uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        item.setTitle(isCurrentNightMode ? R.string.nav_day_mode : R.string.nav_night_mode);
        Log.i("way", "onCreate isCurrentNightMode = " + isCurrentNightMode);
    }

    @Override
    public void onActivityReenter(int resultCode, Intent data) {
        super.onActivityReenter(resultCode, data);
        if (mAdapter != null && mViewPager != null) {
            mTmpReenterState = new Bundle(data.getExtras());
            BaseFragment fragment = mAdapter.getItem(mViewPager.getCurrentItem());
            fragment.onActivityReenter(mTmpReenterState);
        }
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
                Snackbar.make(mDrawerLayout, R.string.twice_press_exit, Snackbar.LENGTH_SHORT).show();
                mLastPressTime = time;
            } else
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
                mViewPager.setCurrentItem(0, true);
                break;
            case R.id.nav_gifs:
                item.setChecked(true);
                mViewPager.setCurrentItem(1, true);
                break;
            case R.id.nav_videos:
                item.setChecked(true);
                mViewPager.setCurrentItem(2, true);
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

    private void setupViewPager(ViewPager viewPager) {
        mAdapter = new Adapter(getSupportFragmentManager());
        mAdapter.addFragment(ScreenshotFragment.newInstance(DataInfo.TYPE_SCREEN_SHOT), this.getString(R.string.screen_shot));
        mAdapter.addFragment(ScreenshotFragment.newInstance(DataInfo.TYPE_SCREEN_GIF), this.getString(R.string.gif_title));
        mAdapter.addFragment(ScreenshotFragment.newInstance(DataInfo.TYPE_SCREEN_RECORD), this.getString(R.string.video_title));
        viewPager.setAdapter(mAdapter);
        mNavigationView.getMenu().findItem(R.id.nav_screenshot).setChecked(true);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        switch (position) {
            case 0:
                mNavigationView.getMenu().findItem(R.id.nav_screenshot).setChecked(true);
                break;
            case 1:
                mNavigationView.getMenu().findItem(R.id.nav_gifs).setChecked(true);
                break;
            case 2:
                mNavigationView.getMenu().findItem(R.id.nav_videos).setChecked(true);
                break;
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    class Adapter extends FragmentPagerAdapter {
        private final List<BaseFragment> mFragments = new ArrayList<>();
        private final List<String> mFragmentTitles = new ArrayList<>();

        public Adapter(FragmentManager fm) {
            super(fm);
        }

        public void addFragment(BaseFragment fragment, String title) {
            mFragments.add(fragment);
            mFragmentTitles.add(title);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            mFragment = (BaseFragment) object;
        }

        @Override
        public BaseFragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitles.get(position);
        }
    }
}
