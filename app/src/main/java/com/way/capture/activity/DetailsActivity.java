package com.way.capture.activity;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import com.bumptech.glide.Glide;
import com.way.capture.R;
import com.way.capture.base.BaseActivity;
import com.way.capture.data.DataInfo;
import com.way.capture.fragment.DetailsFragment;
import com.way.capture.fragment.ScreenshotFragment;
import com.way.capture.utils.AppUtils;
import com.way.capture.widget.DepthPageTransformer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class DetailsActivity extends BaseActivity {
    private static final String STATE_CURRENT_PAGE_POSITION = "state_current_page_position";
    private DetailsFragment mCurrentDetailsFragment;
    private int mCurrentPosition;
    private int mStartingPosition;
    private boolean mIsReturning;
    private final SharedElementCallback mCallback = new SharedElementCallback() {
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            if (mIsReturning) {
                View sharedElement = mCurrentDetailsFragment.getAlbumImage();
                if (sharedElement == null) {
                    // If shared element is null, then it has been scrolled off screen and
                    // no longer visible. In this case we cancel the shared element transition by
                    // removing the shared element from the shared elements map.
                    names.clear();
                    sharedElements.clear();
                } else if (mStartingPosition != mCurrentPosition) {
                    // If the user has swiped to a different ViewPager page, then we need to
                    // remove the old shared element and replace it with the new shared element
                    // that should be transitioned instead.
                    names.clear();
                    names.add(sharedElement.getTransitionName());
                    sharedElements.clear();
                    sharedElements.put(sharedElement.getTransitionName(), sharedElement);
                }
            }
        }
    };
    private ArrayList<String> mDatas;
    private int mType;
    private Toolbar toolbar;
    private boolean fullscreenmode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        mType = intent.getIntExtra(ScreenshotFragment.ARGS_TYPE, DataInfo.TYPE_SCREEN_SHOT);
        mDatas = getIntent().getStringArrayListExtra(ScreenshotFragment.EXTRA_DATAS);
        mStartingPosition = getIntent().getIntExtra(ScreenshotFragment.EXTRA_STARTING_POSITION, 0);
        if (savedInstanceState != null) {
            mCurrentPosition = savedInstanceState.getInt(STATE_CURRENT_PAGE_POSITION, 0);
        } else {
            mCurrentPosition = mStartingPosition;
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initWidget() {
        super.initWidget();
        initToolbar();
        setupSystemUI();
        initViewPager();
        postponeEnterTransition();
        setEnterSharedElementCallback(mCallback);
        setStatusBarColor();
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_details;
    }

    private void setStatusBarColor() {
        final android.view.Window window = getWindow();
        ObjectAnimator animator = ObjectAnimator.ofInt(window,
                "statusBarColor", window.getStatusBarColor(), Color.BLACK);
        animator.setEvaluator(new ArgbEvaluator());
        animator.setDuration(200L);
        animator.start();
    }

    private void initToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            //setActionBarTitle();
        }
    }

    private void setActionBarTitle() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null)
            return;
        String path = mDatas.get(mCurrentPosition);
        if (path.contains(File.separator) && path.contains(".")) {
            String title = path.substring(path.lastIndexOf(File.separatorChar) + 1, path.lastIndexOf('.'));
            Log.i("liweiping", "title = " + title);
            actionBar.setTitle(title);
        }
    }

    private void initViewPager() {
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        if (pager != null) {
            pager.setAdapter(new DetailsFragmentPagerAdapter(getSupportFragmentManager()));
            pager.setCurrentItem(mCurrentPosition);
            pager.setPageTransformer(true, new DepthPageTransformer());
            pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                @Override
                public void onPageSelected(int position) {
                    mCurrentPosition = position;
                    //setActionBarTitle();
                }
            });
        }
    }

    private void setupSystemUI() {
        toolbar.animate().translationY(AppUtils.getStatusBarHeight(getResources())).setInterpolator(new DecelerateInterpolator())
                .setDuration(0).start();
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) showSystemUI();
                        else hideSystemUI();
                    }
                });
    }

    public void toggleSystemUI() {
        if (fullscreenmode)
            showSystemUI();
        else
            hideSystemUI();
    }

    private void showSystemUI() {
        runOnUiThread(new Runnable() {
            public void run() {
                toolbar.animate().translationY(AppUtils.getStatusBarHeight(getResources())).setInterpolator(new DecelerateInterpolator())
                        .setDuration(240);
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
                fullscreenmode = false;
                //changeBackGroundColor();
            }
        });
    }

    private void hideSystemUI() {
        runOnUiThread(new Runnable() {
            public void run() {
                toolbar.animate().translationY(-toolbar.getHeight()).setInterpolator(new AccelerateInterpolator())
                        .setDuration(200);
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                                | View.SYSTEM_UI_FLAG_IMMERSIVE);

                fullscreenmode = true;
                //changeBackGroundColor();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CURRENT_PAGE_POSITION, mCurrentPosition);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_info, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Glide.get(getApplicationContext()).clearMemory();
        Glide.get(getApplicationContext()).trimMemory(TRIM_MEMORY_COMPLETE);
        System.gc();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String path = mDatas.get(mCurrentPosition);

        switch (item.getItemId()) {
            case android.R.id.home:
                finishAfterTransition();
                break;
            /*case R.id.image_delete:

                break;*/
            case R.id.image_info:
                AppUtils.showDetails(this, path, mType);
                break;
            case R.id.image_share:
                AppUtils.shareScreenshot(this, path, mType);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finishAfterTransition() {
        mIsReturning = true;
        Intent data = new Intent();
        data.putExtra(ScreenshotFragment.EXTRA_STARTING_POSITION, mStartingPosition);
        data.putExtra(ScreenshotFragment.EXTRA_CURRENT_POSITION, mCurrentPosition);
        setResult(RESULT_OK, data);
        super.finishAfterTransition();
    }

    private class DetailsFragmentPagerAdapter extends FragmentStatePagerAdapter {
        public DetailsFragmentPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return DetailsFragment.newInstance(mType, mDatas.get(position));
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            mCurrentDetailsFragment = (DetailsFragment) object;
        }

        @Override
        public int getCount() {
            return mDatas.size();
        }
    }
}
