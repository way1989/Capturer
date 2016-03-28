package com.way.captain.service;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;

import com.ogaclejapan.arclayout.ArcLayout;
import com.way.captain.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by way on 16/3/27.
 */
public class FloatMenuDialog extends Dialog {
    private ArcLayout mArcLayout;
    private View mCenterItem;
    private View.OnClickListener mListener;

    public FloatMenuDialog(Context context, int themeResId) {
        super(context, themeResId);
        setCanceledOnTouchOutside(true);
        getWindow().setType(WindowManager.LayoutParams.TYPE_TOAST);
    }

    public void setOnClickListener(View.OnClickListener listener) {
        mListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.float_dialog_menu);
        mArcLayout = (ArcLayout) findViewById(R.id.arc_layout);
        mCenterItem = findViewById(R.id.menu_center);
        mArcLayout.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                cancel();
                return true;
            }
        });
        mCenterItem.setOnClickListener(mListener);
        for (int i = 0, size = mArcLayout.getChildCount(); i < size; i++) {
            Button button = (Button) mArcLayout.getChildAt(i);
            /*if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && i < 3) {
                button.setVisibility(View.GONE);
                continue;
            }*/
            //button.setColorFilter(Color.WHITE);
            button.setOnClickListener(mListener);
        }
    }

    @Override
    public void show() {
        super.show();
        //showMenu();
        showMenu(mArcLayout, mCenterItem);
    }

    @Override
    public void cancel() {
        //super.cancel();
        //hideMenu();
        hideMenu(mArcLayout, mCenterItem);
    }

    @Override
    public void dismiss() {
        hideMenu(mArcLayout, mCenterItem);
        //super.dismiss();
    }

    private void superDimiss() {
        super.dismiss();
    }

    private void showMenu(ArcLayout arcLayout, View centerItem) {

        List<Animator> animList = new ArrayList<>();

        animList.add(createShowItemAnimator(centerItem, centerItem));

        for (int i = 0, len = arcLayout.getChildCount(); i < len; i++) {
            animList.add(createShowItemAnimator(centerItem, arcLayout.getChildAt(i)));
        }

        AnimatorSet animSet = new AnimatorSet();
        animSet.playSequentially(animList);
        animSet.start();
    }

    private void hideMenu(ArcLayout arcLayout, View centerItem) {
        List<Animator> animList = new ArrayList<>();

        for (int i = arcLayout.getChildCount() - 1; i >= 0; i--) {
            animList.add(createHideItemAnimator(centerItem, arcLayout.getChildAt(i)));
        }

        animList.add(createHideItemAnimator(centerItem, centerItem));


        AnimatorSet animSet = new AnimatorSet();
        animSet.playSequentially(animList);
        //animSet.playTogether(animList);
        animSet.start();
        animSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                superDimiss();
            }

        });
    }

    private Animator createShowItemAnimator(View centerItem, View item) {
        float dx = centerItem.getX() - item.getX();
        float dy = centerItem.getY() - item.getY();

        item.setScaleX(0f);
        item.setScaleY(0f);
        item.setTranslationX(dx);
        item.setTranslationY(dy);

        Animator anim = ObjectAnimator.ofPropertyValuesHolder(
                item,
                AnimatorUtils.scaleX(0f, 1f),
                AnimatorUtils.scaleY(0f, 1f),
                AnimatorUtils.translationX(dx, 0f),
                AnimatorUtils.translationY(dy, 0f)
        );

        anim.setInterpolator(new DecelerateInterpolator());
        anim.setDuration(50);
        return anim;
    }

    private Animator createHideItemAnimator(View centerItem, final View item) {
        final float dx = centerItem.getX() - item.getX();
        final float dy = centerItem.getY() - item.getY();

        Animator anim = ObjectAnimator.ofPropertyValuesHolder(
                item,
                AnimatorUtils.scaleX(1f, 0f),
                AnimatorUtils.scaleY(1f, 0f),
                AnimatorUtils.translationX(0f, dx),
                AnimatorUtils.translationY(0f, dy)
        );

        anim.setInterpolator(new DecelerateInterpolator());
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                item.setTranslationX(0f);
                item.setTranslationY(0f);
            }
        });
        anim.setDuration(50);
        return anim;
    }


    private void showMenu() {
        //menuLayout.setVisibility(View.VISIBLE);

        List<Animator> animList = new ArrayList<>();

        for (int i = 0, len = mArcLayout.getChildCount(); i < len; i++) {
            animList.add(createShowItemAnimator(mArcLayout.getChildAt(i)));
        }

        AnimatorSet animSet = new AnimatorSet();
        animSet.setDuration(400);
        animSet.setInterpolator(new OvershootInterpolator());
        animSet.playTogether(animList);
        animSet.start();
    }

    private void hideMenu() {

        List<Animator> animList = new ArrayList<>();

        for (int i = mArcLayout.getChildCount() - 1; i >= 0; i--) {
            animList.add(createHideItemAnimator(mArcLayout.getChildAt(i)));
        }

        AnimatorSet animSet = new AnimatorSet();
        animSet.setDuration(400);
        animSet.setInterpolator(new AnticipateInterpolator());
        animSet.playTogether(animList);
        animSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                //menuLayout.setVisibility(View.INVISIBLE);
                superDimiss();
            }
        });
        animSet.start();

    }

    private Animator createShowItemAnimator(View item) {

        float dx = mCenterItem.getX() - item.getX();
        float dy = mCenterItem.getY() - item.getY();

        item.setRotation(0f);
        item.setTranslationX(dx);
        item.setTranslationY(dy);

        Animator anim = ObjectAnimator.ofPropertyValuesHolder(
                item,
                AnimatorUtils.rotation(0f, 720f),
                AnimatorUtils.translationX(dx, 0f),
                AnimatorUtils.translationY(dy, 0f)
        );

        return anim;
    }

    private Animator createHideItemAnimator(final View item) {
        float dx = mCenterItem.getX() - item.getX();
        float dy = mCenterItem.getY() - item.getY();

        Animator anim = ObjectAnimator.ofPropertyValuesHolder(
                item,
                AnimatorUtils.rotation(720f, 0f),
                AnimatorUtils.translationX(0f, dx),
                AnimatorUtils.translationY(0f, dy)
        );

        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                item.setTranslationX(0f);
                item.setTranslationY(0f);
            }
        });

        return anim;
    }
}
