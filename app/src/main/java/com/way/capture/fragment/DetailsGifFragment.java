package com.way.capture.fragment;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.github.chrisbanes.photoview.PhotoView;
import com.way.capture.R;
import com.way.capture.activity.DetailsActivity;
import com.way.capture.data.DataInfo;
import com.way.capture.utils.glide.GlideHelper;

import butterknife.BindView;
import cn.jzvd.JZVideoPlayerStandard;

public class DetailsGifFragment extends DetailsBaseFragment {
    private static final String TAG = "DetailsRecordFragment";
    @BindView(R.id.detail_image)
    PhotoView mDetailImage;
    @BindView(R.id.video_indicator)
    ImageView mVideoIndicator;

    public static DetailsGifFragment newInstance(DataInfo dataInfo) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_IMAGE_PATH, dataInfo);
        DetailsGifFragment fragment = new DetailsGifFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_detail_gif_layout;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser || mDetailImage == null) {
            return;
        }
        Glide.with(mDetailImage).clear(mDetailImage);
        GlideHelper.loadResourceBitmap(mDataInfo.path, mDetailImage);
        mVideoIndicator.setVisibility(View.VISIBLE);
    }

    @Override
    protected void initData() {
        super.initData();
        mDetailImage.setTransitionName(mDataInfo.path);
        mDetailImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((DetailsActivity) getActivity()).toggleSystemUI();
            }
        });

        GlideHelper.loadResourceBitmap(mDataInfo.path, mDetailImage, new RequestListener<Bitmap>() {
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
        mVideoIndicator.setVisibility(View.VISIBLE);
        mVideoIndicator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setVisibility(View.GONE);
                GlideHelper.loadResourceGif(mDataInfo.path, mDetailImage);
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
