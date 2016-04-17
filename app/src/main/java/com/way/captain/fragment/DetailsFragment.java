package com.way.captain.fragment;

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

import com.way.captain.R;
import com.way.captain.activity.VideoActivity;
import com.way.captain.data.DataInfo;
import com.way.captain.utils.DensityUtil;
import com.way.captain.utils.glide.GlideHelper;
import com.way.captain.widget.subscaleview.ImageSource;
import com.way.captain.widget.subscaleview.SubsamplingScaleImageView;

import java.io.File;

/**
 * Created by way on 16/4/10.
 */
public class DetailsFragment extends Fragment implements View.OnClickListener {
    private static final String ARG_IMAGE_PATH = "arg_image_path";
    private static final String ARG_IMAGE_TYPE = "arg_image_type";
    private ImageView mImageView;
    private SubsamplingScaleImageView subsamplingScaleImageView;

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
        mImageView = (ImageView) view.findViewById(R.id.detail_image);
        mImageView.setTransitionName(path);
        GlideHelper.loadResourceBitmap(path, mImageView);

        if (type == DataInfo.TYPE_SCREEN_SHOT) {
            subsamplingScaleImageView = (SubsamplingScaleImageView) view.findViewById(R.id.detail_image_height_quality);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);
            int height = options.outHeight;
            boolean isLongImage = height > 2 * DensityUtil.getDisplayHeight(getContext());
            if (isLongImage) {
                Button button = (Button) view.findViewById(R.id.height_quality_btn);
                button.setOnClickListener(this);
                button.setVisibility(View.VISIBLE);
            }
        } else {
            ImageView videoIndicator = (ImageView) view.findViewById(R.id.video_indicator);
            videoIndicator.setVisibility(View.VISIBLE);
            videoIndicator.setOnClickListener(this);
        }
        mImageView.postDelayed(new Runnable() {
            @Override
            public void run() {
                startPostponedEnterTransition();

            }
        }, 200L);
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
                subsamplingScaleImageView.setVisibility(View.VISIBLE);
                mImageView.setTransitionName("");
                subsamplingScaleImageView.setTransitionName(path);
                subsamplingScaleImageView.setImage(ImageSource.uri(Uri.fromFile(new File(path))));
                subsamplingScaleImageView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mImageView.setVisibility(View.GONE);
                    }
                }, 300L);
                break;
        }
    }

}
