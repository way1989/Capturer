package com.way.capture.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.Log;
import android.util.Pair;

import com.glidebitmappool.GlideBitmapPool;


/**
 * Created by android on 16-3-8.
 */
public class LongScreenshotUtil {
    private static final String TAG = "LongScreenshotUtil";
    private static final int PURE_LINE_DIF_NUM_MAX = 10;
    private static final int GRAY_PIXEL_RED_DIF_MAX = 50;
    private static final int GRAY_PIXEL_GREEN_DIF_MAX = 16;
    private static final int GRAY_PIXEL_BLUE_DIF_MAX = 40;

    private static final int REDUCE_PIXEL_BY_ONCE = 20;

    private volatile static LongScreenshotUtil sInstance;
    private final int TOP_NOT_COMPARE_HEIGHT;
    private final int BORDER_NOT_COMPARE_WIDTH;
    private final int BOTTOM_NOT_COMPARE_EXTRA_HEIGHT;
    private final int MOVE_Y_MIN;
    private int BOTTOM_NOT_COMPARE_HEIGHT;

    private LongScreenshotUtil() {
        TOP_NOT_COMPARE_HEIGHT = DensityUtil.dp2px(88);//顶部不用对比的高度，即长截屏提示界面的高度。
        BOTTOM_NOT_COMPARE_EXTRA_HEIGHT = DensityUtil.dp2px(48);//底部不用对比的高度，在第一次对比的高度上再加上一些偏移
        BORDER_NOT_COMPARE_WIDTH = DensityUtil.dp2px(16);//每行像素对比时，两边不用对比的宽度，去除滚动条之类的干扰
        MOVE_Y_MIN = DensityUtil.dp2px(4);
    }

    public static LongScreenshotUtil getInstance() {
        if (sInstance == null) {
            synchronized (LongScreenshotUtil.class) {
                if (sInstance == null) {
                    sInstance = new LongScreenshotUtil();
                }
            }
        }
        return sInstance;
    }

    /**
     * 从下往上比较两张图的每一行像素，返回像素开始不同时的行数值，如果两张图片完全相同返回-1
     * 主要用来第一次对比时判断两张图是不是相同，如果不同时，获取两张图底部相同部分的高度，比如虚拟按键的高度或者底部tab的高度
     *
     * @param oldBitmap 需要比较的图
     * @param newBitmap 被比较的图
     * @return 像素开始不同时的行数值
     */
    private int getBottomSameHeight(Bitmap oldBitmap, Bitmap newBitmap) {
        final int oldBitmapWidth = oldBitmap.getWidth();
        final int oldBitmapHeight = oldBitmap.getHeight();

        final int newBitmapWidth = newBitmap.getWidth();
        final int newBitmapHeight = newBitmap.getHeight();

        final int[] oldPixels = new int[oldBitmapWidth];
        final int[] newPixels = new int[newBitmapWidth];

        for (int i = 0; i <= (newBitmapHeight - TOP_NOT_COMPARE_HEIGHT); i += 4) {
            //将oldBitmap中第（oldBitmapHeight - 1 - i）行宽度为oldBitmapWidth的像素取出存入oldPixels数组中。
            oldBitmap.getPixels(oldPixels, 0, oldBitmapWidth, 0, oldBitmapHeight - 1 - i, oldBitmapWidth, 1);
            newBitmap.getPixels(newPixels, 0, newBitmapWidth, 0, newBitmapHeight - 1 - i, newBitmapWidth, 1);

            if (!isLinePixelsEqual(oldPixels, newPixels)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @param bitmap       需要寻找的图片
     * @param screenHeight 屏幕的高度
     * @param offset       偏移量
     * @return 高度
     */
    private int getNotPureLineHeight(Bitmap bitmap, int screenHeight, int offset) {
        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();

        if (height < screenHeight) {
            return -1;
        }

        final int[] pixels = new int[width];
        for (int i = BOTTOM_NOT_COMPARE_HEIGHT + offset; i <= (screenHeight - TOP_NOT_COMPARE_HEIGHT); i += 2) {
            //将第i行宽度为width的像素点存入数组pixels中
            bitmap.getPixels(pixels, 0, width, 0, height - 1 - i, width, 1);
            if (!isPureColorLine(pixels)) {
                return i;
            }
        }
        return -1;
    }


    private Pair<Integer, Integer> compareTwoBitmap(Bitmap oldBitmap, Bitmap newBitmap) {
        final int oldBitmapHeight = oldBitmap.getHeight();

        final int screenHeight = newBitmap.getHeight();//屏幕的高度就是新图的高度

        //第一次对比拿到底部共用部分的高度
        if (oldBitmapHeight == screenHeight) {//first compare
            //bottomSameHeight compute once only
            final int bottomSameHeight = getBottomSameHeight(oldBitmap, newBitmap);
            if (bottomSameHeight < 0 || bottomSameHeight == (screenHeight - TOP_NOT_COMPARE_HEIGHT)) {
                Log.i(TAG, "compareTwoBitmap... two bitmap is same return");
                return null;
            }
            BOTTOM_NOT_COMPARE_HEIGHT = BOTTOM_NOT_COMPARE_EXTRA_HEIGHT + bottomSameHeight;//增加底部不用对比的高度
            Log.i(TAG, "compareTwoBitmap... first compare "
                    + ", bottomSameHeight = " + bottomSameHeight
                    + ", BOTTOM_NOT_COMPARE_HEIGHT = " + BOTTOM_NOT_COMPARE_HEIGHT);
        }

        //改变左图基线5次，确保找到正确的分割线
        for (int i = 0; i < 5; i++) {
            final int baseLine = getNotPureLineHeight(oldBitmap, screenHeight, i * REDUCE_PIXEL_BY_ONCE);
            Pair<Integer, Integer> pair = getDividingLinePair(oldBitmap, newBitmap, baseLine);
            if (pair != null) {
                return pair;
            }
        }

        return null;
    }

    /**
     * 获取两个bitmap重叠部分分割线
     * @param oldBitmap 需要比较的bitmap
     * @param newBitmap
     * @param baseLine
     * @return
     */
    private Pair<Integer, Integer> getDividingLinePair(Bitmap oldBitmap, Bitmap newBitmap, int baseLine) {
        int oldBitmapWidth = oldBitmap.getWidth();
        int oldBitmapHeight = oldBitmap.getHeight();
        int newBitmapWidth = newBitmap.getWidth();
        int newBitmapHeight = newBitmap.getHeight();

        int oldY = baseLine;
        int newY = baseLine;
        int newStartY = 0;
        int count = 0;

        final int[] oldPixels = new int[oldBitmapWidth];
        final int[] newPixels = new int[newBitmapWidth];

        while (newBitmapHeight - newY > TOP_NOT_COMPARE_HEIGHT) {
            oldBitmap.getPixels(oldPixels, 0, oldBitmapWidth, 0, oldBitmapHeight - 1 - oldY, oldBitmapWidth, 1);
            newBitmap.getPixels(newPixels, 0, newBitmapWidth, 0, newBitmapHeight - 1 - newY, newBitmapWidth, 1);
            final boolean equal = isLinePixelsEqual(oldPixels, newPixels);
            if (equal) {
                if (count == 0) {
                    newStartY = newY;
                }
                count++;
                oldY++;
            } else if (count > 0) {
                oldY = baseLine;
                count = 0;
                if (newStartY != 0 && newStartY != newY) {
                    newY = newStartY;
                }
                newStartY = 0;
            }
            newY++;
            if (count >= 50 && Math.abs(oldY - newStartY) > MOVE_Y_MIN) {
                return new Pair<>(oldBitmapHeight - baseLine, newBitmapHeight - newStartY + 1);
            }
        }
        return null;
    }

    /**
     * 比较两行像素颜色值是否相同
     *
     * @param oldPixels 需要比较的像素行
     * @param newPixels 被比较的像素行
     * @return 是否相同
     */
    private boolean isLinePixelsEqual(int[] oldPixels, int[] newPixels) {
        if (oldPixels.length != newPixels.length) {
            return false;
        }

        final int compareWidth = oldPixels.length - BORDER_NOT_COMPARE_WIDTH;
        int differentCount = 0;
        for (int i = BORDER_NOT_COMPARE_WIDTH; i < compareWidth; i += 2) {
            if (oldPixels[i] != newPixels[i]) {
                final int oldRed = Color.red(oldPixels[i]);
                final int oldGreen = Color.green(oldPixels[i]);
                final int oldBlue = Color.blue(oldPixels[i]);

                final int newRed = Color.red(newPixels[i]);
                final int newGreen = Color.green(newPixels[i]);
                final int newBlue = Color.blue(newPixels[i]);

                final int redDifferent = Math.abs(oldRed - newRed);
                final int greenDifferent = Math.abs(oldGreen - newGreen);
                final int blueDifferent = Math.abs(oldBlue - newBlue);

                if (redDifferent > GRAY_PIXEL_RED_DIF_MAX
                        || greenDifferent > GRAY_PIXEL_GREEN_DIF_MAX
                        || blueDifferent > GRAY_PIXEL_BLUE_DIF_MAX) {
                    differentCount++;
                }
                //超过10个像素点颜色值不同，就认为两个像素数组不同
                if (differentCount >= PURE_LINE_DIF_NUM_MAX) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 判断一行像素是否为纯色
     *
     * @param pixels 一行像素
     * @return 是否为纯色
     */
    private boolean isPureColorLine(int[] pixels) {
        int differentCount = 0;
        final int compareWidth = pixels.length - BORDER_NOT_COMPARE_WIDTH;
        for (int i = BORDER_NOT_COMPARE_WIDTH; i < compareWidth; i += 2) {
            if (pixels[i] != pixels[i - 1]) {
                differentCount++;
            }
            if (differentCount > 4 * PURE_LINE_DIF_NUM_MAX) {
                return false;
            }
        }
        return true;
    }

    public Bitmap collageLongBitmap(Bitmap oldBitmap, Bitmap newBitmap) {
        if (oldBitmap == null || oldBitmap.isRecycled()
                || newBitmap == null || newBitmap.isRecycled()) {
            return null;
        }

        final long collageStart = System.currentTimeMillis();
        Pair<Integer, Integer> pair = compareTwoBitmap(oldBitmap, newBitmap);
        Log.d(TAG, "collageLongBitmap compareTwoBitmap end... isSucceed = " + (pair != null)
                + ", cost = " + (System.currentTimeMillis() - collageStart) + "ms");

        if (pair == null) {//failed, stop
            return null;
        }
        int width = oldBitmap.getWidth();
        int height = pair.first + (newBitmap.getHeight() - pair.second);

        Log.i(TAG, "collageLongBitmap start height = " + height
                + ", screenSize = " + newBitmap.getWidth() + "x" + newBitmap.getHeight()
                + ", mOldBitmapEndY = " + pair.first
                + ", mNewBitmapStartY = " + pair.second);
        if (height < oldBitmap.getHeight()) {
            return null;
        }
        final long drawStart = System.currentTimeMillis();
        Bitmap resultBitmap = GlideBitmapPool.getBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(oldBitmap, new Rect(0, 0, width, pair.first),
                new Rect(0, 0, width, pair.first), null);
        canvas.drawBitmap(newBitmap, new Rect(0, pair.second, width, newBitmap.getHeight()),
                new Rect(0, pair.first, width, height), null);
        GlideBitmapPool.putBitmap(oldBitmap);
        GlideBitmapPool.putBitmap(newBitmap);
        Log.d(TAG, "collageLongBitmap drawBitmap end... cost = " + (System.currentTimeMillis() - drawStart) + "ms");

        return resultBitmap;
    }

}
