package com.way.capture.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.OvershootInterpolator;

import com.ogaclejapan.arclayout.ArcLayout;
import com.way.capture.R;
import com.way.capture.core.screenrecord.CheatSheet;
import com.way.capture.utils.AnimatorUtil;
import com.way.capture.utils.ViewUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by way on 16/3/27.
 */
public class FloatMenuDialog extends Dialog implements View.OnClickListener {
    private ArcLayout mArcLayout;
    private View mCenterItem;
    private View.OnClickListener mListener;
    private boolean isHideAnimPlaying;

    public FloatMenuDialog(Context context, int themeResId) {
        super(context, themeResId);
        setCanceledOnTouchOutside(true);
        getWindow().setType(ViewUtils.getFloatType());
    }

    public void setOnClickListener(View.OnClickListener listener) {
        mListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.float_dialog_menu);
        mArcLayout = findViewById(R.id.arc_layout);
        mCenterItem = findViewById(R.id.menu_screnshot_center);
        mArcLayout.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                cancel();
                return true;
            }
        });

        for (int i = 0, size = mArcLayout.getChildCount(); i < size; i++) {
            View button = mArcLayout.getChildAt(i);
            CheatSheet.setup(button);
            button.setOnClickListener(this);
        }
    }

    @Override
    public void show() {
        super.show();
        showMenu();
    }

    @Override
    public void cancel() {
        dismiss();
    }

    @Override
    public void dismiss() {
        hideMenu();
    }

    private void superDismiss() {
        super.dismiss();
    }

    private void showMenu() {

        //周围button动画数组
        List<Animator> animList = new ArrayList<>();
        for (int i = 0, len = mArcLayout.getChildCount(); i < len; i++) {
            animList.add(createShowItemAnimator(mArcLayout.getChildAt(i), (i + 1) * 100));
        }

        //总动画
        final AnimatorSet animSet = new AnimatorSet();
        animSet.setInterpolator(new OvershootInterpolator());
        animSet.playTogether(animList);

        animSet.start();//中心动画结束后，开始周围button动画

    }

    private void hideMenu() {
        if (isHideAnimPlaying) return;
        isHideAnimPlaying = true;//标记为动画开始

        //周围button动画数组
        List<Animator> animList = new ArrayList<>();
        for (int i = mArcLayout.getChildCount() - 1; i >= 0; i--) {
            animList.add(createHideItemAnimator(mArcLayout.getChildAt(i), (i + 1) * 100L));
        }

        //总动画
        AnimatorSet animSet = new AnimatorSet();
        animSet.setInterpolator(new AnticipateInterpolator());
        animSet.playTogether(animList);
        animSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                isHideAnimPlaying = false;
                superDismiss();//中心button动画结束，整个过程结束，dialog消失

            }
        });
        animSet.start();
    }

    private Animator createShowItemAnimator(View item, long duration) {
        final float centerX = ViewUtils.getCenterX(mCenterItem);
        final float centerY = ViewUtils.getCenterY(mCenterItem);
        final float x = centerX - ViewUtils.getCenterX(item);
        final float y = centerY - ViewUtils.getCenterY(item);

        item.setTranslationX(x);
        item.setTranslationY(y);
        item.setAlpha(0f);

        Animator anim = AnimatorUtil.of(
                item,
                AnimatorUtil.ofTranslationX(x, 0f),
                AnimatorUtil.ofTranslationY(y, 0f),
                AnimatorUtil.ofAlpha(0f, 1f)
        );
        anim.setDuration(duration);
        return anim;
    }

    private Animator createHideItemAnimator(final View item, long duration) {
        final float centerX = ViewUtils.getCenterX(mCenterItem);
        final float centerY = ViewUtils.getCenterY(mCenterItem);
        final float x = centerX - ViewUtils.getCenterX(item);
        final float y = centerY - ViewUtils.getCenterY(item);

        Animator anim = AnimatorUtil.of(
                item,
                AnimatorUtil.ofTranslationX(0f, x),
                AnimatorUtil.ofTranslationY(0f, y),
                AnimatorUtil.ofAlpha(1f, 0f)
        );
        anim.setDuration(duration);

        return anim;
    }

    @Override
    public void onClick(View v) {
        dismiss();
        if (mListener != null) {
            mListener.onClick(v);
        }
    }
}
