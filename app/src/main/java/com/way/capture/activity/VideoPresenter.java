package com.way.capture.activity;

import android.support.annotation.NonNull;

import com.way.capture.App;
import com.way.capture.utils.ffmpeg.FFmpeg;

import rx.subscriptions.CompositeSubscription;

/**
 * Created by android on 16-12-2.
 */

public class VideoPresenter extends VideoContract.Presenter {
    @NonNull
    private final CompositeSubscription mSubscriptions;
    private VideoContract.View mView;
    private VideoContract.Model mModel;

    public VideoPresenter(VideoContract.View view) {
        mView = view;
        mSubscriptions = new CompositeSubscription();
        mModel = new VideoModel();
    }

    @Override
    public void toGif(String path) {
        if(FFmpeg.getInstance(App.getContext()).hasLibrary()){
            mView.showCheckQuality();
            return;
        }
    }

    @Override
    public void unSubscribe() {
        mSubscriptions.clear();
    }
}
