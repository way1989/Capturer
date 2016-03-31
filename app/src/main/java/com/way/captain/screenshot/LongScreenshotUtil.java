package com.way.captain.screenshot;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.Log;

import com.way.captain.utils.DensityUtil;


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
    private static final int MOVE_Y_MIN = 10;
    private static final int SUCCEED_COMPARE = 0;
    private static final int ERROR_TWO_BITMAP_IS_SAME = -1;
    private static final int ERROR_COMPARE_FAILED = -2;
    private static final int ERROR_FRONT_BITMAP_PURE_COLOR = -3;
    private static final int ERROR_UNKNOW_STATE = -5;
    private static LongScreenshotUtil sLongScreenshotUtil;
    private final int mTopNotCompareHeight;
    private final int mBorderNotCompareWidth;
    private final int mBottomNotCompareHeightMargin;
    private int mBottomNotCompareHeight;
    private int mOldBitmapEndY = 0;
    private int mNewBitmapStartY = 0;

    private LongScreenshotUtil(Context context) {
        mTopNotCompareHeight = DensityUtil.dip2px(context, 104);//顶部不用对比的高度，即长截屏提示界面的高度。
        mBottomNotCompareHeightMargin = DensityUtil.dip2px(context, 48);//底部不用对比的高度，在第一次对比的高度上再加上一些偏移
        mBorderNotCompareWidth = DensityUtil.dip2px(context, 16);//每行像素对比时，两边不用对比的宽度，去除滚动条之类的干扰
    }

    public static LongScreenshotUtil getInstance(Context context) {
        if (sLongScreenshotUtil == null)
            sLongScreenshotUtil = new LongScreenshotUtil(context);
        return sLongScreenshotUtil;
    }

    /**
     * 从下往上比较两张图的每一行像素，返回像素开始不同时的行数值，如果两张图片完全相同返回-1
     * 主要用来第一次对比时判断两张图是不是相同，如果不同时，获取两张图底部相同部分的高度，比如虚拟按键的高度或者底部tab的高度
     *
     * @param oldBitmap 需要比较的图
     * @param newBitmap 被比较的图
     * @return 像素开始不同时的行数值
     */
    private int getFromBottomSameY(Bitmap oldBitmap, Bitmap newBitmap) {
        final int oldBitmapWidth = oldBitmap.getWidth();
        final int newBitmapWidth = newBitmap.getWidth();
        final int oldBitmapHeight = oldBitmap.getHeight();
        final int newBitmapHeight = newBitmap.getHeight();

        int[] oldPixels = new int[oldBitmapWidth];
        int[] newPixels = new int[newBitmapWidth];

        for (int i = 0; i < (newBitmapHeight - mTopNotCompareHeight); i++) {
            //将oldBitmap中第（oldBitmapHeight - 1 - i）行宽度为oldBitmapWidth的像素取出存入oldPixels数组中。
            oldBitmap.getPixels(oldPixels, 0, oldBitmapWidth, 0, (oldBitmapHeight - 1) - i, oldBitmapWidth, 1);
            newBitmap.getPixels(newPixels, 0, newBitmapWidth, 0, (newBitmapHeight - 1) - i, newBitmapWidth, 1);
            if (!isLinePixelsEqual(oldPixels, newPixels)) return i;
        }
        return -1;
    }

    private int becompareTwoBitmap(Bitmap oldBitmap, Bitmap newBitmap) {
        final int oldBitmapWidth = oldBitmap.getWidth();
        final int newBitmapWidth = newBitmap.getWidth();
        final int oldBitmapHeight = oldBitmap.getHeight();
        final int newBitmapHeight = newBitmap.getHeight();

        final int oldBottomY = oldBitmapHeight - 1;
        final int newBottomY = newBitmapHeight - 1;

        int[] oldPixels = new int[oldBitmapWidth];
        int[] newPixels = new int[newBitmapWidth];

        if (oldBitmapHeight == newBitmapHeight) {//first compare
            //sameY compute once only
            int sameY = getFromBottomSameY(oldBitmap, newBitmap);
            Log.i(TAG, " first compare sameY = " + sameY);
            //if (sameY > newBitmapHeight - mTopNotCompareHeight) {//two bitmap is same
            if (sameY == -1) {
                Log.i(TAG, "two bitmap is same, to end");
                return ERROR_TWO_BITMAP_IS_SAME;
            }
            mBottomNotCompareHeight = mBottomNotCompareHeightMargin + sameY;//增加底部不用对比的高度
            Log.i(TAG, " first compare mBottomNotCompareHeight = " + mBottomNotCompareHeight);
        }

        int oldBitmapStartCompareY = getOldBitmapStartCompareY(oldBitmap);
        if (oldBitmapStartCompareY == -1) {
            Log.i(TAG, "the front bitmap is pure error");
            return ERROR_FRONT_BITMAP_PURE_COLOR;
        }
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

        int oldBitmapStartCompareYDefault = oldBitmapStartCompareY;//临时保存一个默认值
        mOldBitmapEndY = oldBitmapStartCompareY;
        int newBitmapStartCompareY = newBottomY - (oldBottomY - oldBitmapStartCompareY);//new bitmap开始比较的高度，确保与old bitmap比较的高度一致
        Log.i(TAG, "mBottomNotCompareHeight = " + mBottomNotCompareHeight
                + ", oldBitmapStartCompareY = " + oldBitmapStartCompareY
                + ", oldBitmapHeight = " + oldBitmapHeight
                + ", newBitmapStartCompareY = " + newBitmapStartCompareY
                + ", newBitmapHeight = " + newBitmapHeight
                + ", (oldBottomY - oldBitmapStartCompareY) = " + (oldBottomY - oldBitmapStartCompareY));

        int oldBitmapEndCompareY = oldBitmapStartCompareY - USE_TO_COMPARE_PIXEL_HEIGHT_500;//比较500个像素
        int sameStartY = 0;
        int sameNum = 0;
        while (oldBitmapStartCompareY > oldBitmapEndCompareY) {

            oldBitmap.getPixels(oldPixels, 0, oldBitmapWidth, 0, oldBitmapStartCompareY, oldBitmapWidth, 1);
            newBitmap.getPixels(newPixels, 0, newBitmapWidth, 0, newBitmapStartCompareY, newBitmapWidth, 1);

            boolean equal = isLinePixelsEqual(oldPixels, newPixels);
            if (equal) {
                sameNum++;
                if (sameNum == 1) {
                    sameStartY = newBitmapStartCompareY;
                    Log.i(TAG, "same oldBitmapStartCompareY = " + oldBitmapStartCompareY
                            + " sameNum = " + sameNum
                            + " newBitmapStartCompareY = " + newBitmapStartCompareY);
                }

                oldBitmapStartCompareY--;
            } else {
                if (sameNum != 0)
                    Log.i(TAG, "not same oldBitmapStartCompareY = " + oldBitmapStartCompareY
                            + " sameNum = " + sameNum
                            + " newBitmapStartCompareY = " + newBitmapStartCompareY
                            + " sameStartY = " + sameStartY);
                oldBitmapStartCompareY = oldBitmapStartCompareYDefault;
                sameNum = 0;
                if (sameStartY != 0 && sameStartY != newBitmapStartCompareY) {
                    newBitmapStartCompareY = sameStartY;
                }

                sameStartY = 0;
            }

            /********* record 5 lines equal start ***************/
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_5
                    && behindBitmapCompareStartY_5 == 0) {
                behindBitmapCompareStartY_5 = sameStartY;
                int front_move_height_to_compare = oldBottomY - mOldBitmapEndY;
                int behind_move_height_to_compare = newBottomY - behindBitmapCompareStartY_5;

                moveY = behind_move_height_to_compare - front_move_height_to_compare;
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_10
                    && behindBitmapCompareStartY_10 == 0) {
                behindBitmapCompareStartY_10 = sameStartY;
                int front_move_height_to_compare = oldBottomY - mOldBitmapEndY;
                int behind_move_height_to_compare = newBottomY - behindBitmapCompareStartY_10;

                moveY = behind_move_height_to_compare - front_move_height_to_compare;
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_20
                    && behindBitmapCompareStartY_20 == 0) {
                behindBitmapCompareStartY_20 = sameStartY;
                int front_move_height_to_compare = oldBottomY - mOldBitmapEndY;
                int behind_move_height_to_compare = newBottomY - behindBitmapCompareStartY_20;

                moveY = behind_move_height_to_compare - front_move_height_to_compare;
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_30
                    && behindBitmapCompareStartY_30 == 0) {
                behindBitmapCompareStartY_30 = sameStartY;
                int front_move_height_to_compare = oldBottomY - mOldBitmapEndY;
                int behind_move_height_to_compare = newBottomY - behindBitmapCompareStartY_30;

                moveY = behind_move_height_to_compare - front_move_height_to_compare;
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_40
                    && behindBitmapCompareStartY_40 == 0) {
                behindBitmapCompareStartY_40 = sameStartY;
                int front_move_height_to_compare = oldBottomY - mOldBitmapEndY;
                int behind_move_height_to_compare = newBottomY - behindBitmapCompareStartY_40;

                moveY = behind_move_height_to_compare - front_move_height_to_compare;
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_50
                    && behindBitmapCompareStartY_50 == 0) {
                behindBitmapCompareStartY_50 = sameStartY;
                int front_move_height_to_compare = oldBottomY - mOldBitmapEndY;
                int behind_move_height_to_compare = newBottomY - behindBitmapCompareStartY_50;

                moveY = behind_move_height_to_compare - front_move_height_to_compare;
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_60
                    && behindBitmapCompareStartY_60 == 0) {
                behindBitmapCompareStartY_60 = sameStartY;
                int front_move_height_to_compare = oldBottomY - mOldBitmapEndY;
                int behind_move_height_to_compare = newBottomY - behindBitmapCompareStartY_60;

                moveY = behind_move_height_to_compare - front_move_height_to_compare;
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_70
                    && behindBitmapCompareStartY_70 == 0) {
                behindBitmapCompareStartY_70 = sameStartY;
                int front_move_height_to_compare = oldBottomY - mOldBitmapEndY;
                int behind_move_height_to_compare = newBottomY - behindBitmapCompareStartY_70;

                moveY = behind_move_height_to_compare - front_move_height_to_compare;
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_80
                    && behindBitmapCompareStartY_80 == 0) {
                behindBitmapCompareStartY_80 = sameStartY;
                int front_move_height_to_compare = oldBottomY - mOldBitmapEndY;
                int behind_move_height_to_compare = newBottomY - behindBitmapCompareStartY_80;

                moveY = behind_move_height_to_compare - front_move_height_to_compare;
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_90
                    && behindBitmapCompareStartY_90 == 0) {
                behindBitmapCompareStartY_90 = sameStartY;
                int front_move_height_to_compare = oldBottomY - mOldBitmapEndY;
                int behind_move_height_to_compare = newBottomY - behindBitmapCompareStartY_90;

                moveY = behind_move_height_to_compare - front_move_height_to_compare;
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_100
                    && behindBitmapCompareStartY_100 == 0) {
                behindBitmapCompareStartY_100 = sameStartY;
                int front_move_height_to_compare = oldBottomY - mOldBitmapEndY;
                int behind_move_height_to_compare = newBottomY - behindBitmapCompareStartY_100;

                moveY = behind_move_height_to_compare - front_move_height_to_compare;
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_150
                    && behindBitmapCompareStartY_150 == 0) {
                behindBitmapCompareStartY_150 = sameStartY;
                int front_move_height_to_compare = oldBottomY - mOldBitmapEndY;
                int behind_move_height_to_compare = newBottomY - behindBitmapCompareStartY_150;

                moveY = behind_move_height_to_compare - front_move_height_to_compare;
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_200
                    && behindBitmapCompareStartY_200 == 0) {
                behindBitmapCompareStartY_200 = sameStartY;
                int front_move_height_to_compare = oldBottomY - mOldBitmapEndY;
                int behind_move_height_to_compare = newBottomY - behindBitmapCompareStartY_200;

                moveY = behind_move_height_to_compare - front_move_height_to_compare;
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_250
                    && behindBitmapCompareStartY_250 == 0) {
                behindBitmapCompareStartY_250 = sameStartY;
                int front_move_height_to_compare = oldBottomY - mOldBitmapEndY;
                int behind_move_height_to_compare = newBottomY - behindBitmapCompareStartY_250;

                moveY = behind_move_height_to_compare - front_move_height_to_compare;
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_300
                    && behindBitmapCompareStartY_300 == 0) {
                behindBitmapCompareStartY_300 = sameStartY;
                int front_move_height_to_compare = oldBottomY - mOldBitmapEndY;
                int behind_move_height_to_compare = newBottomY - behindBitmapCompareStartY_300;

                moveY = behind_move_height_to_compare - front_move_height_to_compare;
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_350
                    && behindBitmapCompareStartY_350 == 0) {
                behindBitmapCompareStartY_350 = sameStartY;
                int front_move_height_to_compare = oldBottomY - mOldBitmapEndY;
                int behind_move_height_to_compare = newBottomY - behindBitmapCompareStartY_350;

                moveY = behind_move_height_to_compare - front_move_height_to_compare;
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_400
                    && behindBitmapCompareStartY_400 == 0) {
                behindBitmapCompareStartY_400 = sameStartY;
                int front_move_height_to_compare = oldBottomY - mOldBitmapEndY;
                int behind_move_height_to_compare = newBottomY - behindBitmapCompareStartY_400;

                moveY = behind_move_height_to_compare - front_move_height_to_compare;
            }
            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_450
                    && behindBitmapCompareStartY_450 == 0) {
                behindBitmapCompareStartY_450 = sameStartY;
                int front_move_height_to_compare = oldBottomY - mOldBitmapEndY;
                int behind_move_height_to_compare = newBottomY - behindBitmapCompareStartY_450;

                moveY = behind_move_height_to_compare - front_move_height_to_compare;
            }
            /********* record 5 lines equal end ***************/

            if (sameNum == USE_TO_COMPARE_PIXEL_HEIGHT_500) {
                mNewBitmapStartY = sameStartY;
                int front_move_height_to_compare = oldBottomY - mOldBitmapEndY;
                int behind_move_height_to_compare = newBottomY - mNewBitmapStartY;

                moveY = behind_move_height_to_compare - front_move_height_to_compare;

                if (moveY < MOVE_Y_MIN) {
                    return ERROR_TWO_BITMAP_IS_SAME;
                }
                return SUCCEED_COMPARE;
            }

            newBitmapStartCompareY--;

            if (newBitmapStartCompareY < mTopNotCompareHeight) {
                /********* look if have 100 lines equal start ***************/
                if (behindBitmapCompareStartY_450 != 0) {
                    mNewBitmapStartY = behindBitmapCompareStartY_450;

                    Log.i(TAG, "000 okokok 450 compare succeed moveY = " + moveY
                            + " behindBitmapCompareStartY_450 = " + behindBitmapCompareStartY_450
                            + " mOldBitmapEndY = " + mOldBitmapEndY);

                    if (moveY < MOVE_Y_MIN) {
                        return ERROR_TWO_BITMAP_IS_SAME;
                    }
                    return SUCCEED_COMPARE;
                }
                if (behindBitmapCompareStartY_400 != 0) {
                    mNewBitmapStartY = behindBitmapCompareStartY_400;

                    Log.i(TAG, "000 okokok 400 compare succeed moveY = " + moveY
                            + " behindBitmapCompareStartY_400 = " + behindBitmapCompareStartY_400
                            + " mOldBitmapEndY = " + mOldBitmapEndY);

                    if (moveY < MOVE_Y_MIN) {
                        return ERROR_TWO_BITMAP_IS_SAME;
                    }
                    return SUCCEED_COMPARE;
                }
                if (behindBitmapCompareStartY_350 != 0) {
                    mNewBitmapStartY = behindBitmapCompareStartY_350;

                    Log.i(TAG, "000 okokok 350 compare succeed moveY = " + moveY
                            + " behindBitmapCompareStartY_350 = " + behindBitmapCompareStartY_350
                            + " mOldBitmapEndY = " + mOldBitmapEndY);

                    if (moveY < MOVE_Y_MIN) {
                        return ERROR_TWO_BITMAP_IS_SAME;
                    }
                    return SUCCEED_COMPARE;
                }
                if (behindBitmapCompareStartY_300 != 0) {
                    mNewBitmapStartY = behindBitmapCompareStartY_300;

                    Log.i(TAG, "000 okokok 300 compare succeed moveY = " + moveY
                            + " behindBitmapCompareStartY_300 = " + behindBitmapCompareStartY_300
                            + " mOldBitmapEndY = " + mOldBitmapEndY);

                    if (moveY < MOVE_Y_MIN) {
                        return ERROR_TWO_BITMAP_IS_SAME;
                    }
                    return SUCCEED_COMPARE;
                }
                if (behindBitmapCompareStartY_250 != 0) {
                    mNewBitmapStartY = behindBitmapCompareStartY_250;

                    Log.i(TAG, "000 okokok 250 compare succeed moveY = " + moveY
                            + " behindBitmapCompareStartY_250 = " + behindBitmapCompareStartY_250
                            + " mOldBitmapEndY = " + mOldBitmapEndY);

                    if (moveY < MOVE_Y_MIN) {
                        return ERROR_TWO_BITMAP_IS_SAME;
                    }
                    return SUCCEED_COMPARE;
                }
                if (behindBitmapCompareStartY_200 != 0) {
                    mNewBitmapStartY = behindBitmapCompareStartY_200;

                    Log.i(TAG, "000 okokok 200 compare succeed moveY = " + moveY
                            + " behindBitmapCompareStartY_200 = " + behindBitmapCompareStartY_200
                            + " mOldBitmapEndY = " + mOldBitmapEndY);

                    if (moveY < MOVE_Y_MIN) {
                        return ERROR_TWO_BITMAP_IS_SAME;
                    }
                    return SUCCEED_COMPARE;
                }
                if (behindBitmapCompareStartY_150 != 0) {
                    mNewBitmapStartY = behindBitmapCompareStartY_150;

                    Log.i(TAG, "000 okokok 150 compare succeed moveY = " + moveY
                            + " behindBitmapCompareStartY_150 = " + behindBitmapCompareStartY_150
                            + " mOldBitmapEndY = " + mOldBitmapEndY);

                    if (moveY < MOVE_Y_MIN) {
                        return ERROR_TWO_BITMAP_IS_SAME;
                    }
                    return SUCCEED_COMPARE;
                }
                if (behindBitmapCompareStartY_100 != 0) {
                    mNewBitmapStartY = behindBitmapCompareStartY_100;

                    Log.i(TAG, "000 okokok 100 compare succeed moveY = " + moveY
                            + " behindBitmapCompareStartY_100 = " + behindBitmapCompareStartY_100
                            + " mOldBitmapEndY = " + mOldBitmapEndY);

                    if (moveY < MOVE_Y_MIN) {
                        return ERROR_TWO_BITMAP_IS_SAME;
                    }
                    return SUCCEED_COMPARE;
                }
                /********* look if have 100 lines equal end ***************/

                /****************equal height smaller than 100 start ***************************/
                if (oldBitmapStartCompareYDefault - oldBitmapEndCompareY >= REDUCE_PIXEL_BY_ONCE) {
                    oldBitmapStartCompareYDefault -= REDUCE_PIXEL_BY_ONCE;//compare again
                    oldBitmapStartCompareY = oldBitmapStartCompareYDefault;
                    mOldBitmapEndY = oldBitmapStartCompareY;
                    newBitmapStartCompareY = newBottomY - (oldBottomY - oldBitmapStartCompareY);
                    sameStartY = 0;
                    sameNum = 0;
                    Log.i(TAG, "REDUCE_PIXEL_BY_ONCE oldBitmapStartCompareY = " + oldBitmapStartCompareY
                            + " newBitmapStartCompareY = " + newBitmapStartCompareY
                            + " oldBitmapEndCompareY = " + oldBitmapEndCompareY);
                    continue;
                }
                /****************equal height smaller than 100 end*******************************/

                Log.i(TAG, "error error error oldBitmapStartCompareY = " + oldBitmapStartCompareY +
                        " sameNum = " + sameNum + " newBitmapStartCompareY = " + newBitmapStartCompareY
                        + " sameStartY = " + sameStartY
                        + " mNewBitmapStartY = " + mNewBitmapStartY);

                return ERROR_COMPARE_FAILED;
            }
        }

        /********* look if have 5 lines equal start ***************/
        if (behindBitmapCompareStartY_90 != 0) {
            mNewBitmapStartY = behindBitmapCompareStartY_90;

            Log.i(TAG, "111 okokok 90 compare succeed moveY = " + moveY
                    + " behindBitmapCompareStartY_90 = " + behindBitmapCompareStartY_90
                    + " mOldBitmapEndY = " + mOldBitmapEndY);

            if (moveY < MOVE_Y_MIN) {
                return ERROR_TWO_BITMAP_IS_SAME;
            }
            return SUCCEED_COMPARE;
        }
        if (behindBitmapCompareStartY_80 != 0) {
            mNewBitmapStartY = behindBitmapCompareStartY_80;

            Log.i(TAG, "111 okokok 80 compare succeed moveY = " + moveY
                    + " behindBitmapCompareStartY_80 = " + behindBitmapCompareStartY_80
                    + " mOldBitmapEndY = " + mOldBitmapEndY);

            if (moveY < MOVE_Y_MIN) {
                return ERROR_TWO_BITMAP_IS_SAME;
            }
            return SUCCEED_COMPARE;
        }
        if (behindBitmapCompareStartY_70 != 0) {
            mNewBitmapStartY = behindBitmapCompareStartY_70;

            Log.i(TAG, "111 okokok 70 compare succeed moveY = " + moveY
                    + " behindBitmapCompareStartY_70 = " + behindBitmapCompareStartY_70
                    + " mOldBitmapEndY = " + mOldBitmapEndY);

            if (moveY < MOVE_Y_MIN) {
                return ERROR_TWO_BITMAP_IS_SAME;
            }
            return SUCCEED_COMPARE;
        }
        if (behindBitmapCompareStartY_60 != 0) {
            mNewBitmapStartY = behindBitmapCompareStartY_60;

            Log.i(TAG, "111 okokok 60 compare succeed moveY = " + moveY
                    + " behindBitmapCompareStartY_60 = " + behindBitmapCompareStartY_60
                    + " mOldBitmapEndY = " + mOldBitmapEndY);

            if (moveY < MOVE_Y_MIN) {
                return ERROR_TWO_BITMAP_IS_SAME;
            }
            return SUCCEED_COMPARE;
        }
        if (behindBitmapCompareStartY_50 != 0) {
            mNewBitmapStartY = behindBitmapCompareStartY_50;

            Log.i(TAG, "111 okokok 50 compare succeed moveY = " + moveY
                    + " behindBitmapCompareStartY_50 = " + behindBitmapCompareStartY_50
                    + " mOldBitmapEndY = " + mOldBitmapEndY);

            if (moveY < MOVE_Y_MIN) {
                return ERROR_TWO_BITMAP_IS_SAME;
            }
            return SUCCEED_COMPARE;
        }
        if (behindBitmapCompareStartY_40 != 0) {
            mNewBitmapStartY = behindBitmapCompareStartY_40;

            Log.i(TAG, "111 okokok 40 compare succeed moveY = " + moveY
                    + " behindBitmapCompareStartY_40 = " + behindBitmapCompareStartY_40
                    + " mOldBitmapEndY = " + mOldBitmapEndY);

            if (moveY < MOVE_Y_MIN) {
                return ERROR_TWO_BITMAP_IS_SAME;
            }
            return SUCCEED_COMPARE;
        }
        if (behindBitmapCompareStartY_30 != 0) {
            mNewBitmapStartY = behindBitmapCompareStartY_30;

            Log.i(TAG, "111 okokok 30 compare succeed moveY = " + moveY
                    + " behindBitmapCompareStartY_30 = " + behindBitmapCompareStartY_30
                    + " mOldBitmapEndY = " + mOldBitmapEndY);

            if (moveY < MOVE_Y_MIN) {
                return ERROR_TWO_BITMAP_IS_SAME;
            }
            return SUCCEED_COMPARE;
        }
        if (behindBitmapCompareStartY_20 != 0) {
            mNewBitmapStartY = behindBitmapCompareStartY_20;

            Log.i(TAG, "111 okokok 20 compare succeed moveY = " + moveY
                    + " behindBitmapCompareStartY_20 = " + behindBitmapCompareStartY_20
                    + " mOldBitmapEndY = " + mOldBitmapEndY);

            if (moveY < MOVE_Y_MIN) {
                return ERROR_TWO_BITMAP_IS_SAME;
            }
            return SUCCEED_COMPARE;
        }
        if (behindBitmapCompareStartY_10 != 0) {
            mNewBitmapStartY = behindBitmapCompareStartY_10;

            Log.i(TAG, "111 okokok 10 compare succeed moveY = " + moveY
                    + " behindBitmapCompareStartY_10 = " + behindBitmapCompareStartY_10
                    + " mOldBitmapEndY = " + mOldBitmapEndY);

            if (moveY < MOVE_Y_MIN) {
                return ERROR_TWO_BITMAP_IS_SAME;
            }
            return SUCCEED_COMPARE;
        }
        if (behindBitmapCompareStartY_5 != 0) {
            mNewBitmapStartY = behindBitmapCompareStartY_5;

            Log.i(TAG, "111 okokok 5 compare succeed moveY = " + moveY
                    + " behindBitmapCompareStartY_5 = " + behindBitmapCompareStartY_5
                    + " mOldBitmapEndY = " + mOldBitmapEndY);

            if (moveY < MOVE_Y_MIN) {
                return ERROR_TWO_BITMAP_IS_SAME;
            }
            return SUCCEED_COMPARE;
        }
        /********* look if have 5 lines equal end ***************/

        return ERROR_UNKNOW_STATE;
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
        int compareWidth = oldPixels.length - mBorderNotCompareWidth;
        int differentCount = 0;

        for (int i = mBorderNotCompareWidth; i < compareWidth; i++) {
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
        int compareWidth = pixels.length - mBorderNotCompareWidth;
        for (int i = mBorderNotCompareWidth; i < compareWidth; i++) {
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
    private int getOldBitmapStartCompareY(Bitmap bitmap) {
        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();

        int[] pixels = new int[width];
        for (int i = (height - mBottomNotCompareHeight - 1); i >= (USE_TO_COMPARE_PIXEL_HEIGHT_500 + mTopNotCompareHeight); i--) {
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
        int state = becompareTwoBitmap(oldBitmap, newBitmap);
        Log.i(TAG, "collageLongBitmap start state = " + state);

        if (state != SUCCEED_COMPARE) {//failed, stop
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
