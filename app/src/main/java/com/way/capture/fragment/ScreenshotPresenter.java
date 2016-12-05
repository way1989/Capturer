package com.way.capture.fragment;

import android.support.annotation.NonNull;

import java.util.List;

import rx.Observer;
import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by android on 16-12-1.
 */

public class ScreenshotPresenter extends ScreenshotContract.Presenter {
    @NonNull
    private final CompositeSubscription mSubscriptions;
    private final ScreenshotContract.Model mModel;

    public ScreenshotPresenter(ScreenshotContract.View view) {
        mView = view;
        mSubscriptions = new CompositeSubscription();
        mModel = new ScreenshotModel();
    }

    @Override
    public void getData(int type) {
        mSubscriptions.clear();
        Subscription subscription = mModel.getData(type)
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        mView.showLoading();
                    }
                })
                .subscribe(new Observer<List<String>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        mView.onError(e);
                    }

                    @Override
                    public void onNext(List<String> data) {
                        mView.onLoadFinished(data);
                    }
                });
        mSubscriptions.add(subscription);
    }

    @Override
    public void unSubscribe() {
        mSubscriptions.clear();
    }
}
