package com.way.capture.utils.glide;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;

public class GlideHelper {

    public static void loadResource(String path, @NonNull ImageView image) {
        RequestOptions options = new RequestOptions().fitCenter();

        Glide.with(image.getContext())
                .load(path)
                .apply(options)
                .into(image);
    }

    public static void loadResourceBitmap(String path, @NonNull ImageView image) {
        RequestOptions options = new RequestOptions().fitCenter();
        Glide.with(image.getContext())
                .asBitmap()
                .load(path)
                .apply(options)
                .into(image);
    }

    public static void loadResourceBitmap(String path, @NonNull ImageView image,
                                          RequestListener<Bitmap> requestListener) {
        RequestOptions options = new RequestOptions().fitCenter();
        Glide.with(image.getContext())
                .asBitmap()
                .load(path)
                .listener(requestListener)
                .apply(options)
                .into(image);
    }

    public static void loadResourceBitmapCenterCrop(String path, @NonNull ImageView image) {
        RequestOptions options = new RequestOptions().centerCrop();
        Glide.with(image.getContext())
                .asBitmap()
                .load(path)
                .apply(options)
                .into(image);
    }
}
