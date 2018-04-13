package com.way.capture.fragment;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.afollestad.materialcab.MaterialCab;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.michaelflisar.dragselectrecyclerview.DragSelectTouchListener;
import com.michaelflisar.dragselectrecyclerview.DragSelectionProcessor;
import com.way.capture.App;
import com.way.capture.R;
import com.way.capture.activity.DetailsActivity;
import com.way.capture.adapter.ScreenshotAdapter;
import com.way.capture.base.BaseFragment;
import com.way.capture.base.BaseScreenshotFragment;
import com.way.capture.data.DataInfo;
import com.way.capture.utils.AppUtil;
import com.way.capture.utils.ViewUtils;
import com.way.capture.widget.SpaceGridItemDecoration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import me.bakumon.statuslayoutmanager.library.OnStatusChildClickListener;
import me.bakumon.statuslayoutmanager.library.StatusLayoutManager;

/**
 * Created by way on 16/4/10.
 */
public class ScreenshotFragment extends BaseScreenshotFragment implements
        MaterialCab.Callback, ScreenshotContract.View {
    public static final String ARGS_TYPE = "type";
    public static final String EXTRA_DATAS = "extra_datas";
    public static final String EXTRA_STARTING_POSITION = "extra_starting_item_position";
    public static final String EXTRA_CURRENT_POSITION = "extra_current_item_position";
    private static final String TAG = "ScreenshotFragment";
    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;
    private ScreenshotAdapter mAdapter;
    private DragSelectTouchListener mDragSelectTouchListener;
    private StatusLayoutManager mStatusLayoutManager;

    private MaterialCab mCab;
    private boolean mIsDetailsActivityStarted;
    private int mType;
    private boolean hasLoadData;
    private ScreenshotPresenter mScreenshotPresenter;

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
        // mAdapter.restoreInstanceState(savedInstanceState);
        //mCab = MaterialCab.restoreState(savedInstanceState, (AppCompatActivity) getActivity(), this);
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
        // mAdapter.saveInstanceState(outState);
        //if (mCab != null) mCab.saveState(outState);
    }

    @Override
    public boolean onBackPressed() {
        if (mAdapter.getCountSelected() > 0) {
            mAdapter.deselectAll();
            exitCab();
            return true;
        }
        return false;
    }

    @Override
    public void onActivityReenter(Bundle bundle) {
        if (mRecyclerView == null) return;
        int startingPosition = bundle.getInt(EXTRA_STARTING_POSITION);
        int currentPosition = bundle.getInt(EXTRA_CURRENT_POSITION);
        Log.i(TAG, "onActivityReenter startingPosition = " + startingPosition + ", currentPosition = " + currentPosition);

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
                Log.i(TAG, "onActivityReenter startPostponedEnterTransition...");
                getActivity().startPostponedEnterTransition();
                return true;
            }
        });
    }

    @Override
    public void changeSharedElements(List<String> names, Map<String, View> sharedElements, int position) {
        String newTransitionName = mAdapter.getItem(position).path;
        View newSharedElement = mRecyclerView.findViewHolderForAdapterPosition(position).itemView.findViewById(R.id.iv_image);
        Log.i(TAG, "changeSharedElements newSharedElement = " + newSharedElement);
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
        mRecyclerView.addItemDecoration(new SpaceGridItemDecoration(ViewUtils.dp2px(1)));
        mRecyclerView.setLayoutManager(layoutManager);
        mAdapter = new ScreenshotAdapter(R.layout.item_screenshot, mType);

        mRecyclerView.setAdapter(mAdapter);

        mStatusLayoutManager = new StatusLayoutManager.Builder(mRecyclerView)
                .setOnStatusChildClickListener(new OnStatusChildClickListener() {
                    @Override
                    public void onEmptyChildClick(View view) {
                        mStatusLayoutManager.showLoadingLayout();
                        mScreenshotPresenter.getData(mType);
                    }

                    @Override
                    public void onErrorChildClick(View view) {
                        mStatusLayoutManager.showLoadingLayout();
                        mScreenshotPresenter.getData(mType);
                    }

                    @Override
                    public void onCustomerChildClick(View view) {

                    }
                })
                .setDefaultLayoutsBackgroundColor(getResources().getColor(R.color.colorPrimary))
                .build();
        mStatusLayoutManager.showLoadingLayout();

        mAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                if (mCab != null && mCab.isActive()) {
                    mAdapter.toggleSelection(position);
                    mCab.setTitleRes(R.string.cab_title_x, mAdapter.getCountSelected());
                } else {
                    ImageView imageView = view.findViewById(R.id.iv_image);
                    if (!mIsDetailsActivityStarted) {
                        mIsDetailsActivityStarted = true;
                        Intent intent = new Intent(getActivity(), DetailsActivity.class);
                        intent.putExtra(ARGS_TYPE, mType);
                        ArrayList<DataInfo> lists = new ArrayList<>(mAdapter.getData());
                        intent.putExtra(EXTRA_DATAS, lists);
                        intent.putExtra(EXTRA_STARTING_POSITION, position);
                        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(getActivity(),
                                imageView, imageView.getTransitionName()).toBundle());
                    }
                }
            }
        });
        mAdapter.setOnItemLongClickListener(new BaseQuickAdapter.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(BaseQuickAdapter adapter, View view, int position) {
                // if one item is long pressed, we start the drag selection like following:
                // we just call this function and pass in the position of the first selected item
                // the selection processor does take care to update the positions selection mode correctly
                // and will correctly transform the touch events so that they can be directly applied to your adapter!!!
                mDragSelectTouchListener.startDragSelection(position);
                return false;
            }
        });
        DragSelectionProcessor dragSelectionProcessor = new DragSelectionProcessor(new DragSelectionProcessor.ISelectionHandler() {
            @Override
            public HashSet<Integer> getSelection() {
                return mAdapter.getSelection();
            }

            @Override
            public boolean isSelected(int index) {
                return mAdapter.getSelection().contains(index);
            }

            @Override
            public void updateSelection(int start, int end, boolean isSelected, boolean calledFromOnStart) {
                mAdapter.selectRange(start, end, isSelected);
                Log.d(TAG, "updateSelection: ");
                if (getSelection().size() > 0) {
                    if (mCab == null) {
                        mCab = new MaterialCab((AppCompatActivity) getActivity(), R.id.cab_stub)
                                .setBackgroundColor(getResources().getColor(R.color.colorAccentExtra))
                                .setMenu(R.menu.menu_drag_selection)
                                .setCloseDrawableRes(R.drawable.ic_clear)
                                .start(ScreenshotFragment.this);
                    }
                    mCab.setTitleRes(R.string.cab_title_x, getSelection().size());
                } else {
                    exitCab();
                }
            }
        }).withMode(DragSelectionProcessor.Mode.Simple);
        mDragSelectTouchListener = new DragSelectTouchListener()
                .withSelectListener(dragSelectionProcessor)
                .withBottomOffset(-ViewUtils.getNavigationBarHeight());
        mRecyclerView.addOnItemTouchListener(mDragSelectTouchListener);
    }

    @Override
    protected void initData() {
        super.initData();
        mScreenshotPresenter = new ScreenshotPresenter(this);
       /* RxBus.getInstance().toObservable(RxEvent.NewPathEvent.class)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(this.<RxEvent.NewPathEvent>bindUntilEvent(FragmentEvent.DESTROY))
                .subscribe(new Consumer<RxEvent.NewPathEvent>() {
                    @Override
                    public void accept(RxEvent.NewPathEvent newPathEvent) {
                        final int type = newPathEvent.type;
                        final String path = newPathEvent.path;
                        if (type == mType) {
                            //mAdapter.addData(path);
                        }
                    }
                });*/
        loadData();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        loadData();
    }

    private void loadData() {
        if (isAdded() && getUserVisibleHint() && !hasLoadData && mScreenshotPresenter != null) {
            mScreenshotPresenter.getData(mType);
            hasLoadData = true;
        }
    }

    @Override
    public boolean onCabCreated(MaterialCab cab, Menu menu) {
        return true;
    }

    @Override
    public boolean onCabItemClicked(MenuItem item) {
        HashSet<Integer> selections = mAdapter.getSelection();
        switch (item.getItemId()) {
            case R.id.image_delete:
                List<DataInfo> dataInfos = new ArrayList<>();
                for (int index : selections) {
                    dataInfos.add(mAdapter.getItem(index));
                }
                mAdapter.removeMulData(dataInfos);
                mAdapter.deselectAll();
                AppUtil.deleteMultipleScreenshot(App.getContext(), dataInfos);// TODO: 2018/4/6 should in thread!!
                exitCab();
                if (mAdapter.getItemCount() < 1) {
                    mStatusLayoutManager.showEmptyLayout();
                }
                break;
            case R.id.image_share:
                String[] paths = new String[selections.size()];
                for (int i = 0; i < selections.size(); i++) {
                    paths[i] = mAdapter.getItem(i).path;
                }
                AppUtil.shareMultipleScreenshot(App.getContext(), paths, mType);
                mAdapter.deselectAll();
                break;
            case R.id.image_select_all:
                if (!item.isChecked()) {
                    mAdapter.selectAll();
                    mCab.setTitleRes(R.string.cab_title_x, mAdapter.getSelection().size());
                    item.setTitle(R.string.clear_all);
                    item.setChecked(true);
                } else {
                    mAdapter.deselectAll();
                    exitCab();
                    item.setChecked(false);
                }
                break;
        }
        return true;
    }

    private void exitCab() {
        if (mCab != null && mCab.isActive()) {
            mCab.reset().finish();
        }
        mCab = null;
    }

    @Override
    public boolean onCabFinished(MaterialCab cab) {
        mAdapter.deselectAll();
        mCab = null;
        return true;
    }

    @Override
    public void onLoadFinished(List<DataInfo> data) {
        if (!data.isEmpty()) {
            mStatusLayoutManager.showSuccessLayout();
            mAdapter.setNewData(data);
        } else {
            mAdapter.clearData();
            mStatusLayoutManager.showEmptyLayout();
        }
    }

    @Override
    public void onError(Throwable e) {
        mStatusLayoutManager.showErrorLayout();
        if (mAdapter != null)
            mAdapter.clearData();
    }

}
