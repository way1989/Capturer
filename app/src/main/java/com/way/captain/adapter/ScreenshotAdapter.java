package com.way.captain.adapter;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.way.captain.R;
import com.way.captain.data.DataInfo;
import com.way.captain.data.DataProvider;
import com.way.captain.utils.glide.GlideHelper;
import com.way.captain.widget.SimpleTagImageView;

/**
 * Created by android on 16-2-1.
 */
public class ScreenshotAdapter extends RecyclerView.Adapter<ScreenshotAdapter.ViewHolder> {
    private LayoutInflater mInflater;
    private DataProvider mDataProvider = new DataProvider();
    private int mType;
    private OnItemClickListener mListener;

    public ScreenshotAdapter(Context context, int type, OnItemClickListener listener) {
        mType = type;
        mListener = listener;
        mInflater = LayoutInflater.from(context);
        setHasStableIds(true);
    }

    public void setDatas(DataProvider dataProvider) {
        mDataProvider = dataProvider;
        notifyDataSetChanged();
    }

    public void removeItem(int pos) {
        mDataProvider.removeItem(pos);
        notifyItemChanged(pos);
    }

    public void clearData() {
        mDataProvider.clear();
        notifyDataSetChanged();
    }

    public String getItem(int pos) {
        return mDataProvider.getItem(pos);
    }

    @Override
    public ScreenshotAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View v = mInflater.inflate(R.layout.item_screenshot, parent, false);
        ViewHolder holder = new ViewHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(ScreenshotAdapter.ViewHolder holder, int position) {
        String info = mDataProvider.getItem(position);
        GlideHelper.loadResourceBitmap(info, holder.image);
        switch (mType) {
            case DataInfo.TYPE_SCREEN_SHOT:
                holder.image.setTagEnable(false);
                break;
            case DataInfo.TYPE_SCREEN_GIF:
                holder.image.setTagEnable(true);
                holder.image.setTagText("GIF");
                break;
            case DataInfo.TYPE_SCREEN_RECORD:
                holder.image.setTagEnable(true);
                holder.image.setTagText("MP4");
                holder.videoIndicator.setVisibility(View.VISIBLE);
                break;
        }

        // 把每个图片视图设置不同的Transition名称, 防止在一个视图内有多个相同的名称, 在变换的时候造成混乱
        // Fragment支持多个View进行变换, 使用适配器时, 需要加以区分
        ViewCompat.setTransitionName(holder.image, info);
        holder.itemView.setTag(R.id.tag_item, position);
        holder.popupMenuButton.setTag(R.id.tag_item, position);
        if(holder.itemView.getScaleX() == 0f) {
            reset(holder.itemView);
        }
    }

    private void reset(View root) {
        root.setScaleX(1);
        root.setScaleY(1);
        root.setAlpha(1);
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return mDataProvider.getCount();
    }

    @Override
    public long getItemId(int position) {
        return mDataProvider.getItem(position).hashCode();
    }

    public interface OnItemClickListener {
        void onItemClick(View v);

        void onPopupMenuClick(View v);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public final SimpleTagImageView image;
        public final ImageButton popupMenuButton;
        ImageView videoIndicator;

        public ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);

            image = (SimpleTagImageView) itemView.findViewById(R.id.ic_screenshot);
            videoIndicator = (ImageView) itemView.findViewById(R.id.video_indicator);
            popupMenuButton = (ImageButton) itemView.findViewById(R.id.popup_menu_button);
            popupMenuButton.setOnClickListener(this);

        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.popup_menu_button:
                    mListener.onPopupMenuClick(v);
                    break;
                default:
                    mListener.onItemClick(v);
                    break;
            }
        }


    }

}
