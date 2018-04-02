package com.way.capture.utils;

import android.util.Log;

import org.junit.Test;

import io.reactivex.functions.Consumer;

import static org.junit.Assert.*;

public class RxCountDownTest {
    private static final String TAG = "RxCountDownTest";
    @Test
    public void getCountDown() {
        RxCountDown.getCountDown(5).subscribe(new Consumer<Long>() {
            @Override
            public void accept(Long time) {
                Log.d(TAG, "getCountDown: time = " + time);
            }
        });
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}