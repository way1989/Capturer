package com.way.capture.fragment;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.github.chrisbanes.photoview.PhotoView;
import com.way.capture.R;
import com.way.capture.activity.DetailsActivity;
import com.way.capture.data.DataInfo;
import com.way.capture.utils.ViewUtils;
import com.way.capture.utils.glide.GlideHelper;

import java.io.File;

import butterknife.BindView;

/**
 * Created by way on 16/4/10.
 */
public class DetailsFragment extends DetailsBaseFragment {
    private static final String TAG = "DetailsFragment";

    @BindView(R.id.detail_image_height_quality)
    SubsamplingScaleImageView mDetailImageHeightQuality;
    @BindView(R.id.detail_image)
    PhotoView mDetailImage;
    @BindView(R.id.height_quality_btn)
    Button mHeightQualityBtn;

    public static DetailsFragment newInstance(DataInfo path) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_IMAGE_PATH, path);
        DetailsFragment fragment = new DetailsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser || mDetailImage == null)
            return;

        mDetailImageHeightQuality.recycle();
        mDetailImageHeightQuality.setTransitionName("");
        mDetailImageHeightQuality.setVisibility(View.GONE);
        mDetailImage.setVisibility(View.VISIBLE);
        GlideHelper.loadResourceBitmap(mDataInfo.path, mDetailImage);
        mDetailImage.setTransitionName(mDataInfo.path);
        if (mHeightQualityBtn != null)
            mHeightQualityBtn.setVisibility(View.VISIBLE);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_detail_layout;
    }

    @Override
    protected void initData() {
        super.initData();
        loadImage();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private void loadImage() {
        mDetailImage.setTransitionName(mDataInfo.path);
        mDetailImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((DetailsActivity) getActivity()).toggleSystemUI();
            }
        });
        mDetailImageHeightQuality.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((DetailsActivity) getActivity()).toggleSystemUI();
            }
        });

        boolean isLongImage = mDataInfo.height > 2 * ViewUtils.getHeight();
        mHeightQualityBtn.setVisibility(isLongImage ? View.VISIBLE : View.GONE);
        mHeightQualityBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setVisibility(View.GONE);
                mDetailImageHeightQuality.setVisibility(View.VISIBLE);
                mDetailImage.setTransitionName("");
                mDetailImageHeightQuality.setTransitionName(mDataInfo.path);
                mDetailImageHeightQuality.setImage(ImageSource.uri(Uri.fromFile(new File(mDataInfo.path))));
                mDetailImageHeightQuality.setScaleAndCenter(1.0f, new PointF(0, 0));
                mDetailImageHeightQuality.post(new Runnable() {
                    @Override
                    public void run() {
                        mDetailImage.setVisibility(View.GONE);
                        Glide.with(mDetailImage).clear(mDetailImage);
                        //Glide.clear(mDetailImage);
                    }
                });
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
