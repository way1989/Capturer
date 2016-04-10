package com.way.captain.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.way.captain.R;

/**
 * Created by way on 16/4/10.
 */
public class DetailFragment extends Fragment {
    private static final String ARG_NUMBER = "arg_number";
    private static final String ARG_USE_TRANSITION = "arg_useTransition";
    private static final String ARG_TRANSITION_NAME = "arg_transitionName";
    private static final String ARG_PATH = "arg_path";

    public static Fragment newInstance(int position, boolean useTransition, String transitionName) {
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_NUMBER, position);
        bundle.putBoolean(ARG_USE_TRANSITION, useTransition);
        if (useTransition)
            bundle.putString(ARG_TRANSITION_NAME, transitionName);
        DetailFragment detailFragment = new DetailFragment();
        detailFragment.setArguments(bundle);

        return detailFragment;
    }

    public static DetailFragment newInstance(String path, int position) {
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_NUMBER, position);
        bundle.putString(ARG_PATH, path);
        DetailFragment detailFragment = new DetailFragment();
        detailFragment.setArguments(bundle);

        return detailFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detail_layout, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        final ActionBar ab = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        final int number = getArguments().getInt(ARG_NUMBER);
        final String path = getArguments().getString(ARG_PATH);
        ImageView imageView = (ImageView) view.findViewById(R.id.detail_image);
        Glide.with(imageView.getContext())
                .load(path)
                .dontAnimate()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(imageView);
    }
}
