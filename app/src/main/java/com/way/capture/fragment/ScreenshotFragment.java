package com.way.capture.fragment;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.Toast;

import com.afollestad.dragselectrecyclerview.DragSelectRecyclerView;
import com.afollestad.dragselectrecyclerview.DragSelectRecyclerViewAdapter;
import com.afollestad.materialcab.MaterialCab;
import com.way.capture.R;
import com.way.capture.activity.DetailsActivity;
import com.way.capture.adapter.ScreenshotAdapter;
import com.way.capture.data.DataInfo;
import com.way.capture.data.DataLoader;
import com.way.capture.data.DataProvider;
import com.way.capture.utils.DensityUtil;
import com.weavey.loading.lib.LoadingLayout;

import java.util.List;
import java.util.Map;

import butterknife.BindView;

/**
 * Created by way on 16/4/10.
 */
public class ScreenshotFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener,
        LoaderManager.LoaderCallbacks<DataLoader.Result>, ScreenshotAdapter.OnItemClickListener,
        DragSelectRecyclerViewAdapter.SelectionListener, MaterialCab.Callback {
    public static final String ARGS_TYPE = "type";
    public static final String EXTRA_DATAS = "extra_datas";
    public static final String EXTRA_STARTING_POSITION = "extra_starting_item_position";
    public static final String EXTRA_CURRENT_POSITION = "extra_current_item_position";
    private static final int SCREENSHOT_LOADER_ID = 0;
    @BindView(R.id.loading_layout)
    LoadingLayout mLoadingLayout;
    @BindView(R.id.recycler_view)
    DragSelectRecyclerView mRecyclerView;
    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout mSwipeRefresh;
    private DataProvider mDataProvider = new DataProvider();
    private ScreenshotAdapter mAdapter;

    private MaterialCab mCab;
    private boolean mIsDetailsActivityStarted;

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
        String newTransitionName = mDataProvider.getItem(position);
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
    protected void initWidget(View root) {
        super.initWidget(root);
        int type = mBundle.getInt(ARGS_TYPE, DataInfo.TYPE_SCREEN_SHOT);

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        mRecyclerView.addItemDecoration(new SpaceGridItemDecoration(DensityUtil.dip2px(getContext(), 2)));
        mRecyclerView.setLayoutManager(layoutManager);
        mAdapter = new ScreenshotAdapter(getContext(), type, this);
        mAdapter.setSelectionListener(this);
        mRecyclerView.setAdapter(mAdapter);

        mSwipeRefresh.setColorSchemeResources(R.color.colorAccent);
        mSwipeRefresh.setOnRefreshListener(this);
    }

    @Override
    protected void initData() {
        super.initData();
        mLoadingLayout.setStatus(LoadingLayout.Loading);
        getLoaderManager().initLoader(SCREENSHOT_LOADER_ID, null, ScreenshotFragment.this);
    }

    @Override
    public void onRefresh() {
        mSwipeRefresh.setRefreshing(true);
        mSwipeRefresh.setEnabled(false);
        if (getLoaderManager().getLoader(SCREENSHOT_LOADER_ID) == null)
            getLoaderManager().initLoader(SCREENSHOT_LOADER_ID, null, this);
        else
            getLoaderManager().restartLoader(SCREENSHOT_LOADER_ID, null, this);
    }

    @Override
    public Loader<DataLoader.Result> onCreateLoader(int id, Bundle args) {
        return new DataLoader(getContext(), getArguments().getInt(ARGS_TYPE, DataInfo.TYPE_SCREEN_SHOT));
    }

    @Override
    public void onLoadFinished(Loader<DataLoader.Result> loader, DataLoader.Result data) {
        mSwipeRefresh.setRefreshing(false);
        mSwipeRefresh.setEnabled(true);
        if (data.dataInfoes != null && !data.dataInfoes.isEmpty()) {
            mLoadingLayout.setStatus(LoadingLayout.Success);
            mDataProvider.setData(data.dataInfoes);
            mAdapter.setDatas(mDataProvider);
        } else {
            mLoadingLayout.setStatus(LoadingLayout.Empty);
        }
    }

    @Override
    public void onLoaderReset(Loader<DataLoader.Result> loader) {
        mSwipeRefresh.setRefreshing(false);
        if (mAdapter != null)
            mAdapter.clearData();
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
                intent.putExtra(ARGS_TYPE, getArguments().getInt(ARGS_TYPE, DataInfo.TYPE_SCREEN_SHOT));
                intent.putStringArrayListExtra(EXTRA_DATAS, mDataProvider.getData());
                intent.putExtra(EXTRA_STARTING_POSITION, position);
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(getActivity(), imageView,
                        imageView.getTransitionName()).toBundle());
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
            mSwipeRefresh.setEnabled(false);
            if (mCab == null) {
                mCab = new MaterialCab((AppCompatActivity) getActivity(), R.id.cab_stub)
                        .setMenu(R.menu.menu_info)
                        .setCloseDrawableRes(R.drawable.ic_clear_white_24dp)
                        .start(this);
            }
            mCab.setTitleRes(R.string.cab_title_x, count);
        } else if (mCab != null && mCab.isActive()) {
            mSwipeRefresh.setEnabled(true);
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
        if (item.getItemId() == R.id.done) {
            StringBuilder sb = new StringBuilder();
            int traverse = 0;
            for (Integer index : mAdapter.getSelectedIndices()) {
                if (traverse > 0) sb.append(", ");
                sb.append(mAdapter.getItem(index));
                traverse++;
            }
            Toast.makeText(getContext(),
                    String.format("Selected letters (%d): %s", mAdapter.getSelectedCount(), sb.toString()),
                    Toast.LENGTH_LONG).show();
            mAdapter.clearSelected();
        }
        return true;
    }

    @Override
    public boolean onCabFinished(MaterialCab cab) {
        mAdapter.clearSelected();
        return true;
    }
}
