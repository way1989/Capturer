package com.way.captain.activity;

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

import com.way.captain.R;

public class SplashActivity extends AppCompatActivity {
    private static final ArgbEvaluator ARGB_EVALUATOR = new ArgbEvaluator();
    private static final int HANDLER_MESSAGE_ANIMATION = 0;
    private static final int HANDLER_MESSAGE_NEXT_ACTIVITY = 1;
    ImageView image;
    TextView title;
    View foreMask;
    View logo;
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == HANDLER_MESSAGE_ANIMATION) {
                playAnimator();
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

    private void setStatusBarNavigationBarColor() {
        final android.view.Window window = getWindow();
        ObjectAnimator statusBarColor = ObjectAnimator.ofInt(window,
                "statusBarColor", window.getStatusBarColor(), getResources().getColor(R.color.colorPrimaryDark));
        statusBarColor.setEvaluator(new ArgbEvaluator());
        statusBarColor.setDuration(1500L);
        statusBarColor.start();

        ObjectAnimator navigationBarColor = ObjectAnimator.ofInt(window,
                "navigationBarColor", window.getNavigationBarColor(), getResources().getColor(R.color.colorPrimaryDark));
        navigationBarColor.setEvaluator(new ArgbEvaluator());
        navigationBarColor.setDuration(1500L);
        navigationBarColor.start();
    }

    protected void setBackgroundColor() {
        ColorDrawable colorDrawable = new ColorDrawable(Color.BLACK);
        ObjectAnimator.ofObject(colorDrawable, "color", ARGB_EVALUATOR, getResources().getColor(R.color.colorPrimaryDark))
                .setDuration(1500L).start();
        image.setBackground(colorDrawable);
    }

    private void playAnimator() {
        foreMask.animate().alpha(0f).withEndAction(new Runnable() {
            @Override
            public void run() {
                mHandler.sendEmptyMessageDelayed(HANDLER_MESSAGE_NEXT_ACTIVITY, 500L);
            }
        }).setDuration(750);
        setStatusBarNavigationBarColor();
        setBackgroundColor();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        initViews();
        settingBackground();
    }

    private void settingBackground() {

//        Glide.with(image.getContext()).load(R.drawable.zhongqiu_init_photo)
//                .override(DensityUtil.getDisplayWidth(this), DensityUtil.getDisplayHeight(this))
//                .into(image);
        //image.setImageResource(R.drawable.zhongqiu_init_photo);
    }

    private void initViews() {
        image = (ImageView) findViewById(R.id.image);
        title = (TextView) findViewById(R.id.title);
        foreMask = findViewById(R.id.foreMask);
        logo = findViewById(R.id.logo);
        mHandler.sendEmptyMessageDelayed(HANDLER_MESSAGE_ANIMATION, 900L);
    }
}
