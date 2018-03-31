package com.way.capture.adapter;

import android.support.v4.view.ViewCompat;
import android.view.View;
import android.widget.ImageView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.way.capture.R;
import com.way.capture.data.DataInfo;
import com.way.capture.utils.glide.GlideHelper;

import java.util.HashSet;

/**
 * Created by android on 16-2-1.
 */
public class ScreenshotAdapter extends BaseQuickAdapter<String, BaseViewHolder> {
    private int mType;
    private HashSet<Integer> mSelected;

    public ScreenshotAdapter(int layoutResId, int type) {
        super(layoutResId);
        mType = type;
        setHasStableIds(true);
        mSelected = new HashSet<>();
    }

    public void removeItem(int pos) {
        mData.remove(pos);
        notifyItemChanged(pos);
    }

    public void clearData() {
        mData.clear();
        notifyDataSetChanged();
    }

    public String getItem(int pos) {
        return mData.get(pos);
    }

    @Override
    protected void convert(BaseViewHolder helper, String item) {
        ImageView imageView = helper.getView(R.id.iv_image);
        ImageView selectImageView = helper.getView(R.id.cb_selected);
        View coverView = helper.getView(R.id.lay_mask);
        ImageView videoIndicator = helper.getView(R.id.iv_is_gif);
        GlideHelper.loadResourceBitmapCenterCrop(item, imageView);
        boolean isSelected = mSelected.contains(helper.getAdapterPosition());
        imageView.animate().scaleX(isSelected ? 0.8f : 1.0f).scaleY(isSelected ? 0.8f : 1.0f);
        selectImageView.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        coverView.setVisibility(isSelected ? View.VISIBLE : View.GONE);

        switch (mType) {
            case DataInfo.TYPE_SCREEN_SHOT:
                videoIndicator.setVisibility(View.GONE);
                break;
            case DataInfo.TYPE_SCREEN_GIF:
                videoIndicator.setVisibility(View.VISIBLE);
                videoIndicator.setImageResource(R.drawable.ic_gif);
                break;
            case DataInfo.TYPE_SCREEN_RECORD:
                videoIndicator.setVisibility(View.VISIBLE);
                videoIndicator.setImageResource(R.drawable.ic_play);
                break;
            default:
                break;
        }

        // 把每个图片视图设置不同的Transition名称, 防止在一个视图内有多个相同的名称, 在变换的时候造成混乱
        // Fragment支持多个View进行变换, 使用适配器时, 需要加以区分
        ViewCompat.setTransitionName(imageView, item);
        helper.itemView.setTag(R.id.tag_item, helper.getAdapterPosition());
    }
    // ----------------------
    // Selection
    // ----------------------

    public void toggleSelection(int pos) {
        if (mSelected.contains(pos))
            mSelected.remove(pos);
        else
            mSelected.add(pos);
        notifyItemChanged(pos);
    }

    public void select(int pos, boolean selected) {
        if (selected)
            mSelected.add(pos);
        else
            mSelected.remove(pos);
        notifyItemChanged(pos);
    }

    public void selectRange(int start, int end, boolean selected) {
        for (int i = start; i <= end; i++) {
            if (selected)
                mSelected.add(i);
            else
                mSelected.remove(i);
        }
        notifyItemRangeChanged(start, end - start + 1);
    }

    public void deselectAll() {
        // this is not beautiful...
        mSelected.clear();
        notifyDataSetChanged();
    }

    public void selectAll() {
        for (int i = 0; i < getData().size(); i++)
            mSelected.add(i);
        notifyDataSetChanged();
    }

    public int getCountSelected() {
        return mSelected.size();
    }

    public HashSet<Integer> getSelection() {
        return mSelected;
    }
}
