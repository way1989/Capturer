package com.way.captain.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;

import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.fonts.MaterialIcons;
import com.ogaclejapan.arclayout.ArcLayout;
import com.way.captain.R;
import com.way.captain.utils.AnimatorUtils;
import com.way.captain.utils.ViewUtils;
import com.way.captain.screenshot.ShellCmdUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by way on 16/3/27.
 */
public class FloatMenuDialog extends Dialog implements View.OnClickListener{
    private ArcLayout mArcLayout;
    private ImageView mCenterItem;
    private View.OnClickListener mListener;
    private boolean isHideAnimPlaying;

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
        mCenterItem = (ImageView) findViewById(R.id.menu_screnshot_center);
        mArcLayout.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                cancel();
                return true;
            }
        });
        mCenterItem.setImageDrawable(new IconDrawable(this.getContext().getApplicationContext(),
                MaterialIcons.md_home).color(Color.WHITE).actionBarSize());
        mCenterItem.setOnClickListener(this);
        for (int i = 0, size = mArcLayout.getChildCount(); i < size; i++) {
            ImageView button = (ImageView) mArcLayout.getChildAt(i);
            switch (button.getId()) {
                case R.id.menu_normal_screenshot:
                    button.setImageDrawable(new IconDrawable(this.getContext().getApplicationContext(),
                            MaterialIcons.md_cast_connected).color(Color.WHITE).actionBarSize());
                    break;
                case R.id.menu_screenrecord:
                    button.setImageDrawable(new IconDrawable(this.getContext().getApplicationContext(),
                            MaterialIcons.md_theaters).color(Color.WHITE).actionBarSize());
                    break;
                case R.id.menu_long_screenshot:
                    if (!ShellCmdUtils.isDeviceRoot()) {
                        button.setVisibility(View.GONE);
                        continue;
                    }
                    button.setImageDrawable(new IconDrawable(this.getContext().getApplicationContext(),
                            MaterialIcons.md_view_day).color(Color.WHITE).actionBarSize());
                    break;
                case R.id.menu_free_screenshot:
                    button.setImageDrawable(new IconDrawable(this.getContext().getApplicationContext(),
                            MaterialIcons.md_crop_free).color(Color.WHITE).actionBarSize());
                    break;
                case R.id.menu_rect_screenshot:
                    button.setImageDrawable(new IconDrawable(this.getContext().getApplicationContext(),
                            MaterialIcons.md_crop).color(Color.WHITE).actionBarSize());
                    break;
            }
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
        //super.cancel();
        dismiss();
    }

    @Override
    public void dismiss() {
        hideMenu();
        //super.dismiss();
    }

    private void superDimiss() {
        super.dismiss();
    }

    private void showMenu() {
        //中心button动画
        mCenterItem.setScaleX(0f);
        mCenterItem.setScaleY(0f);
        mCenterItem.setAlpha(0f);
        Animator animator = AnimatorUtils.of(mCenterItem, AnimatorUtils.ofAlpha(0f, 1f), AnimatorUtils.ofScaleX(0f, 1f), AnimatorUtils.ofScaleY(0f, 1f));
        animator.setInterpolator(new DecelerateInterpolator());
        animator.setDuration(100L);

        //周围button动画数组
        List<Animator> animList = new ArrayList<>();
        for (int i = 0, len = mArcLayout.getChildCount(); i < len; i++) {
            animList.add(createShowItemAnimator(mArcLayout.getChildAt(i), (i + 1) * 100));
        }

        //总动画
        final AnimatorSet animSet = new AnimatorSet();
        animSet.setInterpolator(new OvershootInterpolator());
        animSet.playTogether(animList);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                animSet.start();//中心动画结束后，开始周围button动画
            }
        });
        animator.start();//中心动画开始播放
    }

    private void hideMenu() {
        if (isHideAnimPlaying) return;
        isHideAnimPlaying = true;//标记为动画开始

        //周围button动画数组
        List<Animator> animList = new ArrayList<>();
        for (int i = mArcLayout.getChildCount() - 1; i >= 0; i--) {
            animList.add(createHideItemAnimator(mArcLayout.getChildAt(i), (i + 1) * 80L));
        }
        //中心button动画
        final Animator animator = AnimatorUtils.of(mCenterItem, AnimatorUtils.ofAlpha(1f, 0f), AnimatorUtils.ofScaleX(1f, 0f), AnimatorUtils.ofScaleY(1f, 0f));
        animator.setDuration(100L);
        animator.setInterpolator(new AccelerateInterpolator());
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                isHideAnimPlaying = false;
                superDimiss();//中心button动画结束，整个过程结束，dialog消失
                if(mListener != null && mClickView != null){
                    mListener.onClick(mClickView);
                    mClickView = null;
                }
            }
        });

        //总动画
        AnimatorSet animSet = new AnimatorSet();
        animSet.setInterpolator(new AnticipateInterpolator());
        animSet.playTogether(animList);
        animSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                animator.start();//周围button动画播完之后再播中心button动画
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
        item.setScaleX(0f);
        item.setScaleY(0f);

        Animator anim = AnimatorUtils.of(
                item,
                AnimatorUtils.ofTranslationX(x, 0f),
                AnimatorUtils.ofTranslationY(y, 0f),
                AnimatorUtils.ofAlpha(0f, 1f),
                AnimatorUtils.ofScaleX(0f, 1f),
                AnimatorUtils.ofScaleY(0f, 1f)
        );
        anim.setDuration(duration);
        return anim;
    }

    private Animator createHideItemAnimator(final View item, long duration) {
        final float centerX = ViewUtils.getCenterX(mCenterItem);
        final float centerY = ViewUtils.getCenterY(mCenterItem);
        final float x = centerX - ViewUtils.getCenterX(item);
        final float y = centerY - ViewUtils.getCenterY(item);

        Animator anim = AnimatorUtils.of(
                item,
                AnimatorUtils.ofTranslationX(0f, x),
                AnimatorUtils.ofTranslationY(0f, y),
                AnimatorUtils.ofAlpha(1f, 0f),
                AnimatorUtils.ofScaleX(1f, 0f),
                AnimatorUtils.ofScaleY(1f, 0f)
        );
        anim.setDuration(duration);

        return anim;
    }
    private View mClickView;
    @Override
    public void onClick(View v) {
        mClickView = v;
        dismiss();
    }
}
