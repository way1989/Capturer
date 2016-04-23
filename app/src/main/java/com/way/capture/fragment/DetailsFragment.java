package com.way.capture.fragment;

import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.way.capture.R;
import com.way.capture.activity.VideoActivity;
import com.way.capture.data.DataInfo;
import com.way.capture.utils.DensityUtil;
import com.way.capture.utils.glide.GlideHelper;
import com.way.capture.widget.subscaleview.ImageSource;
import com.way.capture.widget.subscaleview.SubsamplingScaleImageView;

import java.io.File;

/**
 * Created by way on 16/4/10.
 */
public class DetailsFragment extends Fragment implements View.OnClickListener {
    private static final String ARG_IMAGE_PATH = "arg_image_path";
    private static final String ARG_IMAGE_TYPE = "arg_image_type";
    private ImageView mImageView;
    private ImageView mPlayButton;
    private Button mShowHeighQualityButton;
    private SubsamplingScaleImageView mSubsamplingScaleImageView;

    public static DetailsFragment newInstance(int type, String path) {
        Bundle args = new Bundle();
        args.putInt(ARG_IMAGE_TYPE, type);
        args.putString(ARG_IMAGE_PATH, path);
        DetailsFragment fragment = new DetailsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Returns true if {@param view} is contained within {@param container}'s bounds.
     */
    private static boolean isViewInBounds(@NonNull View container, @NonNull View view) {
        Rect containerBounds = new Rect();
        container.getHitRect(containerBounds);
        return view.getLocalVisibleRect(containerBounds);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser || mImageView == null)
            return;

        final int type = getArguments().getInt(ARG_IMAGE_TYPE, DataInfo.TYPE_SCREEN_SHOT);
        final String path = getArguments().getString(ARG_IMAGE_PATH);
        switch (type) {
            case DataInfo.TYPE_SCREEN_SHOT:
                if (mSubsamplingScaleImageView != null) {
                    mSubsamplingScaleImageView.recycle();
                    mSubsamplingScaleImageView.setTransitionName("");
                    mSubsamplingScaleImageView.setVisibility(View.GONE);
                }
                mImageView.setVisibility(View.VISIBLE);
                GlideHelper.loadResourceBitmap(path, mImageView);
                mImageView.setTransitionName(path);
                if (mShowHeighQualityButton != null)
                    mShowHeighQualityButton.setVisibility(View.VISIBLE);
                break;
            case DataInfo.TYPE_SCREEN_GIF:
                Glide.clear(mImageView);
                GlideHelper.loadResourceBitmap(path, mImageView);
                mPlayButton.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final int type = getArguments().getInt(ARG_IMAGE_TYPE, DataInfo.TYPE_SCREEN_SHOT);
        if (type == DataInfo.TYPE_SCREEN_SHOT)
            return inflater.inflate(R.layout.fragment_detail_layout_image, container, false);
        return inflater.inflate(R.layout.fragment_detail_layout, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final int type = getArguments().getInt(ARG_IMAGE_TYPE, DataInfo.TYPE_SCREEN_SHOT);
        final String path = getArguments().getString(ARG_IMAGE_PATH);
        loadImage(view, type, path);
    }

    private void loadImage(View view, int type, String path) {
        mImageView = (ImageView) view.findViewById(R.id.detail_image);
        mImageView.setTransitionName(path);

        switch (type) {
            case DataInfo.TYPE_SCREEN_SHOT:
                mSubsamplingScaleImageView = (SubsamplingScaleImageView) view.findViewById(R.id.detail_image_height_quality);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(path, options);
                int height = options.outHeight;
                boolean isLongImage = height > 2 * DensityUtil.getDisplayHeight(getContext());
                if (isLongImage) {
                    mShowHeighQualityButton = (Button) view.findViewById(R.id.height_quality_btn);
                    mShowHeighQualityButton.setOnClickListener(this);
                    mShowHeighQualityButton.setVisibility(View.VISIBLE);
                }
                break;
            case DataInfo.TYPE_SCREEN_GIF:
                GlideHelper.loadResourceBitmap(path, mImageView);
                mImageView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startPostponedEnterTransition();
                    }
                }, 200L);
                break;
            case DataInfo.TYPE_SCREEN_RECORD:
                break;
        }

        if (type != DataInfo.TYPE_SCREEN_GIF) {
            Glide.with(mImageView.getContext()).load(path).dontAnimate()
                    .listener(new RequestListener<String, GlideDrawable>() {
                        @Override
                        public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                            startPostponedEnterTransition();
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                            startPostponedEnterTransition();
                            return false;
                        }
                    }).fitCenter().into(mImageView);
        }
        if (type != DataInfo.TYPE_SCREEN_SHOT) {
            mPlayButton = (ImageView) view.findViewById(R.id.video_indicator);
            mPlayButton.setVisibility(View.VISIBLE);
            mPlayButton.setOnClickListener(this);
        }
    }

    private void startPostponedEnterTransition() {
        mImageView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mImageView.getViewTreeObserver().removeOnPreDrawListener(this);
                getActivity().startPostponedEnterTransition();
                return true;
            }
        });
    }

    /**
     * Returns the shared element that should be transitioned back to the previous Activity,
     * or null if the view is not visible on the screen.
     */
    @Nullable
    public View getAlbumImage() {
        if (isViewInBounds(getActivity().getWindow().getDecorView(), mImageView)) {
            return mImageView;
        }
        return null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.video_indicator:
                final int type = getArguments().getInt(ARG_IMAGE_TYPE, DataInfo.TYPE_SCREEN_SHOT);
                if (type == DataInfo.TYPE_SCREEN_RECORD) {
                    VideoActivity.startVideoActivity(getActivity(), getArguments().getString(ARG_IMAGE_PATH), mImageView);
                } else if (type == DataInfo.TYPE_SCREEN_GIF) {
                    v.setVisibility(View.GONE);
                    final String path = getArguments().getString(ARG_IMAGE_PATH);
                    GlideHelper.loadResource(path, mImageView);
                }
                break;
            case R.id.height_quality_btn:
                String path = getArguments().getString(ARG_IMAGE_PATH);
                v.setVisibility(View.GONE);
                mSubsamplingScaleImageView.setVisibility(View.VISIBLE);
                mImageView.setTransitionName("");
                mSubsamplingScaleImageView.setTransitionName(path);
                mSubsamplingScaleImageView.setImage(ImageSource.uri(Uri.fromFile(new File(path))));
                mSubsamplingScaleImageView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mImageView.setVisibility(View.GONE);
                        Glide.clear(mImageView);
                    }
                }, 300L);
                break;
        }
    }

}
