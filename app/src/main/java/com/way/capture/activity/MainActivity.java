package com.way.capture.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.jakewharton.rxbinding2.view.RxView;
import com.thefinestartist.finestwebview.FinestWebView;
import com.way.capture.App;
import com.way.capture.R;
import com.way.capture.base.BaseActivity;
import com.way.capture.base.BaseFragment;
import com.way.capture.base.BaseScreenshotFragment;
import com.way.capture.data.DataInfo;
import com.way.capture.fragment.ScreenshotFragment;
import com.way.capture.service.ShakeService;
import com.way.capture.utils.AppUtils;
import com.wooplr.spotlight.SpotlightView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import io.reactivex.functions.Consumer;

public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";
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
                    .show("https://way1989.github.io/2016/05/15/help/CapturerHelp");
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
    @BindView(R.id.viewpager)
    ViewPager mViewPager;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.tab_layout)
    TabLayout mTabLayout;
    private BaseScreenshotFragment mCurrentFragment;
    private long mLastPressTime;
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

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(AppUtils.APP_FIRST_RUN, true))
            startActivity(new Intent(MainActivity.this, GuideActivity.class));

        setExitSharedElementCallback(mCallback);
    }

    @Override
    protected void initWidget() {
        super.initWidget();
        setSupportActionBar(mToolbar);
        setTitle("");

        setupViewPager(mViewPager);
        mViewPager.setOffscreenPageLimit(2);
        mTabLayout.setupWithViewPager(mViewPager);
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_main;
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
        if (mCurrentFragment != null && mCurrentFragment.onBackPressed())
            return;
        long time = System.currentTimeMillis();
        if (time - mLastPressTime > 3000) {
            Snackbar.make(mViewPager, R.string.twice_press_exit, Snackbar.LENGTH_LONG).show();
            mLastPressTime = time;
        } else
            super.onBackPressed();

    }

    private void syncNightMode(MenuItem item) {
        int uiMode = getResources().getConfiguration().uiMode;
        boolean isCurrentNightMode = (uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        item.setTitle(isCurrentNightMode ? R.string.nav_day_mode : R.string.nav_night_mode);
        item.setIcon(isCurrentNightMode ? R.drawable.ic_mode_day : R.drawable.ic_mode_night);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem item = menu.findItem(R.id.action_night_mode);
        syncNightMode(item);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_night_mode:
                navigateChangeMode.run();
                return true;
            case R.id.action_help:
                navigateHelp.run();
                return true;
            case R.id.action_settings:
                navigateSettings.run();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    private void setupViewPager(ViewPager viewPager) {
        Adapter adapter = new Adapter(getSupportFragmentManager());
        adapter.addFragment(ScreenshotFragment.newInstance(DataInfo.TYPE_SCREEN_SHOT), this.getString(R.string.screen_shot_title));
        adapter.addFragment(ScreenshotFragment.newInstance(DataInfo.TYPE_SCREEN_GIF), this.getString(R.string.gif_title));
        adapter.addFragment(ScreenshotFragment.newInstance(DataInfo.TYPE_SCREEN_RECORD), this.getString(R.string.video_title));
        viewPager.setAdapter(adapter);
    }

    class Adapter extends FragmentPagerAdapter {
        private final List<BaseFragment> mFragments = new ArrayList<>();
        private final List<String> mFragmentTitles = new ArrayList<>();

        Adapter(FragmentManager fm) {
            super(fm);
        }

        void addFragment(BaseFragment fragment, String title) {
            mFragments.add(fragment);
            mFragmentTitles.add(title);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            mCurrentFragment = (BaseScreenshotFragment) object;
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
