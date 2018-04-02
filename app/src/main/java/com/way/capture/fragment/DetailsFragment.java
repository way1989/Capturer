package com.way.capture.fragment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;

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
import com.way.capture.activity.VideoActivity;
import com.way.capture.base.BaseFragment;
import com.way.capture.data.DataInfo;
import com.way.capture.utils.ViewUtils;
import com.way.capture.utils.glide.GlideHelper;

import java.io.File;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by way on 16/4/10.
 */
public class DetailsFragment extends BaseFragment implements View.OnClickListener {
    private static final String TAG = "DetailsFragment";
    private static final String ARG_IMAGE_PATH = "arg_image_path";
    private static final String ARG_IMAGE_TYPE = "arg_image_type";

    @BindView(R.id.detail_image_height_quality)
    SubsamplingScaleImageView mDetailImageHeightQuality;
    @BindView(R.id.detail_image)
    PhotoView mDetailImage;
    @BindView(R.id.video_indicator)
    ImageView mVideoIndicator;
    @BindView(R.id.height_quality_btn)
    Button mHeightQualityBtn;
    private int mType;
    private String mPath;

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
        if (isVisibleToUser || mDetailImage == null)
            return;

        switch (mType) {
            case DataInfo.TYPE_SCREEN_SHOT:
                mDetailImageHeightQuality.recycle();
                mDetailImageHeightQuality.setTransitionName("");
                mDetailImageHeightQuality.setVisibility(View.GONE);
                mDetailImage.setVisibility(View.VISIBLE);
                GlideHelper.loadResourceBitmap(mPath, mDetailImage);
                mDetailImage.setTransitionName(mPath);
                if (mHeightQualityBtn != null)
                    mHeightQualityBtn.setVisibility(View.VISIBLE);
                break;
            case DataInfo.TYPE_SCREEN_GIF:
//                Glide.clear(mDetailImage);
                Glide.with(mDetailImage).clear(mDetailImage);
                GlideHelper.loadResourceBitmap(mPath, mDetailImage);
                mVideoIndicator.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_detail_layout;
    }

    @Override
    protected void initBundle(Bundle bundle) {
        super.initBundle(bundle);
        mType = bundle.getInt(ARG_IMAGE_TYPE, DataInfo.TYPE_SCREEN_SHOT);
        mPath = bundle.getString(ARG_IMAGE_PATH);
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
        mDetailImage.setTransitionName(mPath);
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

        boolean isLongImage = isLongImage(mPath);
        mHeightQualityBtn.setVisibility(isLongImage ? View.VISIBLE : View.GONE);
        mVideoIndicator.setVisibility(mType == DataInfo.TYPE_SCREEN_SHOT ? View.GONE : View.VISIBLE);

        GlideHelper.loadResourceBitmap(mPath, mDetailImage, new RequestListener<Bitmap>() {
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

    private boolean isLongImage(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        int height = options.outHeight;
        return height > 2 * ViewUtils.getHeight();
    }

    private void startPostEnterTransition() {
        mDetailImage.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mDetailImage.getViewTreeObserver().removeOnPreDrawListener(this);
                getActivity().startPostponedEnterTransition();
                return true;
            }
        });
    }

    /**
     * Returns the shared element that should be transitioned back to the previous Activity, or null
     * if the view is not visible on the screen.
     */
    @Nullable
    public View getAlbumImage() {
        if (isViewInBounds(getActivity().getWindow().getDecorView(), mDetailImage)) {
            return mDetailImage;
        }
        return null;
    }

    @OnClick({R.id.video_indicator, R.id.height_quality_btn})
    public void onClick(final View view) {
        switch (view.getId()) {
            case R.id.video_indicator:
                if (mType == DataInfo.TYPE_SCREEN_RECORD) {
                    VideoActivity.startVideoActivity(getActivity(), mPath, mDetailImage);
                } else if (mType == DataInfo.TYPE_SCREEN_GIF) {
                    view.setVisibility(View.GONE);
                    final String path = getArguments().getString(ARG_IMAGE_PATH);
                    GlideHelper.loadResourceGif(path, mDetailImage);
                }
                break;
            case R.id.height_quality_btn:
                view.setVisibility(View.GONE);
                mDetailImageHeightQuality.setVisibility(View.VISIBLE);
                mDetailImage.setTransitionName("");
                mDetailImageHeightQuality.setTransitionName(mPath);
                mDetailImageHeightQuality.setImage(ImageSource.uri(Uri.fromFile(new File(mPath))));
                mDetailImageHeightQuality.setScaleAndCenter(1.0f, new PointF(0, 0));
                mDetailImageHeightQuality.post(new Runnable() {
                    @Override
                    public void run() {
                        mDetailImage.setVisibility(View.GONE);
                        Glide.with(mDetailImage).clear(mDetailImage);
                        //Glide.clear(mDetailImage);
                    }
                });
                break;
        }
    }
}
