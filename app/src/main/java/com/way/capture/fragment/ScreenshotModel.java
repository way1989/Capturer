package com.way.capture.fragment;

import com.way.capture.data.DataInfo;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;


/**
 * Created by android on 16-12-1.
 */

public class ScreenshotModel implements ScreenshotContract.Model {

    @Override
    public Observable<List<String>> getData(final int type) {
        return Observable.create(new ObservableOnSubscribe<List<String>>() {
            @Override
            public void subscribe(ObservableEmitter<List<String>> e) throws Exception {
                e.onNext(DataInfo.getDataInfos(type));
                e.onComplete();
            }
        });
    }
}
