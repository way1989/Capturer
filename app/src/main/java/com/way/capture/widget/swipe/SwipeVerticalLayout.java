package com.way.capture.widget.swipe;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import com.way.capture.R;

public class SwipeVerticalLayout extends FrameLayout implements SwipeHelper.Callback {
    private static final String TAG = "SwipeVerticalLayout";

    private SwipeHelper mSwipeHelper;
    private Callback mCallback;
    private View mAnimView;

    public SwipeVerticalLayout(Context context) {
        this(context, null);
    }

    public SwipeVerticalLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwipeVerticalLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mSwipeHelper = new SwipeHelper(SwipeHelper.Y, this, context);
        mSwipeHelper.setMinSwipeProgress(0.3f);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mAnimView = findViewById(R.id.global_screenshot);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        float densityScale = getResources().getDisplayMetrics().density;
        mSwipeHelper.setDensityScale(densityScale);
        float pagingTouchSlop = ViewConfiguration.get(getContext()).getScaledPagingTouchSlop();
        mSwipeHelper.setPagingTouchSlop(pagingTouchSlop);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        Log.d(TAG, "onInterceptTouchEvent()");
        if (!isEnabled())
            return true;
        return mSwipeHelper.onInterceptTouchEvent(ev) || super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!isEnabled())
            return true;
        return mSwipeHelper.onTouchEvent(ev) || super.onTouchEvent(ev);
    }

    @Override
    public View getChildAtPosition(MotionEvent ev) {
        if (mAnimView == null) return null;
        final int x = (int) ev.getRawX();
        final int y = (int) ev.getRawY();
        Rect r = new Rect();
        mAnimView.getGlobalVisibleRect(r);
        Log.i(TAG, "screenshot rect = " + r + ", x * y = " + x + " * " + y);
        if (r.contains(x, y))
            return this;
        return null;
    }

    @Override
    public View getChildContentView(View v) {
        return mAnimView;
    }

    @Override
    public boolean canChildBeDismissed(View v) {
        return true;
    }

    @Override
    public boolean isAntiFalsingNeeded() {
        return false;
    }

    @Override
    public void onBeginDrag(View v) {
        // We do this so the underlying ScrollView knows that it won't get
        // the chance to intercept events anymore
        requestDisallowInterceptTouchEvent(true);
        if (mCallback != null)
            mCallback.onBeginDrag(v);
    }

    @Override
    public void onChildDismissed(View v, int direction) {
        Log.i(TAG, "onChildDismissed... direction = " + direction);
        if (mCallback != null)
            mCallback.onChildDismissed(v, direction);
    }

    @Override
    public void onDragCancelled(View v) {
        if (mCallback != null)
            mCallback.onDragCancelled(v);

    }

    @Override
    public void onChildSnappedBack(View animView) {
        if (mCallback != null)
            mCallback.onChildSnappedBack(animView);
    }

    @Override
    public boolean updateSwipeProgress(View animView, boolean dismissable, float swipeProgress) {
        if (mCallback != null)
            return mCallback.updateSwipeProgress(animView, dismissable, swipeProgress);
        return false;
    }

    @Override
    public float getFalsingThresholdFactor() {
        return 1.0f;
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public void setPressListener(SwipeHelper.PressListener listener) {
        mSwipeHelper.setPressListener(listener);
    }

    public interface Callback {
        void onChildDismissed(View v, int direction);

        void onBeginDrag(View v);

        void onDragCancelled(View v);

        void onChildSnappedBack(View animView);

        /**
         * Updates the swipe progress on a child.
         *
         * @return if true, prevents the default alpha fading.
         */
        boolean updateSwipeProgress(View animView, boolean dismissable, float swipeProgress);
    }
}
