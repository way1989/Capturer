package com.way.capture.fragment;

import android.os.Bundle;
import android.view.View;

import java.util.List;
import java.util.Map;

/**
 * Created by android on 16-12-1.
 */

public abstract class BaseScreenshotFragment extends BaseFragment{
    public abstract boolean onBackPressed();

    public abstract void onActivityReenter(Bundle bundle);

    public abstract void changeSharedElements(List<String> names, Map<String, View> sharedElements, int position);

    public abstract void scrollToTop();

}
