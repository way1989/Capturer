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
    private static final int USE_TO_COMPARE_PIXEL_HEIGHT_500 = 500;
    private static final int USE_TO_COMPARE_PIXEL_HEIGHT_450 = 450;
    private static final int USE_TO_COMPARE_PIXEL_HEIGHT_400 = 400;
    private static final int USE_TO_COMPARE_PIXEL_HEIGHT_350 = 350;
    private static final int USE_TO_COMPARE_PIXEL_HEIGHT_300 = 300;
    private static final int USE_TO_COMPARE_PIXEL_HEIGHT_250 = 250;
    private static final int USE_TO_COMPARE_PIXEL_HEIGHT_200 = 200;
    private static final int USE_TO_COMPARE_PIXEL_HEIGHT_150 = 150;
    private static final int USE_TO_COMPARE_PIXEL_HEIGHT_100 = 100;
    private static final int USE_TO_COMPARE_PIXEL_HEIGHT_90 = 90;
    private static final int USE_TO_COMPARE_PIXEL_HEIGHT_80 = 80;
    private static final int USE_TO_COMPARE_PIXEL_HEIGHT_70 = 70;
    private static final int USE_TO_COMPARE_PIXEL_HEIGHT_60 = 60;
    private static final int USE_TO_COMPARE_PIXEL_HEIGHT_50 = 50;
    private static final int USE_TO_COMPARE_PIXEL_HEIGHT_40 = 40;
    private static final int USE_TO_COMPARE_PIXEL_HEIGHT_30 = 30;
    private static final int USE_TO_COMPARE_PIXEL_HEIGHT_20 = 20;
    private static final int USE_TO_COMPARE_PIXEL_HEIGHT_10 = 10;
    private static final int USE_TO_COMPARE_PIXEL_HEIGHT_5 = 5;
    private static final int REDUCE_PIXEL_BY_ONCE = 20;

    private static LongScreenshotUtil sLongScreenshotUtil;
    private final int TOP_NOT_COMPARE_HEIGHT;
    private final int BORDER_NOT_COMPARE_WIDTH;
    private final int BOTTOM_NOT_COMPARE_EXTRA_HEIGHT;
    private final int MOVE_Y_MIN;
    private int BOTTOM_NOT_COMPARE_HEIGHT;
    private int mOldBitmapEndY = 0;
    private int mNewBitmapStartY = 0;
    private boolean mIsStop;

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

    public void stop() {
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
        int moveY = 0;
        int behindBitmapCompareStartY_5 = 0;
        int behindBitmapCompareStartY_10 = 0;
        int behindBitmapCompareStartY_20 = 0;
        int behindBitmapCompareStartY_30 = 0;
        int behindBitmapCompareStartY_40 = 0;
        int behindBitmapCompareStartY_50 = 0;
        int behindBitmapCompareStartY_60 = 0;
        int behindBitmapCompareStartY_70 = 0;
        int behindBitmapCompareStartY_80 = 0;
        int behindBitmapCompareStartY_90 = 0;
        int behindBitmapCompareStartY_100 = 0;
        int behindBitmapCompareStartY_150 = 0;
        int behindBitmapCompareStartY_200 = 0;
        int behindBitmapCompareStartY_250 = 0;
        int behindBitmapCompareStartY_300 = 0;
        int behindBitmapCompareStartY_350 = 0;
        int behindBitmapCompareStartY_400 = 0;
        int behindBitmapCompareStartY_450 = 0;

        int oldBitmapStartCompareYDefault = oldBitmapCompareEndY;//临时保存一个默认值

        int oldBitmapEndCompareY = oldBitmapCompareEndY - USE_TO_COMPARE_PIXEL_HEIGHT_500;//比较500个像素
        int sameStartY = 0;
        int sameNum = 0;
        while (oldBitmapCompareEndY > oldBitmapEndCompareY) {
            if(mIsStop){
                Log.i(TAG, "FAILED_COMPARE user stop compare...");
                return false;
            }

            oldBitmap.getPixels(oldPixels, 0, oldBitmapWidth, 0, oldBitmapCompareEndY, oldBitmapWidth, 1);
            newBitmap.getPixels(newPixels, 0, newBitmapWidth, 0, newBitmapCompareEndY, newBitmapWidth, 1);

            boolean equal = isLinePixelsEqual(oldPixels, newPixels);
            if (equal) {
                sameNum++;
                if (sameNum == 1) {
                    sameStartY = newBitmapCompareEndY;
                    Log.i(TAG, "same oldBitmapCompareEndY = " + oldBitmapCompareEndY
                            + " sameNum = " + sameNum
                            + " newBitmapCompareEndY = " + newBitmapCompareEndY);
                }

                oldBitmapCompareEndY--;
            } else {
                if (sameNum != 0)
                    Log.i(TAG, "not same oldBitmapStartCompareY = " + oldBitmapCompareEndY
                            + " sameNum = " + sameNum
                            + " newBitmapCompareEndY = " + newBitmapCompareEndY
                            + " sameStartY = " + sameStartY);
                oldBitmapCompareEndY = oldBitmapStartCompareYDefault;
                sameNum = 0;
                if (sameStartY != 0 && sameStartY != newBitmapCompareEndY) {
                    newBitmapCompareEndY = sameStartY;
                }

                sameStartY = 0;
            }
            newBitmapCompareEndY--;

            /********* record 5 lines equal start ***************/
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_5
                    && behindBitmapCompareStartY_5 == 0) {
                behindBitmapCompareStartY_5 = sameStartY;
                int oldMoveHeight = oldBitmapHeight - mOldBitmapEndY;
                int newMoveHeight = newBitmapHeight - behindBitmapCompareStartY_5;

                moveY = Math.abs(newMoveHeight - oldMoveHeight);
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_10
                    && behindBitmapCompareStartY_10 == 0) {
                behindBitmapCompareStartY_10 = sameStartY;
                int oldMoveHeight = oldBitmapHeight - mOldBitmapEndY;
                int newMoveHeight = newBitmapHeight - behindBitmapCompareStartY_10;

                moveY = Math.abs(newMoveHeight - oldMoveHeight);
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_20
                    && behindBitmapCompareStartY_20 == 0) {
                behindBitmapCompareStartY_20 = sameStartY;
                int oldMoveHeight = oldBitmapHeight - mOldBitmapEndY;
                int newMoveHeight = newBitmapHeight - behindBitmapCompareStartY_20;

                moveY = Math.abs(newMoveHeight - oldMoveHeight);
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_30
                    && behindBitmapCompareStartY_30 == 0) {
                behindBitmapCompareStartY_30 = sameStartY;
                int oldMoveHeight = oldBitmapHeight - mOldBitmapEndY;
                int newMoveHeight = newBitmapHeight - behindBitmapCompareStartY_30;

                moveY = Math.abs(newMoveHeight - oldMoveHeight);
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_40
                    && behindBitmapCompareStartY_40 == 0) {
                behindBitmapCompareStartY_40 = sameStartY;
                int oldMoveHeight = oldBitmapHeight - mOldBitmapEndY;
                int newMoveHeight = newBitmapHeight - behindBitmapCompareStartY_40;

                moveY = Math.abs(newMoveHeight - oldMoveHeight);
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_50
                    && behindBitmapCompareStartY_50 == 0) {
                behindBitmapCompareStartY_50 = sameStartY;
                int oldMoveHeight = oldBitmapHeight - mOldBitmapEndY;
                int newMoveHeight = newBitmapHeight - behindBitmapCompareStartY_50;

                moveY = Math.abs(newMoveHeight - oldMoveHeight);
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_60
                    && behindBitmapCompareStartY_60 == 0) {
                behindBitmapCompareStartY_60 = sameStartY;
                int oldMoveHeight = oldBitmapHeight - mOldBitmapEndY;
                int newMoveHeight = newBitmapHeight - behindBitmapCompareStartY_60;

                moveY = Math.abs(newMoveHeight - oldMoveHeight);
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_70
                    && behindBitmapCompareStartY_70 == 0) {
                behindBitmapCompareStartY_70 = sameStartY;
                int oldMoveHeight = oldBitmapHeight - mOldBitmapEndY;
                int newMoveHeight = newBitmapHeight - behindBitmapCompareStartY_70;

                moveY = Math.abs(newMoveHeight - oldMoveHeight);
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_80
                    && behindBitmapCompareStartY_80 == 0) {
                behindBitmapCompareStartY_80 = sameStartY;
                int oldMoveHeight = oldBitmapHeight - mOldBitmapEndY;
                int newMoveHeight = newBitmapHeight - behindBitmapCompareStartY_80;

                moveY = Math.abs(newMoveHeight - oldMoveHeight);
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_90
                    && behindBitmapCompareStartY_90 == 0) {
                behindBitmapCompareStartY_90 = sameStartY;
                int oldMoveHeight = oldBitmapHeight - mOldBitmapEndY;
                int newMoveHeight = newBitmapHeight - behindBitmapCompareStartY_90;

                moveY = Math.abs(newMoveHeight - oldMoveHeight);
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_100
                    && behindBitmapCompareStartY_100 == 0) {
                behindBitmapCompareStartY_100 = sameStartY;
                int oldMoveHeight = oldBitmapHeight - mOldBitmapEndY;
                int newMoveHeight = newBitmapHeight - behindBitmapCompareStartY_100;

                moveY = Math.abs(newMoveHeight - oldMoveHeight);
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_150
                    && behindBitmapCompareStartY_150 == 0) {
                behindBitmapCompareStartY_150 = sameStartY;
                int oldMoveHeight = oldBitmapHeight - mOldBitmapEndY;
                int newMoveHeight = newBitmapHeight - behindBitmapCompareStartY_150;

                moveY = Math.abs(newMoveHeight - oldMoveHeight);
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_200
                    && behindBitmapCompareStartY_200 == 0) {
                behindBitmapCompareStartY_200 = sameStartY;
                int oldMoveHeight = oldBitmapHeight - mOldBitmapEndY;
                int newMoveHeight = newBitmapHeight - behindBitmapCompareStartY_200;

                moveY = Math.abs(newMoveHeight - oldMoveHeight);
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_250
                    && behindBitmapCompareStartY_250 == 0) {
                behindBitmapCompareStartY_250 = sameStartY;
                int oldMoveHeight = oldBitmapHeight - mOldBitmapEndY;
                int newMoveHeight = newBitmapHeight - behindBitmapCompareStartY_250;

                moveY = Math.abs(newMoveHeight - oldMoveHeight);
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_300
                    && behindBitmapCompareStartY_300 == 0) {
                behindBitmapCompareStartY_300 = sameStartY;
                int oldMoveHeight = oldBitmapHeight - mOldBitmapEndY;
                int newMoveHeight = newBitmapHeight - behindBitmapCompareStartY_300;

                moveY = Math.abs(newMoveHeight - oldMoveHeight);
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_350
                    && behindBitmapCompareStartY_350 == 0) {
                behindBitmapCompareStartY_350 = sameStartY;
                int oldMoveHeight = oldBitmapHeight - mOldBitmapEndY;
                int newMoveHeight = newBitmapHeight - behindBitmapCompareStartY_350;

                moveY = Math.abs(newMoveHeight - oldMoveHeight);
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_400
                    && behindBitmapCompareStartY_400 == 0) {
                behindBitmapCompareStartY_400 = sameStartY;
                int oldMoveHeight = oldBitmapHeight - mOldBitmapEndY;
                int newMoveHeight = newBitmapHeight - behindBitmapCompareStartY_400;

                moveY = Math.abs(newMoveHeight - oldMoveHeight);
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_450
                    && behindBitmapCompareStartY_450 == 0) {
                behindBitmapCompareStartY_450 = sameStartY;
                int oldMoveHeight = oldBitmapHeight - mOldBitmapEndY;
                int newMoveHeight = newBitmapHeight - behindBitmapCompareStartY_450;

                moveY = Math.abs(newMoveHeight - oldMoveHeight);
            }
            /********* record 5 lines equal end ***************/

            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_500) {
                mNewBitmapStartY = sameStartY;
                int oldMoveHeight = oldBitmapHeight - mOldBitmapEndY;
                int newMoveHeight = newBitmapHeight - mNewBitmapStartY;

                moveY = Math.abs(newMoveHeight - oldMoveHeight);
                return moveY > MOVE_Y_MIN;
            }

            if (newBitmapCompareEndY < TOP_NOT_COMPARE_HEIGHT) {
                /********* look if have 100 lines equal start ***************/
                if (behindBitmapCompareStartY_450 != 0) {
                    mNewBitmapStartY = behindBitmapCompareStartY_450;

                    Log.i(TAG, "000 okokok 450 compare succeed moveY = " + moveY
                            + " behindBitmapCompareStartY_450 = " + behindBitmapCompareStartY_450
                            + " mOldBitmapEndY = " + mOldBitmapEndY);

                    return moveY > MOVE_Y_MIN;
                }
                if (behindBitmapCompareStartY_400 != 0) {
                    mNewBitmapStartY = behindBitmapCompareStartY_400;

                    Log.i(TAG, "000 okokok 400 compare succeed moveY = " + moveY
                            + " behindBitmapCompareStartY_400 = " + behindBitmapCompareStartY_400
                            + " mOldBitmapEndY = " + mOldBitmapEndY);

                    return moveY > MOVE_Y_MIN;
                }
                if (behindBitmapCompareStartY_350 != 0) {
                    mNewBitmapStartY = behindBitmapCompareStartY_350;

                    Log.i(TAG, "000 okokok 350 compare succeed moveY = " + moveY
                            + " behindBitmapCompareStartY_350 = " + behindBitmapCompareStartY_350
                            + " mOldBitmapEndY = " + mOldBitmapEndY);

                    return moveY > MOVE_Y_MIN;
                }
                if (behindBitmapCompareStartY_300 != 0) {
                    mNewBitmapStartY = behindBitmapCompareStartY_300;

                    Log.i(TAG, "000 okokok 300 compare succeed moveY = " + moveY
                            + " behindBitmapCompareStartY_300 = " + behindBitmapCompareStartY_300
                            + " mOldBitmapEndY = " + mOldBitmapEndY);

                    return moveY > MOVE_Y_MIN;
                }
                if (behindBitmapCompareStartY_250 != 0) {
                    mNewBitmapStartY = behindBitmapCompareStartY_250;

                    Log.i(TAG, "000 okokok 250 compare succeed moveY = " + moveY
                            + " behindBitmapCompareStartY_250 = " + behindBitmapCompareStartY_250
                            + " mOldBitmapEndY = " + mOldBitmapEndY);

                    return moveY > MOVE_Y_MIN;
                }
                if (behindBitmapCompareStartY_200 != 0) {
                    mNewBitmapStartY = behindBitmapCompareStartY_200;

                    Log.i(TAG, "000 okokok 200 compare succeed moveY = " + moveY
                            + " behindBitmapCompareStartY_200 = " + behindBitmapCompareStartY_200
                            + " mOldBitmapEndY = " + mOldBitmapEndY);

                    return moveY > MOVE_Y_MIN;
                }
                if (behindBitmapCompareStartY_150 != 0) {
                    mNewBitmapStartY = behindBitmapCompareStartY_150;

                    Log.i(TAG, "000 okokok 150 compare succeed moveY = " + moveY
                            + " behindBitmapCompareStartY_150 = " + behindBitmapCompareStartY_150
                            + " mOldBitmapEndY = " + mOldBitmapEndY);

                    return moveY > MOVE_Y_MIN;
                }
                if (behindBitmapCompareStartY_100 != 0) {
                    mNewBitmapStartY = behindBitmapCompareStartY_100;

                    Log.i(TAG, "000 okokok 100 compare succeed moveY = " + moveY
                            + " behindBitmapCompareStartY_100 = " + behindBitmapCompareStartY_100
                            + " mOldBitmapEndY = " + mOldBitmapEndY);

                    return moveY > MOVE_Y_MIN;
                }
                /********* look if have 100 lines equal end ***************/

                /****************equal height smaller than 100 start ***************************/
                if (oldBitmapStartCompareYDefault - oldBitmapEndCompareY >= REDUCE_PIXEL_BY_ONCE) {
                    oldBitmapStartCompareYDefault -= REDUCE_PIXEL_BY_ONCE;//compare again
                    oldBitmapCompareEndY = oldBitmapStartCompareYDefault;
                    mOldBitmapEndY = oldBitmapCompareEndY;
                    newBitmapCompareEndY = newBitmapHeight - (oldBitmapHeight - oldBitmapCompareEndY);
                    sameStartY = 0;
                    sameNum = 0;
                    Log.i(TAG, "REDUCE_PIXEL_BY_ONCE oldBitmapStartCompareY = " + oldBitmapCompareEndY
                            + " newBitmapCompareEndY = " + newBitmapCompareEndY
                            + " oldBitmapEndCompareY = " + oldBitmapEndCompareY);
                    continue;
                }
                /****************equal height smaller than 100 end*******************************/

                Log.i(TAG, "error error error oldBitmapCompareEndY = " + oldBitmapCompareEndY +
                        " sameNum = " + sameNum + " newBitmapCompareEndY = " + newBitmapCompareEndY
                        + " sameStartY = " + sameStartY
                        + " mNewBitmapStartY = " + mNewBitmapStartY);

                return false;
            }
        }

        /********* look if have 5 lines equal start ***************/
        if (behindBitmapCompareStartY_90 != 0) {
            mNewBitmapStartY = behindBitmapCompareStartY_90;

            Log.i(TAG, "111 okokok 90 compare succeed moveY = " + moveY
                    + " behindBitmapCompareStartY_90 = " + behindBitmapCompareStartY_90
                    + " mOldBitmapEndY = " + mOldBitmapEndY);

            return moveY > MOVE_Y_MIN;
        }
        if (behindBitmapCompareStartY_80 != 0) {
            mNewBitmapStartY = behindBitmapCompareStartY_80;

            Log.i(TAG, "111 okokok 80 compare succeed moveY = " + moveY
                    + " behindBitmapCompareStartY_80 = " + behindBitmapCompareStartY_80
                    + " mOldBitmapEndY = " + mOldBitmapEndY);

            return moveY > MOVE_Y_MIN;
        }
        if (behindBitmapCompareStartY_70 != 0) {
            mNewBitmapStartY = behindBitmapCompareStartY_70;

            Log.i(TAG, "111 okokok 70 compare succeed moveY = " + moveY
                    + " behindBitmapCompareStartY_70 = " + behindBitmapCompareStartY_70
                    + " mOldBitmapEndY = " + mOldBitmapEndY);

            return moveY > MOVE_Y_MIN;
        }
        if (behindBitmapCompareStartY_60 != 0) {
            mNewBitmapStartY = behindBitmapCompareStartY_60;

            Log.i(TAG, "111 okokok 60 compare succeed moveY = " + moveY
                    + " behindBitmapCompareStartY_60 = " + behindBitmapCompareStartY_60
                    + " mOldBitmapEndY = " + mOldBitmapEndY);

            return moveY > MOVE_Y_MIN;
        }
        if (behindBitmapCompareStartY_50 != 0) {
            mNewBitmapStartY = behindBitmapCompareStartY_50;

            Log.i(TAG, "111 okokok 50 compare succeed moveY = " + moveY
                    + " behindBitmapCompareStartY_50 = " + behindBitmapCompareStartY_50
                    + " mOldBitmapEndY = " + mOldBitmapEndY);

            return moveY > MOVE_Y_MIN;
        }
        if (behindBitmapCompareStartY_40 != 0) {
            mNewBitmapStartY = behindBitmapCompareStartY_40;

            Log.i(TAG, "111 okokok 40 compare succeed moveY = " + moveY
                    + " behindBitmapCompareStartY_40 = " + behindBitmapCompareStartY_40
                    + " mOldBitmapEndY = " + mOldBitmapEndY);

            return moveY > MOVE_Y_MIN;
        }
        if (behindBitmapCompareStartY_30 != 0) {
            mNewBitmapStartY = behindBitmapCompareStartY_30;

            Log.i(TAG, "111 okokok 30 compare succeed moveY = " + moveY
                    + " behindBitmapCompareStartY_30 = " + behindBitmapCompareStartY_30
                    + " mOldBitmapEndY = " + mOldBitmapEndY);

            return moveY > MOVE_Y_MIN;
        }
        if (behindBitmapCompareStartY_20 != 0) {
            mNewBitmapStartY = behindBitmapCompareStartY_20;

            Log.i(TAG, "111 okokok 20 compare succeed moveY = " + moveY
                    + " behindBitmapCompareStartY_20 = " + behindBitmapCompareStartY_20
                    + " mOldBitmapEndY = " + mOldBitmapEndY);

            return moveY > MOVE_Y_MIN;
        }
        if (behindBitmapCompareStartY_10 != 0) {
            mNewBitmapStartY = behindBitmapCompareStartY_10;

            Log.i(TAG, "111 okokok 10 compare succeed moveY = " + moveY
                    + " behindBitmapCompareStartY_10 = " + behindBitmapCompareStartY_10
                    + " mOldBitmapEndY = " + mOldBitmapEndY);

            return moveY > MOVE_Y_MIN;
        }
        if (behindBitmapCompareStartY_5 != 0) {
            mNewBitmapStartY = behindBitmapCompareStartY_5;

            Log.i(TAG, "111 okokok 5 compare succeed moveY = " + moveY
                    + " behindBitmapCompareStartY_5 = " + behindBitmapCompareStartY_5
                    + " mOldBitmapEndY = " + mOldBitmapEndY);

            return moveY > MOVE_Y_MIN;
        }
        /********* look if have 5 lines equal end ***************/
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
        for (int i = (height - BOTTOM_NOT_COMPARE_HEIGHT - 1); i >= (end + TOP_NOT_COMPARE_HEIGHT); i--) {
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
        if(height < oldBitmap.getHeight()) return null;

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
