package com.way.capture.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import com.way.capture.R;
import com.way.capture.utils.ViewUtils;

public class ArrowView extends View {
    private int mArrowHeight;
    private int mArrowWidth;
    private Paint paint;
    private Path path;
    private Paint arrowPaint;
    private Path arrowPath;
    private float startX;
    private float startY;
    private float endX;
    private float endY;

    public ArrowView(Context context) {
        super(context);
        init();
    }

    public ArrowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ArrowView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mArrowHeight = ViewUtils.dp2px(6);
        mArrowWidth = ViewUtils.dp2px(8);
        final int strokeWidth = ViewUtils.dp2px(2);
        paint = new Paint();
        arrowPaint = new Paint();
        arrowPaint.setAntiAlias(true);
        int color = getResources().getColor(R.color.colorAccent);
        arrowPaint.setColor(color);
        arrowPaint.setStrokeWidth(strokeWidth);
        arrowPaint.setStyle(Paint.Style.FILL);//箭头是个实心三角形，所以用fill
        arrowPath = new Path();
        path = new Path();
        paint.setAntiAlias(true);
        paint.setColor(color);
        paint.setStrokeWidth(strokeWidth);
        paint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        setPath(getWidth() / 2, getHeight(), getWidth() / 2, mArrowHeight);
        setArrowPath();
        canvas.drawPath(path, paint);
        canvas.drawPath(arrowPath, arrowPaint);
    }

    /**
     * 画箭头
     */
    private void setArrowPath() {

        double angle = Math.atan(mArrowWidth / mArrowHeight); // 箭头角度
        double arrowLength = Math.sqrt(Math.pow(mArrowWidth, 2) + Math.pow(mArrowHeight, 2)); // 箭头的长度
        //箭头就是个三角形，我们已经有一个点了，根据箭头的角度和长度，确定另外2个点的位置
        double[] point1 = rotateVec(endX - startX, endY - startY, angle, arrowLength);
        double[] point2 = rotateVec(endX - startX, endY - startY, -angle, arrowLength);
        double point1_x = endX - point1[0];
        double point1_y = endY - mArrowHeight - point1[1];
        double point2_x = endX - point2[0];
        double point2_y = endY - mArrowHeight - point2[1];
        int x3 = (int) point1_x;
        int y3 = (int) point1_y;
        int x4 = (int) point2_x;
        int y4 = (int) point2_y;
        // 画线
        arrowPath.moveTo(endX, endY - mArrowHeight);
        arrowPath.lineTo(x3, y3);
        arrowPath.lineTo(x4, y4);
        arrowPath.close();
    }
    // 计算

    /**
     * @param diffX       X的差值
     * @param diffY       Y的差值
     * @param angle       箭头的角度（箭头三角形的线与直线的角度）
     * @param arrowLength 箭头的长度
     */
    public double[] rotateVec(float diffX, float diffY, double angle, double arrowLength) {
        double arr[] = new double[2];
        // 下面的是公式，得出的是以滑动出的线段末点为中心点旋转angle角度后,线段起点的坐标，这个旋转后的线段也就是“变长了的箭头的三角形的一条边”
        //推导见注释1
        double x = diffX * Math.cos(angle) - diffY * Math.sin(angle);
        double y = diffX * Math.sin(angle) + diffY * Math.cos(angle);
        double d = Math.sqrt(x * x + y * y);
        //根据相似三角形，得出真正的箭头三角形顶点坐标，这里见注释2
        x = x / d * arrowLength;
        y = y / d * arrowLength;
        arr[0] = x;
        arr[1] = y;
        return arr;
    }

    public void setPath(float startX, float startY, float endX, float endY) {
        path.moveTo(startX, startY);
        path.lineTo(endX, endY);
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        invalidate();
    }

    public void clear() {
        path.reset();
        arrowPath.reset();
    }
}

