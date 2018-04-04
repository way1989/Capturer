package com.way.capture.utils;

import android.app.Application;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.MainThreadDisposable;

public class RxShake extends Observable<Boolean> {
    private static final String TAG = "RxShake";
    private SensorManager mSensorManager;

    public RxShake(Application application) {
        mSensorManager = (SensorManager) application.getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    protected void subscribeActual(Observer<? super Boolean> observer) {
        RxShake.Listener listener = new RxShake.Listener(mSensorManager, observer);
        observer.onSubscribe(listener);
        Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (sensor != null) {
            mSensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME);
            Log.d(TAG, "subscribeActual: start listen shake...");
        } else {
            Log.e(TAG, "subscribeActual: sensor is null..");
            observer.onError(new Throwable("sensor is null..."));
        }
    }

    private static final class Listener extends MainThreadDisposable implements SensorEventListener {
        private static final int SPEED_THRESHOLD = 60;// 这个值越大需要越大的力气来摇晃手机
        private static final long UPDATE_INTERVAL_TIME = 50L;//50ms处理一次
        private final Observer<? super Boolean> observer;
        private SensorManager mSensorManager;
        private float mLastX;
        private float mLastY;
        private float mLastZ;
        private long mLastUpdateTime;

        public Listener(SensorManager sensorManager, Observer<? super Boolean> observer) {
            mSensorManager = sensorManager;
            this.observer = observer;
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            long currentUpdateTime = System.currentTimeMillis();
            long timeInterval = currentUpdateTime - mLastUpdateTime;
            if (timeInterval < UPDATE_INTERVAL_TIME) {
                return;
            }
            mLastUpdateTime = currentUpdateTime;

            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            float deltaX = x - mLastX;
            float deltaY = y - mLastY;
            float deltaZ = z - mLastZ;

            mLastX = x;
            mLastY = y;
            mLastZ = z;

            double speed = (Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) / timeInterval) * 100;
            if (speed >= SPEED_THRESHOLD) {
                observer.onNext(true);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        @Override
        protected void onDispose() {
            Log.d(TAG, "onDispose: shake finish...");
            mSensorManager.unregisterListener(this);
        }

    }
}
