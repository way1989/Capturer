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

import java.util.ArrayList;

/**
 * Created by way on 16/4/10.
 */
public class DetailsFragment extends Fragment {
    private static final String ARG_ALBUM_IMAGE_POSITION = "arg_album_image_position";
    private static final String ARG_ALBUM_IMAGE_PATH = "arg_album_image_path";
    private static final String ARG_STARTING_ALBUM_IMAGE_POSITION = "arg_starting_album_image_position";


    private int mStartingPosition;
    private int mAlbumPosition;
    private boolean mIsTransitioning;
    private ImageView mImageView;

    public static DetailsFragment newInstance(ArrayList<String> path, int position, int startingPosition) {
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_ALBUM_IMAGE_PATH, path);
        args.putInt(ARG_ALBUM_IMAGE_POSITION, position);
        args.putInt(ARG_STARTING_ALBUM_IMAGE_POSITION, startingPosition);
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mStartingPosition = getArguments().getInt(ARG_STARTING_ALBUM_IMAGE_POSITION);
        mAlbumPosition = getArguments().getInt(ARG_ALBUM_IMAGE_POSITION);
        mIsTransitioning = savedInstanceState == null && mStartingPosition == mAlbumPosition;
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

        final ArrayList<String> path = getArguments().getStringArrayList(ARG_ALBUM_IMAGE_PATH);
        mImageView = (ImageView) view.findViewById(R.id.detail_image);
        mImageView.setTransitionName(path.get(mAlbumPosition));
        Glide.with(mImageView.getContext())
                .load(path.get(mAlbumPosition))
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
}
