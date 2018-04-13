package com.way.capture.fragment;

import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewTreeObserver;

import com.way.capture.base.BaseFragment;
import com.way.capture.data.DataInfo;

public abstract class DetailsBaseFragment extends BaseFragment {
    protected static final String ARG_IMAGE_PATH = "arg_image_path";
    protected DataInfo mDataInfo;

    /**
     * Returns true if {@param view} is contained within {@param container}'s bounds.
     */
    protected static boolean isViewInBounds(@NonNull View container, @NonNull View view) {
        Rect containerBounds = new Rect();
        container.getHitRect(containerBounds);
        return view.getLocalVisibleRect(containerBounds);
    }

    protected void startPostEnterTransition() {
        mRoot.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mRoot.getViewTreeObserver().removeOnPreDrawListener(this);
                getActivity().startPostponedEnterTransition();
                return true;
            }
        });
    }

    @Override
    protected void initBundle(Bundle bundle) {
        super.initBundle(bundle);
        mDataInfo = (DataInfo) bundle.getSerializable(ARG_IMAGE_PATH);
    }

    public abstract View getAlbumImage();
}
