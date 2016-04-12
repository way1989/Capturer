package com.way.captain.fragment;

import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.way.captain.R;
import com.way.captain.activity.VideoActivity;
import com.way.captain.data.DataInfo;
import com.way.captain.utils.glide.GlideHelper;

/**
 * Created by way on 16/4/10.
 */
public class DetailsFragment extends Fragment implements View.OnClickListener {
    private static final String ARG_IMAGE_PATH = "arg_image_path";
    private static final String ARG_IMAGE_TYPE = "arg_image_type";


    private ImageView mImageView;

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
        View view = inflater.inflate(R.layout.fragment_detail_layout, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final int type = getArguments().getInt(ARG_IMAGE_TYPE, DataInfo.TYPE_SCREEN_SHOT);
        final String path = getArguments().getString(ARG_IMAGE_PATH);

        mImageView = (ImageView) view.findViewById(R.id.detail_image);
        mImageView.setTransitionName(path);

        if (type == DataInfo.TYPE_SCREEN_GIF) {
            GlideHelper.loadResourceBitmap(path, mImageView);
            mImageView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    GlideHelper.loadResource(path, mImageView);
                }
            }, 500L);
            startPostponedEnterTransition();
        } else {
            Glide.with(mImageView.getContext())
                    .load(path)
                    .dontAnimate()
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
                    }).fitCenter()
                    .into(mImageView);
            if (type == DataInfo.TYPE_SCREEN_RECORD) {
                ImageView imageView = (ImageView) view.findViewById(R.id.video_indicator);
                imageView.setVisibility(View.VISIBLE);
                imageView.setOnClickListener(this);

            }
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
    public ImageView getAlbumImage() {
        if (isViewInBounds(getActivity().getWindow().getDecorView(), mImageView)) {
            return mImageView;
        }
        return null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.video_indicator:
                if (getArguments().getInt(ARG_IMAGE_TYPE) != DataInfo.TYPE_SCREEN_RECORD)
                    return;
                VideoActivity.startVideoActivity(getActivity(), getArguments().getString(ARG_IMAGE_PATH), mImageView);
                break;
        }
    }
}
