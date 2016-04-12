package com.way.captain.activity;

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
import android.support.v7.widget.Toolbar;
import android.transition.Transition;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.way.captain.R;
import com.way.captain.data.DataInfo;
import com.way.captain.fragment.DetailsFragment;
import com.way.captain.fragment.ScreenshotFragment;
import com.way.captain.utils.TransitionListenerAdapter;

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
                ImageView sharedElement = mCurrentDetailsFragment.getAlbumImage();
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        postponeEnterTransition();
        setEnterSharedElementCallback(mCallback);
        final android.view.Window window = getWindow();
        ObjectAnimator animator = ObjectAnimator.ofInt(window,
                "statusBarColor", window.getStatusBarColor(), Color.BLACK);
        animator.setEvaluator(new ArgbEvaluator());
        animator.setDuration(200L);
        animator.start();

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null)
            toolbar.setAlpha(0f);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        mType = intent.getIntExtra(ScreenshotFragment.ARGS_TYPE, DataInfo.TYPE_SCREEN_SHOT);
        mDatas = getIntent().getStringArrayListExtra(ScreenshotFragment.EXTRA_DATAS);
        mStartingPosition = getIntent().getIntExtra(ScreenshotFragment.EXTRA_STARTING_POSITION, 0);
        if (savedInstanceState == null) {
            mCurrentPosition = mStartingPosition;
        } else {
            mCurrentPosition = savedInstanceState.getInt(STATE_CURRENT_PAGE_POSITION);
        }

        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        if(pager != null) {
            pager.setAdapter(new DetailsFragmentPagerAdapter(getSupportFragmentManager()));
            pager.setCurrentItem(mCurrentPosition);
            pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                @Override
                public void onPageSelected(int position) {
                    mCurrentPosition = position;
                }
            });
        }
        getWindow().getSharedElementEnterTransition().addListener(new TransitionListenerAdapter() {

            @Override
            public void onTransitionEnd(Transition transition) {
                if (toolbar != null)
                    toolbar.animate().alpha(1f);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CURRENT_PAGE_POSITION, mCurrentPosition);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finishAfterTransition();
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
            return DetailsFragment.newInstance(mType, mDatas, position, mStartingPosition);
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
