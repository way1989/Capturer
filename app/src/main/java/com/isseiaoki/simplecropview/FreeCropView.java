package com.isseiaoki.simplecropview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposePathEffect;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnimationUtils;

import java.util.ArrayList;

public class FreeCropView extends ViewGroup {
    private static final String TAG = "FreeCropView";
    private static final float TOUCH_TOLERANCE = 3;
    private static final int FADE_ANIMATION_RATE = 16;
    private static final boolean GESTURE_RENDERING_ANTIALIAS = true;
    private static final boolean DITHER_FLAG = true;
    private static final long FADE_DURATION = 150;
    private final Paint mGesturePaint = new Paint();
    private final AccelerateDecelerateInterpolator mInterpolator = new AccelerateDecelerateInterpolator();
    private final FadeOutRunnable mFadingOut = new FadeOutRunnable();
    private final Rect mInvalidRect = new Rect();
    private final Path mPath = new Path();
    private final ArrayList<GesturePoint> mStrokeBuffer = new ArrayList<GesturePoint>(100);
    GestureStroke drawStroke;
    ClipStroke clipStroke;
    private boolean mIsGesturing = false;
    private boolean mIsListeningForGestures;
    private int mCurrentColor;
    private int mCertainGestureColor = Color.parseColor("#ff00b2a9");
    private float mGestureStrokeWidth = 5.0f;
    private int mInvalidateExtraBorder = 10;
    private float mGestureStrokeLengthThreshold = 50.0f;
    // fading out effect
    private boolean mIsFadingOut = false;
    private float mFadingAlpha = 1.0f;
    private long mFadeOffset = 500;
    private long mFadingStart;
    private boolean mFadingHasStarted;
    private float mTotalLength;
    private float mX;
    private float mY;
    private float mCurveEndX;
    private float mCurveEndY;
    private int mImageWidth;// 绘画板宽度
    private int mImageHeight;// 绘画板高度
    private Bitmap mBaseBitmap;
    private Rect mImageRect;
    private float mScale = 1.0f;

    public FreeCropView(Context context) {
        this(context, null);
    }

    public FreeCropView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FreeCropView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public static Bitmap resizeBitmap(Bitmap bitmap, float scale) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        bitmap.recycle();
        return resizedBitmap;
    }

    private void init() {
        mImageRect = new Rect();
        setWillNotDraw(false);

        final Paint gesturePaint = mGesturePaint;
        gesturePaint.setAntiAlias(GESTURE_RENDERING_ANTIALIAS);
        gesturePaint.setColor(mCertainGestureColor);
        gesturePaint.setStyle(Paint.Style.STROKE);
        gesturePaint.setStrokeJoin(Paint.Join.ROUND);
        gesturePaint.setStrokeCap(Paint.Cap.ROUND);
        gesturePaint.setStrokeWidth(mGestureStrokeWidth);
        gesturePaint.setDither(DITHER_FLAG);
        CornerPathEffect cornerPathEffect = new CornerPathEffect(10);
        DashPathEffect dashPathEffect = new DashPathEffect(new float[]{20, 10, 10, 10}, 5);
        PathEffect pathEffect = new ComposePathEffect(cornerPathEffect, dashPathEffect);
        //DashPathEffect dashStyle = new DashPathEffect(new float[] { 20, 10, 10, 10 }, 5);// 创建虚线边框样式
        gesturePaint.setPathEffect(pathEffect);
        setPaintAlpha(255);
        setCurrentColor(mCertainGestureColor);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

        if (mImageWidth <= 0 || mImageHeight <= 0) {
            return;
        }

        int contentWidth = right - left;
        int contentHeight = bottom - top;
        int viewWidth = contentWidth;
        int viewHeight = contentHeight;
        float widthRatio = (viewWidth * 1.00f) / mImageWidth;
        float heightRatio = (viewHeight * 1.00f) / mImageHeight;
        float ratio = widthRatio < heightRatio ? widthRatio : heightRatio;
        setScale(ratio);
        int realWidth = Math.round(mImageWidth * ratio);
        int realHeight = Math.round(mImageHeight * ratio);

        int imageLeft = (contentWidth - realWidth) / 2;
        int imageTop = (contentHeight - realHeight) / 2;
        int imageRight = imageLeft + realWidth;
        int imageBottom = imageTop + realHeight;
        mImageRect.set(imageLeft, imageTop, imageRight, imageBottom);
        Log.i("FreeCrop", "mImageRect = " + mImageRect);
    }

    private void setScale(float mScale) {
        this.mScale = mScale;
    }

    public void setFreeCropBitmap(Bitmap bitmap) {

        if (bitmap == null) {
            Log.e(TAG, "seFreeCropBitmap : bitmap == null");
            return;
        }

        mImageWidth = bitmap.getWidth();
        mImageHeight = bitmap.getHeight();
        mBaseBitmap = bitmap;
        Log.i(TAG, "seFreeCropBitmap : bitmap = " + bitmap + ", mImageWidth = " + mImageWidth + ", mImageHeight = "
                + mImageHeight);
        requestLayout();
        invalidate();
    }

    public boolean reset() {
        clear(false);
        invalidate();
        return true;
    }

    /**
     * 返回自由裁剪最终结果
     *
     * @return 自由裁剪最终结果
     */
    public Bitmap getFreeCropBitmap() {
        if (mBaseBitmap == null || mPath.isEmpty()) {
            return null;
        }
        Bitmap resizeBitmap = resizeBitmap(mBaseBitmap, mScale);
        Bitmap bitmap = Bitmap.createBitmap(resizeBitmap.getWidth(), resizeBitmap.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        //canvas.drawColor(0xFFFFFFFF);// 设置画布为透明背景
        // PaintFlagsDrawFilter dfd = new
        // PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG,
        // Paint.FILTER_BITMAP_FLAG);
        // canvas.setDrawFilter(dfd);
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        //paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.clipPath(clipStroke.getPath(), Region.Op.REPLACE);// 使用路径剪切画布
        canvas.drawBitmap(resizeBitmap, 0, 0, paint);
        Log.i("FreeCrop", "clipPath = " + clipStroke.boundingBox);
        canvas.save();
        if (resizeBitmap != null && !resizeBitmap.isRecycled())
            resizeBitmap.recycle();
        int x = 0, y = 0, w = 0, h = 0;
        if (bitmap != null) {
            int l = (int) (clipStroke.boundingBox.left);
            int t = (int) (clipStroke.boundingBox.top);
            int r = (int) (clipStroke.boundingBox.right);
            int b = (int) (clipStroke.boundingBox.bottom);
            x = l;
            y = t;
            w = r - l;
            h = b - t;
        }
        Bitmap cropped = Bitmap.createBitmap(bitmap, x, y, w, h, null, false);
        if (bitmap != null && !bitmap.isRecycled())
            bitmap.recycle();
        return cropped;
    }

    public boolean hasCropBitmap() {
        if (mBaseBitmap != null && !mPath.isEmpty())
            return true;
        return false;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelClearAnimation();
    }

    private void setPaintAlpha(int alpha) {
        alpha += alpha >> 7;
        final int baseAlpha = mCurrentColor >>> 24;
        final int useAlpha = baseAlpha * alpha >> 8;
        mGesturePaint.setColor((mCurrentColor << 8 >>> 8) | (useAlpha << 24));
    }

    public boolean isGesturing() {
        return mIsGesturing;
    }

    private void setCurrentColor(int color) {
        mCurrentColor = color;
        if (mFadingHasStarted) {
            setPaintAlpha((int) (255 * mFadingAlpha));
        } else {
            setPaintAlpha(255);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.i(TAG, "onDraw  mBaseBitmap = " + mBaseBitmap + ", mImageRect = " + mImageRect);
        if (mBaseBitmap == null)
            return;
        canvas.drawBitmap(mBaseBitmap, null, mImageRect, null);
        if (!mIsListeningForGestures && !mPath.isEmpty()) {
            canvas.save();
            canvas.clipPath(drawStroke.getPath(), Region.Op.DIFFERENCE);
            canvas.drawColor(0x88000000);
            mGesturePaint.setStrokeWidth(mGestureStrokeWidth);
            mGesturePaint.setColor(Color.parseColor("#88ffffff"));
            canvas.drawPath(mPath, mGesturePaint);// 画手势轨迹
            canvas.restore();

        } else {
            mGesturePaint.setStrokeWidth(mGestureStrokeWidth + 2);
            mGesturePaint.setColor(Color.WHITE);
            canvas.drawPath(mPath, mGesturePaint);
            mGesturePaint.setStrokeWidth(mGestureStrokeWidth);
            mGesturePaint.setColor(Color.parseColor("#ff00b2a9"));
            canvas.drawPath(mPath, mGesturePaint);// 画手势轨迹
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (isEnabled()) {
            // final boolean cancelDispatch = mIsGesturing || !mPath.isEmpty();
            // Log.i(TAG, "onTouchEvent processEvent cancelDispatch = " +
            // cancelDispatch);
            processEvent(event);

            // if (cancelDispatch) {
            // event.setAction(MotionEvent.ACTION_CANCEL);
            // }
            return true;
        }
        return super.onTouchEvent(event);
    }

    private boolean processEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchDown(event);
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (mIsListeningForGestures) {
                    Rect rect = touchMove(event);
                    if (rect != null) {
                        invalidate(rect);
                    }
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mIsListeningForGestures) {
                    touchUp(event, false);
                    invalidate();
                    return true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mIsListeningForGestures) {
                    touchUp(event, true);
                    invalidate();
                    return true;
                }
        }

        return false;
    }

    private void touchDown(MotionEvent event) {
        Log.i(TAG, "onTouchEvent touchDown  = ");
        mIsListeningForGestures = true;
        if(mListener != null) mListener.onStart();

        float x = event.getX();
        float y = event.getY();

        mX = x;
        mY = y;

        mTotalLength = 0;
        mIsGesturing = false;

        // if there is fading out going on, stop it.
        if (mFadingHasStarted) {
            cancelClearAnimation();
        } else if (mIsFadingOut) {
            // setPaintAlpha(255);
            mIsFadingOut = false;
            mFadingHasStarted = false;
            removeCallbacks(mFadingOut);
        }

        mPath.reset();
        mPath.moveTo(x, y);
        mStrokeBuffer.clear();
        if (x < mImageRect.left)
            x = mImageRect.left;
        if (x > mImageRect.right)
            x = mImageRect.right;
        if (y < mImageRect.top)
            y = mImageRect.top;
        if (y > mImageRect.bottom)
            y = mImageRect.bottom;
        mStrokeBuffer.add(new GesturePoint(x, y, event.getEventTime()));
        final int border = mInvalidateExtraBorder;
        mInvalidRect.set((int) x - border, (int) y - border, (int) x + border, (int) y + border);

        mCurveEndX = x;
        mCurveEndY = y;

    }

    private Rect touchMove(MotionEvent event) {
        Log.i(TAG, "onTouchEvent touchMove  = ");
        Rect areaToRefresh = null;

        float x = event.getX();
        float y = event.getY();

        final float previousX = mX;
        final float previousY = mY;

        final float dx = Math.abs(x - previousX);
        final float dy = Math.abs(y - previousY);

        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            areaToRefresh = mInvalidRect;

            // start with the curve end
            final int border = mInvalidateExtraBorder;
            areaToRefresh.set((int) mCurveEndX - border, (int) mCurveEndY - border, (int) mCurveEndX + border,
                    (int) mCurveEndY + border);

            float cX = mCurveEndX = (x + previousX) / 2;
            float cY = mCurveEndY = (y + previousY) / 2;
            mPath.quadTo(previousX, previousY, cX, cY);

            // union with the control point of the new curve
            areaToRefresh.union((int) previousX - border, (int) previousY - border, (int) previousX + border,
                    (int) previousY + border);

            // union with the end point of the new curve
            areaToRefresh.union((int) cX - border, (int) cY - border, (int) cX + border, (int) cY + border);

            mX = x;
            mY = y;
            if (!mIsGesturing) {
                mTotalLength += (float) Math.sqrt(dx * dx + dy * dy);

                if (mTotalLength > mGestureStrokeLengthThreshold) {
                    mIsGesturing = true;
                }
            }
            if (x < mImageRect.left)
                x = mImageRect.left;
            if (x > mImageRect.right)
                x = mImageRect.right;
            if (y < mImageRect.top)
                y = mImageRect.top;
            if (y > mImageRect.bottom)
                y = mImageRect.bottom;
            mStrokeBuffer.add(new GesturePoint(x, y, event.getEventTime()));

        }
        Log.i(TAG, "onTouchEvent touchMove  areaToRefresh = " + areaToRefresh);
        return areaToRefresh;
    }

    private void touchUp(MotionEvent event, boolean cancel) {
        mIsListeningForGestures = false;
        if(mListener != null) mListener.onEnd();

        drawStroke = new GestureStroke(mStrokeBuffer);
        clipStroke = new ClipStroke(mImageRect, mStrokeBuffer);
        // A gesture wasn't started or was cancelled
        if (!mPath.isEmpty() && !cancel && mIsGesturing) {

        } else {
            cancelGesture(event);
        }
        mStrokeBuffer.clear();
        mIsGesturing = false;
    }

    private void cancelGesture(MotionEvent event) {

        clear(false);
    }

    public void cancelGesture() {
        mIsListeningForGestures = false;

        // pass the event to handlers
        final long now = SystemClock.uptimeMillis();
        final MotionEvent event = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0);

        event.recycle();

        clear(false);
        mIsGesturing = false;
        mStrokeBuffer.clear();
    }

    public void clear(boolean animated) {
        removeCallbacks(mFadingOut);

        if (animated && !mPath.isEmpty()) {
            mFadingAlpha = 1.0f;
            mIsFadingOut = true;
            mFadingHasStarted = false;
            mFadingStart = AnimationUtils.currentAnimationTimeMillis() + mFadeOffset;

            postDelayed(mFadingOut, mFadeOffset);
        } else {
            mFadingAlpha = 1.0f;
            mIsFadingOut = false;
            mFadingHasStarted = false;

            mPath.rewind();
            invalidate();
        }
    }

    public void cancelClearAnimation() {
        mIsFadingOut = false;
        mFadingHasStarted = false;
        removeCallbacks(mFadingOut);
        mPath.rewind();
    }

    private class FadeOutRunnable implements Runnable {

        public void run() {
            if (mIsFadingOut) {
                final long now = AnimationUtils.currentAnimationTimeMillis();
                final long duration = now - mFadingStart;
                if (duration > FADE_DURATION) {
                    mIsFadingOut = false;
                    mFadingHasStarted = false;
                    mPath.rewind();
                } else {
                    mFadingHasStarted = true;
                    float interpolatedTime = Math.max(0.0f, Math.min(1.0f, duration / (float) FADE_DURATION));
                    mFadingAlpha = 1.0f - mInterpolator.getInterpolation(interpolatedTime);
                    postDelayed(this, FADE_ANIMATION_RATE);
                }
            } else {
                mFadingHasStarted = false;
                mPath.rewind();
            }

            invalidate();
        }
    }

    private OnStateListener mListener;
    public void setOnStateListener(OnStateListener listener){
        mListener = listener;
    }
    public interface OnStateListener{
        public void onStart();
        public void onEnd();
    }
}