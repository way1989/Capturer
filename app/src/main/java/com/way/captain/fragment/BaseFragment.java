package com.way.captain.fragment;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import java.util.List;
import java.util.Map;

/**
 * Created by android on 16-2-2.
 */
public abstract class BaseFragment extends Fragment {
    public abstract boolean onBackPressed();

    public abstract void onActivityReenter(Bundle bundle);
    public abstract void changeSharedElements(List<String> names, Map<String, View> sharedElements, int position);
}
