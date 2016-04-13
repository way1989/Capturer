package com.way.captain.fragment;

import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.PopupMenu;

import com.way.captain.R;
import com.way.captain.activity.DetailsActivity;
import com.way.captain.adapter.ScreenshotAdapter;
import com.way.captain.data.DataInfo;
import com.way.captain.data.DataLoader;
import com.way.captain.data.DataProvider;
import com.way.captain.widget.LoadingEmptyContainer;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by way on 16/4/10.
 */
public class ScreenshotFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener,
        LoaderManager.LoaderCallbacks<DataLoader.Result>, ScreenshotAdapter.OnItemClickListener,
        PopupMenu.OnMenuItemClickListener {
    public static final String ARGS_TYPE = "type";
    public static final String EXTRA_DATAS = "extra_datas";
    public static final String EXTRA_STARTING_POSITION = "extra_starting_item_position";
    public static final String EXTRA_CURRENT_POSITION = "extra_current_item_position";
    private static final int SCREENSHOT_LOADER_ID = 0;
    private static final String SCREENSHOT_SHARE_SUBJECT_TEMPLATE = "Screenshot (%s)";
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private DataProvider mDataProvider = new DataProvider();
    private ScreenshotAdapter mScreenshotAdapter;
    /**
     * Loading container and no results container
     */
    private LoadingEmptyContainer mLoadingEmptyContainer;
    private int mClickPosition;
    private boolean mIsDetailsActivityStarted;

    public static BaseFragment newInstance(int type) {
        Bundle args = new Bundle();
        args.putInt(ARGS_TYPE, type);
        ScreenshotFragment fragment = new ScreenshotFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public boolean onBackPressed() {
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
        View newSharedElement = mRecyclerView.findViewHolderForAdapterPosition(position).itemView.findViewById(R.id.ic_screenshot);
        Log.i("way", "changeSharedElements newSharedElement = " + newSharedElement);
        if (newSharedElement != null) {
            names.clear();
            names.add(newTransitionName);
            sharedElements.clear();
            sharedElements.put(newTransitionName, newSharedElement);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mIsDetailsActivityStarted = false;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_screenshot, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        int type = getArguments().getInt(ARGS_TYPE, DataInfo.TYPE_SCREEN_SHOT);

        initLoadingEmptyView(view, type);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerview);
        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 3);
        mRecyclerView.setLayoutManager(layoutManager);
        mScreenshotAdapter = new ScreenshotAdapter(getContext(), type, this);
        mRecyclerView.setAdapter(mScreenshotAdapter);

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        getLoaderManager().initLoader(SCREENSHOT_LOADER_ID, null, this);
    }

    private void initLoadingEmptyView(View view, int type) {
        mLoadingEmptyContainer = (LoadingEmptyContainer) view.findViewById(R.id.loading_empty_container);
        switch (type) {
            case DataInfo.TYPE_SCREEN_SHOT:
                mLoadingEmptyContainer.getNoResultsContainer().setSecondaryText(R.string.screenshot_no_result_summary);
                break;
            case DataInfo.TYPE_SCREEN_GIF:
                mLoadingEmptyContainer.getNoResultsContainer().setSecondaryText(R.string.gif_no_result_summary);
                break;
            case DataInfo.TYPE_SCREEN_RECORD:
                mLoadingEmptyContainer.getNoResultsContainer().setSecondaryText(R.string.video_no_result_summary);
                break;
        }
    }

    @Override
    public void onRefresh() {
        mSwipeRefreshLayout.setRefreshing(true);
        getLoaderManager().restartLoader(SCREENSHOT_LOADER_ID, null, this);
    }

    @Override
    public Loader<DataLoader.Result> onCreateLoader(int id, Bundle args) {
        mLoadingEmptyContainer.showLoading();
        return new DataLoader(getContext(), getArguments().getInt(ARGS_TYPE, DataInfo.TYPE_SCREEN_SHOT));
    }

    @Override
    public void onLoadFinished(Loader<DataLoader.Result> loader, DataLoader.Result data) {
        mSwipeRefreshLayout.setRefreshing(false);
        if (data.dataInfoes != null && !data.dataInfoes.isEmpty()) {
            mLoadingEmptyContainer.setVisibility(View.INVISIBLE);
            mDataProvider.setData(data.dataInfoes);
            mScreenshotAdapter.setDatas(mDataProvider);
        } else {
            mLoadingEmptyContainer.showNoResults();
        }
    }

    @Override
    public void onLoaderReset(Loader<DataLoader.Result> loader) {
        mSwipeRefreshLayout.setRefreshing(false);
        if (mScreenshotAdapter != null)
            mScreenshotAdapter.clearData();
    }

    @Override
    public void onItemClick(View v) {
        int position = (Integer) v.getTag(R.id.tag_item);
        ImageView imageView = (ImageView) v.findViewById(R.id.ic_screenshot);
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

    @Override
    public void onPopupMenuClick(View v) {
        mClickPosition = (Integer) v.getTag(R.id.tag_item);
        // create the popup menu
        android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(getContext(), v);
        final Menu menu = popupMenu.getMenu();
        popupMenu.getMenuInflater().inflate(R.menu.menu_screenshot_item, menu);
        // hook up the click listener
        popupMenu.setOnMenuItemClickListener(this);
        // show it
        popupMenu.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.gif_item_share:
                shareScreenshot();
                return true;
            case R.id.gif_item_delete:
                deleteScreenshot();
                return true;
            default:
                break;
        }
        return false;
    }

    private void shareScreenshot() {
        String subjectDate = DateFormat.getDateTimeInstance().format(new Date(System.currentTimeMillis()));
        String subject = String.format(SCREENSHOT_SHARE_SUBJECT_TEMPLATE, subjectDate);
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("image/png");
        sharingIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(mScreenshotAdapter.getItem(mClickPosition))));
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        Intent chooserIntent = Intent.createChooser(sharingIntent, null);
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(chooserIntent);
    }

    private void deleteScreenshot() {
        mScreenshotAdapter.removeItem(mClickPosition);
        mScreenshotAdapter.notifyItemRemoved(mClickPosition);
        Snackbar snackbar = Snackbar.make(
                mRecyclerView,
                R.string.snack_bar_text_item_removed,
                Snackbar.LENGTH_LONG);

        snackbar.setAction(R.string.snack_bar_action_undo, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = mDataProvider.undoLastRemoval();
                if (position >= 0) {
                    mScreenshotAdapter.notifyItemInserted(position);
                    mRecyclerView.scrollToPosition(position);
                    mLoadingEmptyContainer.setVisibility(View.INVISIBLE);
                }
            }
        });
        snackbar.setActionTextColor(Color.WHITE);
        snackbar.setCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                super.onDismissed(snackbar, event);
                mDataProvider.deleteLastRemoval();
            }
        });
        snackbar.show();
        if (mDataProvider.getCount() == 0) {
            mLoadingEmptyContainer.showNoResults();
        } else {
            mLoadingEmptyContainer.setVisibility(View.INVISIBLE);
        }
    }

}
