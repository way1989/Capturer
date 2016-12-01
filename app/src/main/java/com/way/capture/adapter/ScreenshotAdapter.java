package com.way.capture.adapter;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.afollestad.dragselectrecyclerview.DragSelectRecyclerViewAdapter;
import com.way.capture.R;
import com.way.capture.data.DataInfo;
import com.way.capture.data.DataProvider;
import com.way.capture.utils.glide.GlideHelper;

import java.util.ArrayList;

/**
 * Created by android on 16-2-1.
 */
public class ScreenshotAdapter extends DragSelectRecyclerViewAdapter<ScreenshotAdapter.ViewHolder> {
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
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ScreenshotAdapter.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        String info = mDataProvider.getItem(position);
        GlideHelper.loadResourceBitmapCenterCrop(info, holder.image);
        boolean isSelected = isIndexSelected(position);
        holder.image.animate().scaleX(isSelected ? 0.8f : 1.0f).scaleY(isSelected ? 0.8f : 1.0f);
        holder.selectImageView.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        holder.coverView.setVisibility(isSelected ? View.VISIBLE : View.GONE);

        switch (mType) {
            case DataInfo.TYPE_SCREEN_SHOT:
                holder.videoIndicator.setVisibility(View.GONE);
                break;
            case DataInfo.TYPE_SCREEN_GIF:
                holder.videoIndicator.setVisibility(View.VISIBLE);
                holder.videoIndicator.setImageResource(R.drawable.gif);
                break;
            case DataInfo.TYPE_SCREEN_RECORD:
                holder.videoIndicator.setVisibility(View.VISIBLE);
                holder.videoIndicator.setImageResource(R.drawable.ic_gallery_play);
                break;
        }

        // 把每个图片视图设置不同的Transition名称, 防止在一个视图内有多个相同的名称, 在变换的时候造成混乱
        // Fragment支持多个View进行变换, 使用适配器时, 需要加以区分
        ViewCompat.setTransitionName(holder.image, info);
        holder.itemView.setTag(R.id.tag_item, position);
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

    public ArrayList<String> getData() {
        return mDataProvider.getData();
    }

    public void setData(ArrayList<String> data) {
        mDataProvider.setData(data);
        notifyDataSetChanged();
    }

    public void addData(String path) {
        mDataProvider.addData(path);
        notifyDataSetChanged();
    }

    public interface OnItemClickListener {
        void onItemClick(View v);

        void onLongClick(int position);
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        ImageView image;
        ImageView videoIndicator;
        ImageView selectImageView;
        View coverView;

         ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            image = (ImageView) itemView.findViewById(R.id.iv_image);
            videoIndicator = (ImageView) itemView.findViewById(R.id.iv_is_gif);
            selectImageView = (ImageView) itemView.findViewById(R.id.cb_selected);
            coverView = itemView.findViewById(R.id.lay_mask);
        }

        @Override
        public void onClick(View v) {
            mListener.onItemClick(v);
        }


        @Override
        public boolean onLongClick(View view) {
            mListener.onLongClick(getAdapterPosition());
            return true;
        }
    }

}
