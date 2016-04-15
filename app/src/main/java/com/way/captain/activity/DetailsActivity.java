package com.way.captain.activity;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.format.Formatter;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.alexvasilkov.gestures.commons.DepthPageTransformer;
import com.way.captain.R;
import com.way.captain.data.DataInfo;
import com.way.captain.fragment.DetailsFragment;
import com.way.captain.fragment.ScreenshotFragment;
import com.way.captain.utils.AppUtils;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
        setStatusBarColor();
        initDatas(savedInstanceState);//初始化必要数据
        initToolbar();
        initViewPager();
    }

    private void initDatas(Bundle savedInstanceState) {
        Intent intent = getIntent();
        mType = intent.getIntExtra(ScreenshotFragment.ARGS_TYPE, DataInfo.TYPE_SCREEN_SHOT);
        mDatas = getIntent().getStringArrayListExtra(ScreenshotFragment.EXTRA_DATAS);
        mStartingPosition = getIntent().getIntExtra(ScreenshotFragment.EXTRA_STARTING_POSITION, 0);
        if (savedInstanceState == null) {
            mCurrentPosition = mStartingPosition;
        } else {
            mCurrentPosition = savedInstanceState.getInt(STATE_CURRENT_PAGE_POSITION);
        }
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
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setAlpha(0f);
            toolbar.animate().alpha(1f).setDuration(500L);
        }
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            setActionBarTitle();
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
                    setActionBarTitle();
                }
            });
        }
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finishAfterTransition();
                break;
            case R.id.image_info:
                showDetails(mCurrentPosition);
                break;
            case R.id.image_share:
                shareScreenshot();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDetails(int position) {
        String path = mDatas.get(position);
        String message = getDetails(path);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.image_info).setMessage(message).setPositiveButton(android.R.string.ok, null);
        builder.create().show();
    }

    private String getDetails(String path) {
        File file = new File(path);
        String length = Formatter.formatFileSize(this, file.length());
        length = getString(R.string.image_info_length, length);
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(file.lastModified()));
        time = getString(R.string.image_info_time, time);
        String location = getString(R.string.image_info_path, path);
        switch (mType) {
            case DataInfo.TYPE_SCREEN_SHOT:
            case DataInfo.TYPE_SCREEN_GIF:
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(path, options);
                int height = options.outHeight;
                int width = options.outWidth;
                String size = getString(R.string.image_info_size, width, height);

                return size + "\n" + length + "\n" + time + "\n" + location;
            case DataInfo.TYPE_SCREEN_RECORD:
                Pair<Integer, Integer> pair = AppUtils.getVideoWidthHeight(path);
                String sizeVideo = getString(R.string.image_info_size, pair.first, pair.second);
                String duration = AppUtils.getVideoDuration(path);
                duration = getString(R.string.image_info_duration, duration);
                return sizeVideo + "\n" + duration + "\n" + length + "\n" + time + "\n" + location;

        }
        return "";
    }


    private void shareScreenshot() {
        String subjectDate = DateFormat.getDateTimeInstance().format(new Date(System.currentTimeMillis()));
        String subject = String.format(ScreenshotFragment.SCREENSHOT_SHARE_SUBJECT_TEMPLATE, subjectDate);
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("image/png");
        sharingIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(mDatas.get(mCurrentPosition))));
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        Intent chooserIntent = Intent.createChooser(sharingIntent, null);
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(chooserIntent);
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
