package com.way.capture.fragment;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.way.capture.R;
import com.way.capture.activity.DetailsActivity;
import com.way.capture.data.DataInfo;
import com.way.capture.utils.glide.GlideHelper;

import butterknife.BindView;
import cn.jzvd.JZVideoPlayerStandard;

public class DetailsRecordFragment extends DetailsBaseFragment {
    private static final String TAG = "DetailsRecordFragment";
    @BindView(R.id.detail_image)
    JZVideoPlayerStandard mDetailImage;

    public static DetailsRecordFragment newInstance(DataInfo path) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_IMAGE_PATH, path);
        DetailsRecordFragment fragment = new DetailsRecordFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_detail_record_layout;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser || mDetailImage == null) {
            return;
        }
        mDetailImage.onCompletion();
    }

    @Override
    protected void initData() {
        super.initData();
        mDetailImage.setTransitionName(mDataInfo.path);
        mDetailImage.fullscreenButton.setVisibility(View.GONE);
        mDetailImage.textureViewContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((DetailsActivity) getActivity()).toggleSystemUI();
            }
        });
        mDetailImage.setUp(mDataInfo.path, JZVideoPlayerStandard.SCREEN_WINDOW_FULLSCREEN, "");

        GlideHelper.loadResourceBitmap(mDataInfo.path, mDetailImage.thumbImageView, new RequestListener<Bitmap>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                startPostEnterTransition();
                return false;
            }

            @Override
            public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                startPostEnterTransition();
                return false;
            }
        });
    }

    /**
     * Returns the shared element that should be transitioned back to the previous Activity, or null
     * if the view is not visible on the screen.
     */
    @Override
    public View getAlbumImage() {
        if (isViewInBounds(getActivity().getWindow().getDecorView(), mDetailImage)) {
            return mDetailImage;
        }
        return null;
    }
}
