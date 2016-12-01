package com.way.capture.fragment;

import com.way.capture.data.DataInfo;
import com.way.capture.utils.RxSchedulers;

import java.util.List;

import rx.Observable;
import rx.Subscriber;

/**
 * Created by android on 16-12-1.
 */

public class ScreenshotModel implements ScreenshotContract.Model {

    @Override
    public Observable<List<String>> getData(final int type) {
        return Observable.create(new Observable.OnSubscribe<List<String>>() {
            @Override
            public void call(Subscriber<? super List<String>> subscriber) {
                List<String> data = DataInfo.getDataInfos(type);
                subscriber.onNext(data);
                subscriber.onCompleted();
            }
        }).compose(RxSchedulers.<List<String>>io_main());
    }
}
