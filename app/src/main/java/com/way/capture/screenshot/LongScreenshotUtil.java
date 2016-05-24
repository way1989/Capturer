package com.way.capture.screenshot;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.Log;

import com.way.capture.utils.DensityUtil;


/**
 * Created by android on 16-3-8.
 */
public class LongScreenshotUtil {
    private static final String TAG = "LongScreenshotUtil";
    private static final int PURE_LINE_DIF_NUM_MAX = 10;
    private static final int GRAY_PIXEL_RED_DIF_MAX = 50;
    private static final int GRAY_PIXEL_GREEN_DIF_MAX = 16;
    private static final int GRAY_PIXEL_BLUE_DIF_MAX = 40;

    private static LongScreenshotUtil sLongScreenshotUtil;
    private final int TOP_NOT_COMPARE_HEIGHT;
    private final int BORDER_NOT_COMPARE_WIDTH;
    private final int BOTTOM_NOT_COMPARE_EXTRA_HEIGHT;
    private final int MOVE_Y_MIN;
    private int BOTTOM_NOT_COMPARE_HEIGHT;
    private int mOldBitmapEndY = 0;
    private int mNewBitmapStartY = 0;

    private LongScreenshotUtil() {
        TOP_NOT_COMPARE_HEIGHT = DensityUtil.dp2px(88);//顶部不用对比的高度，即长截屏提示界面的高度。
        BOTTOM_NOT_COMPARE_EXTRA_HEIGHT = DensityUtil.dp2px(48);//底部不用对比的高度，在第一次对比的高度上再加上一些偏移
        BORDER_NOT_COMPARE_WIDTH = DensityUtil.dp2px(16);//每行像素对比时，两边不用对比的宽度，去除滚动条之类的干扰
        MOVE_Y_MIN = DensityUtil.dp2px(4);
    }

    public static LongScreenshotUtil getInstance() {
        if (sLongScreenshotUtil == null)
            sLongScreenshotUtil = new LongScreenshotUtil();
        return sLongScreenshotUtil;
    }
    private boolean mIsStop;
    public void stop(){
        mIsStop = true;
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
        final int newBitmapWidth = newBitmap.getWidth();
        final int oldBitmapHeight = oldBitmap.getHeight();
        final int newBitmapHeight = newBitmap.getHeight();

        int[] oldPixels = new int[oldBitmapWidth];
        int[] newPixels = new int[newBitmapWidth];

        for (int i = 0; i < (newBitmapHeight - TOP_NOT_COMPARE_HEIGHT); i++) {
            //将oldBitmap中第（oldBitmapHeight - 1 - i）行宽度为oldBitmapWidth的像素取出存入oldPixels数组中。
            oldBitmap.getPixels(oldPixels, 0, oldBitmapWidth, 0, (oldBitmapHeight - 1) - i, oldBitmapWidth, 1);
            newBitmap.getPixels(newPixels, 0, newBitmapWidth, 0, (newBitmapHeight - 1) - i, newBitmapWidth, 1);
            if (!isLinePixelsEqual(oldPixels, newPixels)) return i;
        }
        return -1;
    }

    private boolean becompareTwoBitmap(Bitmap oldBitmap, Bitmap newBitmap) {
        final int oldBitmapWidth = oldBitmap.getWidth();
        final int newBitmapWidth = newBitmap.getWidth();

        final int oldBitmapHeight = oldBitmap.getHeight();
        final int newBitmapHeight = newBitmap.getHeight();

        int[] oldPixels = new int[oldBitmapWidth];
        int[] newPixels = new int[newBitmapWidth];

        if (oldBitmapHeight == newBitmapHeight) {//first compare
            //bottomSameHeight compute once only
            int bottomSameHeight = getBottomSameHeight(oldBitmap, newBitmap);
            if (bottomSameHeight < 0) {
                Log.i(TAG, "FAILED_COMPARE two bitmap is same, to end");
                return false;
            }
            BOTTOM_NOT_COMPARE_HEIGHT = BOTTOM_NOT_COMPARE_EXTRA_HEIGHT + bottomSameHeight;//增加底部不用对比的高度
            Log.i(TAG, " first compare BOTTOM_NOT_COMPARE_HEIGHT = " + BOTTOM_NOT_COMPARE_HEIGHT);
        }

        int oldBitmapCompareEndY = getOldBitmapCompareEndY(oldBitmap, newBitmapHeight);
        if (oldBitmapCompareEndY < 0) {
            Log.i(TAG, "FAILED_COMPARE the old bitmap is pure error");
            return false;
        }
        int newBitmapCompareEndY = newBitmapHeight - (oldBitmapHeight - oldBitmapCompareEndY);//new bitmap开始比较的高度，确保与old bitmap比较的高度一致
        if (newBitmapCompareEndY < 0) {
            Log.i(TAG, "FAILED_COMPARE the new bitmap end compare height < 0");
            return false;
        }
        mIsStop = false;
        mOldBitmapEndY = oldBitmapCompareEndY;
        mNewBitmapStartY = 0;
        int sameNum = 0;
        int count = 0;//just for test
        while (newBitmapCompareEndY > (TOP_NOT_COMPARE_HEIGHT + BOTTOM_NOT_COMPARE_EXTRA_HEIGHT)) {
            if(mIsStop){
                Log.i(TAG, "FAILED_COMPARE user stop to compare...");
                return false;
            }
            count++;
            oldBitmap.getPixels(oldPixels, 0, oldBitmapWidth, 0, oldBitmapCompareEndY, oldBitmapWidth, 1);
            newBitmap.getPixels(newPixels, 0, newBitmapWidth, 0, newBitmapCompareEndY, newBitmapWidth, 1);
            boolean equal = isLinePixelsEqual(oldPixels, newPixels);
            if (equal) {
                if (sameNum == 0) {
                    mNewBitmapStartY = newBitmapCompareEndY;
                    Log.i(TAG, "same oldBitmapCompareEndY = " + oldBitmapCompareEndY
                            + " sameNum = " + sameNum
                            + " newBitmapCompareEndY = " + newBitmapCompareEndY);
                }
                sameNum++;
                oldBitmapCompareEndY--;
            } else {
                if (sameNum > 5) {
                    Log.i(TAG, "not same oldBitmapCompareEndY = " + oldBitmapCompareEndY
                            + " sameNum = " + sameNum
                            + " newBitmapCompareEndY = " + newBitmapCompareEndY
                            + " mNewBitmapStartY = " + mNewBitmapStartY);
                }
                oldBitmapCompareEndY = mOldBitmapEndY;
                sameNum = 0;
                if (mNewBitmapStartY != 0 && mNewBitmapStartY != newBitmapCompareEndY) {
                    newBitmapCompareEndY = mNewBitmapStartY;
                }
                mNewBitmapStartY = 0;
            }
            newBitmapCompareEndY--;
            if (sameNum >= BOTTOM_NOT_COMPARE_EXTRA_HEIGHT) {
                int oldMoveHeight = oldBitmapHeight - mOldBitmapEndY;
                int newMoveHeight = newBitmapHeight - mNewBitmapStartY;
                Log.i(TAG, "SUCCEED_COMPARE mOldBitmapEndY = " + mOldBitmapEndY
                        + " mNewBitmapStartY = " + mNewBitmapStartY
                        + " move height = " + Math.abs(newMoveHeight - oldMoveHeight)
                        + " sameNum = " + sameNum
                        + " count = " + count
                );
                return Math.abs(newMoveHeight - oldMoveHeight) > MOVE_Y_MIN;
            }
        }
        Log.i(TAG, "FAILED_COMPARE can not find " + BOTTOM_NOT_COMPARE_EXTRA_HEIGHT + "px equal height...");
        return false;
    }

    /**
     * 比较两个像素数组颜色值是否相同
     *
     * @param oldPixels 需要比较的像素数组
     * @param newPixels 被比较的像素数组
     * @return 是否相同
     */
    private boolean isLinePixelsEqual(int[] oldPixels, int[] newPixels) {
        if (oldPixels.length != newPixels.length)
            return false;
        int compareWidth = oldPixels.length - BORDER_NOT_COMPARE_WIDTH;
        int differentCount = 0;

        for (int i = BORDER_NOT_COMPARE_WIDTH; i < compareWidth; i++) {
            if (oldPixels[i] != newPixels[i]) {

                int oldRed = (oldPixels[i] & 0x00ff0000) >> 16;
                int oldGreen = (oldPixels[i] & 0x0000ff00) >> 8;
                int oldBlue = oldPixels[i] & 0x000000ff;

                int newRed = (newPixels[i] & 0x00ff0000) >> 16;
                int newGreen = (newPixels[i] & 0x0000ff00) >> 8;
                int newBlue = newPixels[i] & 0x000000ff;

                int redDifferent = Math.abs(oldRed - newRed);
                int greenDifferent = Math.abs(oldGreen - newGreen);
                int blueDifferent = Math.abs(oldBlue - newBlue);

                if (redDifferent > GRAY_PIXEL_RED_DIF_MAX || greenDifferent > GRAY_PIXEL_GREEN_DIF_MAX
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
     * 判断一个像素数组是否为纯色
     *
     * @param pixels 像素数组
     * @return 是否为纯色
     */
    private boolean isPureColorLine(int[] pixels) {
        int differentCount = 0;
        int compareWidth = pixels.length - BORDER_NOT_COMPARE_WIDTH;
        for (int i = BORDER_NOT_COMPARE_WIDTH; i < compareWidth; i++) {
            if (pixels[i] != pixels[i - 1]) {
                differentCount++;
            }
            if (differentCount > PURE_LINE_DIF_NUM_MAX) {
                return false;
            }
        }
        return true;
    }

    /**
     * 从下往上选取第一张bitmap不是纯色的高度值，用来作为对比的基线。
     *
     * @param bitmap
     * @return
     */
    private int getOldBitmapCompareEndY(Bitmap bitmap, int minHeight) {
        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();

        if (height < minHeight) return -1;

        final int end = height - minHeight;
        int[] pixels = new int[width];
        for (int i = (height - BOTTOM_NOT_COMPARE_HEIGHT - 1); i >= (end + TOP_NOT_COMPARE_HEIGHT + BOTTOM_NOT_COMPARE_EXTRA_HEIGHT); i--) {
            //将第i行宽度为width的像素点存入数组pixels中
            bitmap.getPixels(pixels, 0, width, 0, i, width, 1);
            if (!isPureColorLine(pixels)) return i;
        }
        return -1;
    }

    public Bitmap collageLongBitmap(Bitmap oldBitmap, Bitmap newBitmap) {
        if (oldBitmap == null || oldBitmap.isRecycled()
                || newBitmap == null || newBitmap.isRecycled())
            return null;
        long start = System.currentTimeMillis();
        boolean isSucceed = becompareTwoBitmap(oldBitmap, newBitmap);
        long end = System.currentTimeMillis();
        Log.i(TAG, "SUCCEED_COMPARE collageLongBitmap end isSucceed = " + isSucceed
                + ", cost = " + (end - start) + "ms");

        if (!isSucceed) {//failed, stop
            return null;
        }
        int width = oldBitmap.getWidth();
        int height = mOldBitmapEndY + (newBitmap.getHeight() - mNewBitmapStartY - 1);

        Log.i(TAG, "collageLongBitmap start height = " + height
                + " mOldBitmapEndY = " + mOldBitmapEndY
                + " mNewBitmapStartY = " + mNewBitmapStartY
                + " mScreenBitmap.getHeight() = " + newBitmap.getHeight());

        Bitmap resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(oldBitmap, new Matrix(), null);

        Bitmap bitmap = Bitmap.createBitmap(newBitmap, 0,
                mNewBitmapStartY + 1,
                newBitmap.getWidth(),
                newBitmap.getHeight() - 1 - mNewBitmapStartY);
        canvas.drawBitmap(bitmap, 0, mOldBitmapEndY + 1, null);
//        canvas.setBitmap(null);
        Log.i(TAG, "collageLongBitmap okokok");
        bitmap.recycle();
        newBitmap.recycle();
        oldBitmap.recycle();
        return resultBitmap;
    }

}
