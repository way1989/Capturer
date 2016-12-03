package com.way.capture.activity;

import com.way.capture.base.BaseModel;
import com.way.capture.base.BasePresenter;
import com.way.capture.base.BaseView;


/**
 * Created by android on 16-12-2.
 */

public class VideoContract {
    interface Model extends BaseModel {
    }

    interface View extends BaseView {
        void showCheckQuality();

        void showFinished(boolean succeed);

        void showError(String msg);

        void showLoading(String msg);
    }

    abstract static class Presenter extends BasePresenter<VideoContract.View> {
        public abstract void toGif(String path);
    }
}
