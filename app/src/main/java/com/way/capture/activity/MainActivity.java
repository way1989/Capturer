package com.way.capture.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.thefinestartist.finestwebview.FinestWebView;
import com.way.capture.App;
import com.way.capture.R;
import com.way.capture.data.DataInfo;
import com.way.capture.fragment.BaseFragment;
import com.way.capture.fragment.ScreenshotFragment;
import com.way.capture.service.ShakeService;
import com.way.capture.utils.AppUtils;
import com.way.capture.utils.OsUtil;
import com.way.tourguide.Overlay;
import com.way.tourguide.Pointer;
import com.way.tourguide.ToolTip;
import com.way.tourguide.TourGuide;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener,
        ViewPager.OnPageChangeListener {
    private static final String TAG = "MainActivity";
    private static final int PAGE_SCREENSHOTS = 0;
    private static final int PAGE_GIFS = 1;
    private static final int PAGE_SCREENRECORDS = 2;
    Runnable navigateShare = new Runnable() {
        public void run() {
            String url = "http://fir.im/capturer";
            url = getString(R.string.share_app, url);
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
    Runnable navigateHelp = new Runnable() {
        public void run() {
            //startActivity(new Intent(MainActivity.this, HelpActivity.class));
            new FinestWebView.Builder(MainActivity.this)
                    .titleDefault(getString(R.string.help))
                    .titleColorRes(R.color.finestWhite)
                    .urlColorRes(R.color.finestWhite)
                    .iconDefaultColorRes(R.color.finestWhite)
                    .show("https://github.com/way1989/Captain/blob/master/help/HELP_zh-rCN.md");
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
    private ViewPager mViewPager;
    private NavigationView mNavigationView;
    private DrawerLayout mDrawerLayout;
    private BaseFragment mCurrentFragment;
    private long mLastPressTime;
    private SharedPreferences mPreference;
    private FloatingActionButton mFab;
    private TourGuide mTourGuideHandler;
    private Bundle mTmpReenterState;
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
                    if (mCurrentFragment != null)
                        mCurrentFragment.changeSharedElements(names, sharedElements, currentPosition);
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
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(AppUtils.APP_FIRST_RUN, true))
            startActivity(new Intent(MainActivity.this, GuideActivity.class));
        setContentView(R.layout.activity_main);
        setExitSharedElementCallback(mCallback);

        mPreference = PreferenceManager.getDefaultSharedPreferences(this);
        initToolbar();
        initFab();
        initViewPager();
        syncNightMode();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mFab != null && mTourGuideHandler == null && mPreference.getBoolean(TAG, true)) {
            mTourGuideHandler = TourGuide.init(this).with(TourGuide.Technique.Click)
                    .setPointer(new Pointer())
                    .setToolTip(new ToolTip().setGravity(Gravity.TOP | Gravity.START)
                            .setTitle(getString(R.string.float_action_button_guide_title))
                            .setDescription(getString(R.string.float_action_button_guide_desc)))
                    .setOverlay(new Overlay()).playOn(mFab);
        }
    }

    private void initFab() {

        mFab = (FloatingActionButton) findViewById(R.id.fab);
        if (mFab != null) {
            mFab.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    if (mTourGuideHandler != null) {
                        mTourGuideHandler.cleanUp();
                        mTourGuideHandler = null;
                        mPreference.edit().putBoolean(TAG, false).apply();
                    }
                    startService(new Intent(MainActivity.this, ShakeService.class).setAction("com.way.action.SHOW_MENU"));
                }
            });
        }
    }

    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        initNavigationView(toolbar);
    }

    private void initNavigationView(Toolbar toolbar) {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.setDrawerListener(toggle);
        toggle.syncState();

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        if (mNavigationView != null)
            mNavigationView.setNavigationItemSelectedListener(this);
    }

    private void initViewPager() {
        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        if (mViewPager != null) {
            setupViewPager(mViewPager);
            mViewPager.setOffscreenPageLimit(2);
            mViewPager.addOnPageChangeListener(this);
        }

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        if (tabLayout != null)
            tabLayout.setupWithViewPager(mViewPager);
    }

    private void syncNightMode() {
        MenuItem item = mNavigationView.getMenu().findItem(R.id.nav_night_mode);
        int uiMode = getResources().getConfiguration().uiMode;
        boolean isCurrentNightMode = (uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        item.setTitle(isCurrentNightMode ? R.string.nav_day_mode : R.string.nav_night_mode);
        item.setIcon(isCurrentNightMode ? R.drawable.ic_mode_day : R.drawable.ic_mode_night);
    }

    @Override
    public void onActivityReenter(int resultCode, Intent data) {
        super.onActivityReenter(resultCode, data);
        if (mCurrentFragment != null) {
            mTmpReenterState = new Bundle(data.getExtras());
            mCurrentFragment.onActivityReenter(mTmpReenterState);
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            if (mCurrentFragment != null && mCurrentFragment.onBackPressed())
                return;
            long time = System.currentTimeMillis();
            if (time - mLastPressTime > 3000) {
                Snackbar.make(mViewPager, R.string.twice_press_exit, Snackbar.LENGTH_LONG).show();
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
            case R.id.action_settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return true;
            case R.id.action_help:
                navigateHelp.run();
                return true;
            default:

                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        switch (id) {
            case R.id.nav_screenshot:
                item.setChecked(true);
                mViewPager.setCurrentItem(PAGE_SCREENSHOTS, true);
                break;
            case R.id.nav_gifs:
                item.setChecked(true);
                mViewPager.setCurrentItem(PAGE_GIFS, true);
                break;
            case R.id.nav_videos:
                item.setChecked(true);
                mViewPager.setCurrentItem(PAGE_SCREENRECORDS, true);
                break;
            case R.id.nav_night_mode:
                mNavigationView.post(navigateChangeMode);
                break;
            case R.id.nav_share:
                mNavigationView.post(navigateShare);
                break;
            case R.id.nav_feedback:
                mNavigationView.postDelayed(navigateFeedback, 200L);
                break;
            case R.id.nav_help:
                mNavigationView.postDelayed(navigateHelp, 200L);
                break;
            case R.id.nav_settings:
                mNavigationView.postDelayed(navigateSettings, 200L);
                break;
            default:
                break;
        }
        mDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void setupViewPager(ViewPager viewPager) {
        Adapter adapter = new Adapter(getSupportFragmentManager());
        adapter.addFragment(ScreenshotFragment.newInstance(DataInfo.TYPE_SCREEN_SHOT), this.getString(R.string.screen_shot_title));
        adapter.addFragment(ScreenshotFragment.newInstance(DataInfo.TYPE_SCREEN_GIF), this.getString(R.string.gif_title));
        adapter.addFragment(ScreenshotFragment.newInstance(DataInfo.TYPE_SCREEN_RECORD), this.getString(R.string.video_title));
        viewPager.setAdapter(adapter);
        mNavigationView.getMenu().findItem(R.id.nav_screenshot).setChecked(true);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        switch (position) {
            case PAGE_SCREENSHOTS:
                mNavigationView.getMenu().findItem(R.id.nav_screenshot).setChecked(true);
                break;
            case PAGE_GIFS:
                mNavigationView.getMenu().findItem(R.id.nav_gifs).setChecked(true);
                break;
            case PAGE_SCREENRECORDS:
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
            mCurrentFragment = (BaseFragment) object;
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
