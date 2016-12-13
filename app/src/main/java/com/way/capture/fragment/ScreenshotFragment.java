package com.way.capture.fragment;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.afollestad.dragselectrecyclerview.DragSelectRecyclerView;
import com.afollestad.dragselectrecyclerview.DragSelectRecyclerViewAdapter;
import com.afollestad.materialcab.MaterialCab;
import com.trello.rxlifecycle.android.FragmentEvent;
import com.way.capture.App;
import com.way.capture.R;
import com.way.capture.activity.DetailsActivity;
import com.way.capture.adapter.ScreenshotAdapter;
import com.way.capture.base.BaseFragment;
import com.way.capture.base.BaseScreenshotFragment;
import com.way.capture.data.DataInfo;
import com.way.capture.utils.AppUtils;
import com.way.capture.utils.DensityUtil;
import com.way.capture.utils.RxBus;
import com.way.capture.utils.RxEvent;
import com.way.capture.widget.SpaceGridItemDecoration;
import com.weavey.loading.lib.LoadingLayout;

import java.util.List;
import java.util.Map;

import butterknife.BindView;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by way on 16/4/10.
 */
public class ScreenshotFragment extends BaseScreenshotFragment implements ScreenshotAdapter.OnItemClickListener,
        DragSelectRecyclerViewAdapter.SelectionListener, MaterialCab.Callback, ScreenshotContract.View {
    public static final String ARGS_TYPE = "type";
    public static final String EXTRA_DATAS = "extra_datas";
    public static final String EXTRA_STARTING_POSITION = "extra_starting_item_position";
    public static final String EXTRA_CURRENT_POSITION = "extra_current_item_position";
    @BindView(R.id.loading_layout)
    LoadingLayout mLoadingLayout;
    @BindView(R.id.recycler_view)
    DragSelectRecyclerView mRecyclerView;
    private ScreenshotAdapter mAdapter;

    private MaterialCab mCab;
    private boolean mIsDetailsActivityStarted;
    private int mType;
    private boolean hasLoadData;

    public static BaseFragment newInstance(int type) {
        Bundle args = new Bundle();
        args.putInt(ARGS_TYPE, type);
        ScreenshotFragment fragment = new ScreenshotFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter.restoreInstanceState(savedInstanceState);
        mCab = MaterialCab.restoreState(savedInstanceState, (AppCompatActivity) getActivity(), this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        hasLoadData = false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save selected indices
        mAdapter.saveInstanceState(outState);
        if (mCab != null) mCab.saveState(outState);
    }

    @Override
    public boolean onBackPressed() {
        if (mAdapter.getSelectedCount() > 0) {
            mAdapter.clearSelected();
            return true;
        }
        return false;
    }

    @Override
    public void onActivityReenter(Bundle bundle) {
        if (mRecyclerView == null) return;
        int startingPosition = bundle.getInt(EXTRA_STARTING_POSITION);
        int currentPosition = bundle.getInt(EXTRA_CURRENT_POSITION);
        Log.i("way", "onActivityReenter startingPosition = " + startingPosition + ", currentPosition = " + currentPosition);

        if (startingPosition != currentPosition) {
            mRecyclerView.scrollToPosition(currentPosition);
        }
        getActivity().postponeEnterTransition();
        mRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                // TODO: figure out why it is necessary to request layout here in order to get a smooth transition.
                mRecyclerView.requestLayout();
                Log.i("way", "onActivityReenter startPostponedEnterTransition...");
                getActivity().startPostponedEnterTransition();
                return true;
            }
        });
    }

    @Override
    public void changeSharedElements(List<String> names, Map<String, View> sharedElements, int position) {
        String newTransitionName = mAdapter.getItem(position);
        View newSharedElement = mRecyclerView.findViewHolderForAdapterPosition(position).itemView.findViewById(R.id.iv_image);
        Log.i("way", "changeSharedElements newSharedElement = " + newSharedElement);
        if (newSharedElement != null) {
            names.clear();
            names.add(newTransitionName);
            sharedElements.clear();
            sharedElements.put(newTransitionName, newSharedElement);
        }
    }

    @Override
    public void scrollToTop() {
        mRecyclerView.scrollToPosition(0);
    }

    @Override
    public void onResume() {
        super.onResume();
        mIsDetailsActivityStarted = false;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_screenshot;
    }

    @Override
    protected void initBundle(Bundle bundle) {
        super.initBundle(bundle);
        mType = bundle.getInt(ARGS_TYPE, DataInfo.TYPE_SCREEN_SHOT);
    }

    @Override
    protected void initWidget(View root) {
        super.initWidget(root);

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 4);
        mRecyclerView.addItemDecoration(new SpaceGridItemDecoration(DensityUtil.dip2px(getContext(), 1)));
        mRecyclerView.setLayoutManager(layoutManager);
        mAdapter = new ScreenshotAdapter(getContext(), mType, this);
        mAdapter.setSelectionListener(this);
        mRecyclerView.setAdapter(mAdapter);
        mLoadingLayout.setStatus(LoadingLayout.Loading);
    }

    @Override
    protected void initData() {
        super.initData();
        RxBus.getInstance().toObservable(RxEvent.NewPathEvent.class)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(this.<RxEvent.NewPathEvent>bindUntilEvent(FragmentEvent.DESTROY))
                .subscribe(new Action1<RxEvent.NewPathEvent>() {
                    @Override
                    public void call(RxEvent.NewPathEvent newPathEvent) {
                        final int type = newPathEvent.type;
                        final String path = newPathEvent.path;
                        if (type == mType) {
                            mAdapter.addData(path);
                        }
                    }
                });
        loadData();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        loadData();
    }

    private void loadData() {
        if (isAdded() && getUserVisibleHint() && !hasLoadData) {
            new ScreenshotPresenter(this).getData(mType);
            hasLoadData = true;
        }
    }

    @Override
    public void onItemClick(View v) {
        int position = (Integer) v.getTag(R.id.tag_item);
        if (mCab != null && mCab.isActive()) {
            mAdapter.toggleSelected(position);
        } else {
            ImageView imageView = (ImageView) v.findViewById(R.id.iv_image);
            if (!mIsDetailsActivityStarted) {
                mIsDetailsActivityStarted = true;
                Intent intent = new Intent(getActivity(), DetailsActivity.class);
                intent.putExtra(ARGS_TYPE, mType);
                intent.putStringArrayListExtra(EXTRA_DATAS, mAdapter.getData());
                intent.putExtra(EXTRA_STARTING_POSITION, position);
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(getActivity(),
                        imageView, imageView.getTransitionName()).toBundle());
            }
        }
    }

    @Override
    public void onLongClick(int position) {
        mRecyclerView.setDragSelectActive(true, position);
    }

    @Override
    public void onDragSelectionChanged(int count) {
        if (count > 0) {
            if (mCab == null) {
                mCab = new MaterialCab((AppCompatActivity) getActivity(), R.id.cab_stub)
                        .setMenu(R.menu.menu_screenshot_item)
                        .setCloseDrawableRes(R.drawable.ic_clear_white_24dp)
                        .start(this);
            }
            mCab.setTitleRes(R.string.cab_title_x, count);
        } else if (mCab != null && mCab.isActive()) {
            mCab.reset().finish();
            mCab = null;
        }

    }

    @Override
    public boolean onCabCreated(MaterialCab cab, Menu menu) {
        return true;
    }

    @Override
    public boolean onCabItemClicked(MenuItem item) {
        Integer[] selections = mAdapter.getSelectedIndices();
        if (selections.length < 1) {
            mAdapter.clearSelected();
            return true;
        }
        switch (item.getItemId()) {
            case R.id.image_delete:
                for (int index : selections) {
                    mAdapter.removeItem(index);
                }
                break;
            case R.id.image_share:
                String[] paths = new String[selections.length];
                for (int i = 0; i < selections.length; i++) {
                    paths[i] = mAdapter.getItem(i);
                }
                AppUtils.shareMultipleScreenshot(App.getContext(), paths, mType);
                break;
        }
        mAdapter.clearSelected();
        return true;
    }

    @Override
    public boolean onCabFinished(MaterialCab cab) {
        mAdapter.clearSelected();
        return true;
    }

    @Override
    public void onLoadFinished(List<String> data) {
        if (!data.isEmpty()) {
            mLoadingLayout.setStatus(LoadingLayout.Success);
            mAdapter.setData(data);
        } else {
            mAdapter.clearData();
            mLoadingLayout.setStatus(LoadingLayout.Empty);
        }
    }

    @Override
    public void onError(Throwable e) {
        mLoadingLayout.setStatus(LoadingLayout.Error);
        if (mAdapter != null)
            mAdapter.clearData();
    }

    @Override
    public void showLoading() {
        mLoadingLayout.setStatus(LoadingLayout.Loading);
    }
}
