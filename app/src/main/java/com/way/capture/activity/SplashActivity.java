package com.way.capture.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.way.capture.R;

import java.util.ArrayList;
import java.util.List;

public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "SplashActivity";
    private static final ArgbEvaluator ARGB_EVALUATOR = new ArgbEvaluator();
    private static final int HANDLER_MESSAGE_ANIMATION = 0;
    private static final int HANDLER_MESSAGE_NEXT_ACTIVITY = 1;
    ImageView image;
    TextView title;
    View foreMask;
    View logo;
    ColorDrawable colorDrawable;
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == HANDLER_MESSAGE_ANIMATION) {
                //playAnimator();
                playColorAnimator();
            } else if (msg.what == HANDLER_MESSAGE_NEXT_ACTIVITY) {
                next();
            }
        }
    };


    private void next() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.scroll_in, R.anim.scroll_out);
        finish();
    }

    private void playColorAnimator() {
        //foreMask.setAlpha(0f);//rest the foreMask alpha to 0
        foreMask.animate().alpha(0f);
        final List<Animator> animList = new ArrayList<>();
        final int toColor = getResources().getColor(R.color.colorPrimaryDark);
        //final int toColor = Color.parseColor("#1F1F1F");
        final android.view.Window window = getWindow();
        ObjectAnimator statusBarColor = ObjectAnimator.ofInt(window,
                "statusBarColor", window.getStatusBarColor(), toColor);
        statusBarColor.setEvaluator(ARGB_EVALUATOR);
        animList.add(statusBarColor);

        ObjectAnimator navigationBarColor = ObjectAnimator.ofInt(window,
                "navigationBarColor", window.getNavigationBarColor(), toColor);
        navigationBarColor.setEvaluator(ARGB_EVALUATOR);
        animList.add(navigationBarColor);

        ObjectAnimator backgroundColor = ObjectAnimator.ofObject(colorDrawable, "color", ARGB_EVALUATOR, toColor);
        animList.add(backgroundColor);

        final AnimatorSet animSet = new AnimatorSet();
        animSet.setDuration(1000L);
        animSet.playTogether(animList);
        animSet.start();

        animSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mHandler.sendEmptyMessageDelayed(HANDLER_MESSAGE_NEXT_ACTIVITY, 500L);

            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        initViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
        mHandler = null;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void initViews() {
        image = (ImageView) findViewById(R.id.image);
        title = (TextView) findViewById(R.id.title);
        foreMask = findViewById(R.id.foreMask);
        logo = findViewById(R.id.logo);
        colorDrawable = new ColorDrawable(Color.BLACK);
        image.setBackground(colorDrawable);
        mHandler.sendEmptyMessageDelayed(HANDLER_MESSAGE_ANIMATION, 900L);
    }


}
