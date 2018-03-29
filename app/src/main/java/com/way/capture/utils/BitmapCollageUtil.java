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
public class BitmapCollageUtil {
    private static final String TAG = "BitmapCollageUtil";
    private static final int BASE_LINE_MOVE_NUM = 5;//基线移动次数
    private static final int COMPARE_LINE_NUM_MAX = 50;//连续50行像素一样，就认为找到了分割线
    private static final int PURE_LINE_DIF_NUM_MAX = 40;
    private static final int COLOR_LINE_DIF_NUM_MAX = 10;

    private static final int RGB_PIXEL_DISTANCE_MAX = 100;
    private static final int GRAY_PIXEL_RED_DIF_MAX = 50;
    private static final int GRAY_PIXEL_GREEN_DIF_MAX = 16;
    private static final int GRAY_PIXEL_BLUE_DIF_MAX = 40;

    private volatile static BitmapCollageUtil sInstance;

    private int TOP_NOT_COMPARE_HEIGHT;
    //    private int BOTTOM_NOT_COMPARE_HEIGHT;
    private int BORDER_NOT_COMPARE_WIDTH;
    private int MOVE_HEIGHT_MIN;
    private int mScreenWidth;

    private BitmapCollageUtil() {
    }

    public static BitmapCollageUtil getInstance() {
        if (sInstance == null) {
            synchronized (BitmapCollageUtil.class) {
                if (sInstance == null) {
                    sInstance = new BitmapCollageUtil();
                }
            }
        }
        return sInstance;
    }

    private void initFirstTime(int screenWidth, int screenHeight) {
        if (mScreenWidth == screenWidth) {
            return;
        }
        mScreenWidth = screenWidth;//用于判断是否初始化过参数

        TOP_NOT_COMPARE_HEIGHT = ViewUtils.getStatusBarHeight() + ViewUtils.dp2px(56);//56dp + status bar height
//        BOTTOM_NOT_COMPARE_HEIGHT = (int) (0.075f * screenHeight);//48dp
        BORDER_NOT_COMPARE_WIDTH = ViewUtils.dp2px(16);//16dp
        MOVE_HEIGHT_MIN = ViewUtils.dp2px(5);//5dp
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
        for (int i = offset; i <= (screenHeight - TOP_NOT_COMPARE_HEIGHT); i += 2) {
            //将第i行宽度为width的像素点存入数组pixels中
            bitmap.getPixels(pixels, 0, width, 0, height - 1 - i, width, 1);
            if (!isPureColorLine(pixels)) {
                return i;
            }
        }
        return -1;
    }


    private Pair<Integer, Integer> compareTwoBitmap(Bitmap firstBitmap, Bitmap secondBitmap) {
        final int screenHeight = secondBitmap.getHeight();//屏幕的高度就是第二个bitmap的高度

        //从下往上找到两个bitmap底部相同部分的高度
        final int bottomSameHeight = getBottomSameHeight(firstBitmap, secondBitmap);
        if (bottomSameHeight < 0 || bottomSameHeight == (screenHeight - TOP_NOT_COMPARE_HEIGHT)) {
            Log.e(TAG, "compareTwoBitmap... two bitmap is same");
            return null;
        }
        //bottomSameHeight += BOTTOM_NOT_COMPARE_HEIGHT;

        //减去底部和顶部不用比较的高度，剩下中间可以用于compare的区域
        final int compareHeight = screenHeight - bottomSameHeight - TOP_NOT_COMPARE_HEIGHT;
        //每次基线移动的高度
        final int baseLineReduceHeight = compareHeight / BASE_LINE_MOVE_NUM;
        Log.i(TAG, "compareTwoBitmap..."
                + ", bottomSameHeight = " + bottomSameHeight
                + ", baseLineReduceHeight = " + baseLineReduceHeight);

        //改变first bitmap基线5次，确保找到正确的分割线
        for (int i = 0; i < BASE_LINE_MOVE_NUM; i++) {
            //从下往上找到不是纯色的一行作为参考基线
            final int baseLine = getNotPureLineHeight(firstBitmap, screenHeight, bottomSameHeight + i * baseLineReduceHeight);
            Log.d(TAG, "compareTwoBitmap: getDividingLinePair count = " + (i +1) + ", baseLine = " + baseLine);
            Pair<Integer, Integer> pair = getDividingLinePair(firstBitmap, secondBitmap, baseLine);
            if (pair != null) {
                Log.d(TAG, "compareTwoBitmap: succeed!!! compare count = " + (i + 1));
                return pair;
            }
        }

        return null;
    }

    /**
     * 获取两个bitmap重合部分分割线
     *
     * @param firstBitmap  需要比较的bitmap
     * @param secondBitmap 被比较的bitmap
     * @param baseLine     参考基线
     * @return 是否找到分割线
     */
    private Pair<Integer, Integer> getDividingLinePair(Bitmap firstBitmap, Bitmap secondBitmap, int baseLine) {
        int firstBitmapWidth = firstBitmap.getWidth();
        int firstBitmapHeight = firstBitmap.getHeight();
        int secondBitmapWidth = secondBitmap.getWidth();
        int secondBitmapHeight = secondBitmap.getHeight();

        int oldY = baseLine;
        int newY = baseLine;
        int newStartY = 0;
        int count = 0;

        final int[] oldPixels = new int[firstBitmapWidth];
        final int[] newPixels = new int[secondBitmapWidth];

        while (secondBitmapHeight - newY > TOP_NOT_COMPARE_HEIGHT) {
            firstBitmap.getPixels(oldPixels, 0, firstBitmapWidth, 0, firstBitmapHeight - 1 - oldY, firstBitmapWidth, 1);
            secondBitmap.getPixels(newPixels, 0, secondBitmapWidth, 0, secondBitmapHeight - 1 - newY, secondBitmapWidth, 1);
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
            if (count >= COMPARE_LINE_NUM_MAX && Math.abs(oldY - newStartY) > MOVE_HEIGHT_MIN) {
                return new Pair<>(firstBitmapHeight - baseLine, secondBitmapHeight - newStartY + 1);
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
                //cost more time
                //计算颜色空间的距离
                /*final double distance = Math.sqrt(Math.pow(redDifferent, 2) + Math.pow(greenDifferent, 2) + Math.pow(blueDifferent, 2));
                if (distance > RGB_PIXEL_DISTANCE_MAX) {
                    differentCount++;
                }*/
                if (redDifferent > GRAY_PIXEL_RED_DIF_MAX
                        || greenDifferent > GRAY_PIXEL_GREEN_DIF_MAX
                        || blueDifferent > GRAY_PIXEL_BLUE_DIF_MAX) {
                    differentCount++;
                }
                //超过10个像素点颜色值不同，就认为两个像素数组不同
                if (differentCount >= COLOR_LINE_DIF_NUM_MAX) {
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
            if (differentCount > PURE_LINE_DIF_NUM_MAX) {
                return false;
            }
        }
        return true;
    }

    public Bitmap collageLongBitmap(Bitmap firstBitmap, Bitmap secondBitmap) {
        if (firstBitmap == null || firstBitmap.isRecycled()
                || secondBitmap == null || secondBitmap.isRecycled()
                || firstBitmap.getWidth() != secondBitmap.getWidth()
                || firstBitmap.getWidth() < 1 || firstBitmap.getHeight() < 1
                || secondBitmap.getWidth() < 1 || secondBitmap.getHeight() < 1) {
            return null;
        }
        //第一次初始化一些阀值
        initFirstTime(secondBitmap.getWidth(), secondBitmap.getHeight());

        final long collageStart = System.currentTimeMillis();
        Pair<Integer, Integer> pair = compareTwoBitmap(firstBitmap, secondBitmap);
        Log.d(TAG, "collageLongBitmap compareTwoBitmap end... isSucceed = " + (pair != null)
                + ", screenSize = " + secondBitmap.getWidth() + "x" + secondBitmap.getHeight()
                + ", firstBitmapEndY = " + (pair != null ? pair.first : -1)
                + ", secondBitmapStartY = " + (pair != null ? pair.second : -1)
                + ", cost = " + (System.currentTimeMillis() - collageStart) + "ms");

        if (pair == null) {//failed, stop
            Log.e(TAG, "collageLongBitmap: compare failed!!! pair == null");
            return null;
        }

        int width = firstBitmap.getWidth();
        int height = pair.first + (secondBitmap.getHeight() - pair.second);

        if (height < firstBitmap.getHeight()) {
            Log.e(TAG, "collageLongBitmap: compare failed!!! height too small");
            return null;
        }

        final long drawStart = System.currentTimeMillis();
        Bitmap result = GlideBitmapPool.getBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(firstBitmap, new Rect(0, 0, width, pair.first),
                new Rect(0, 0, width, pair.first), null);
        canvas.drawBitmap(secondBitmap, new Rect(0, pair.second, width, secondBitmap.getHeight()),
                new Rect(0, pair.first, width, height), null);
        Log.d(TAG, "collageLongBitmap drawBitmap end... cost = " + (System.currentTimeMillis() - drawStart) + "ms");
        GlideBitmapPool.putBitmap(firstBitmap);
        GlideBitmapPool.putBitmap(secondBitmap);
        return result;

    }

}
