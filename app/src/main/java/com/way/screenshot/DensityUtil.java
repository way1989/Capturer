package com.way.screenshot;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class DensityUtil {
	public static int dip2px(Context context, float dip) {
		return (int) (0.5F + dip * context.getResources().getDisplayMetrics().density);
	}

	public static int px2dip(Context context, float px) {
		return (int) (0.5F + px / context.getResources().getDisplayMetrics().density);
	}

	/**
	 * 获取手机屏幕高度
	 *
	 * @return
	 */
	public static int getDisplayHeight(Context context) {
		DisplayMetrics dm = new DisplayMetrics();
		// 获取屏幕信息
		WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		windowManager.getDefaultDisplay().getMetrics(dm);
		return dm.heightPixels;
	}

	/**
	 * 获取手机屏幕宽度
	 *
	 * @return
	 */
	public static int getDisplayWidth(Context context) {
		DisplayMetrics dm = new DisplayMetrics();
		// 获取屏幕信息
		WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		windowManager.getDefaultDisplay().getMetrics(dm);
		return dm.widthPixels;
	}

}