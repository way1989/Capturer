package com.way.capture.fragment;

import com.way.capture.base.BaseModel;
import com.way.capture.base.BasePresenter;
import com.way.capture.base.BaseView;
import com.way.capture.data.DataInfo;

import java.util.List;

import io.reactivex.Observable;


/**
 * Created by android on 16-12-1.
 */

public class ScreenshotContract {
    interface Model extends BaseModel {
        Observable<List<DataInfo>> getData(int type);
    }

    interface View extends BaseView {
        void onLoadFinished(List<DataInfo> data);

        void onError(Throwable e);

    }

    abstract static class Presenter extends BasePresenter<View> {
        public abstract void getData(int type);
    }
}
