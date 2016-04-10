package com.way.captain.utils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.MenuItem;

import com.way.captain.R;
import com.way.captain.data.ScreenshotInfos;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;

/**
 * Created by way on 16/2/28.
 */
public abstract class ScreenshotPopupMenuHelper extends PopupMenuHelper {
    private static final String SCREENSHOT_SHARE_SUBJECT_TEMPLATE = "Screenshot (%s)";
    protected ScreenshotInfos mScreenshotInfo;

    public ScreenshotPopupMenuHelper(Activity activity) {
        super(activity);
        mType = PopupMenuType.Screenshot;
    }

    public abstract ScreenshotInfos getScreenshotInfos(int position);

    @Override
    public PopupMenuType onPreparePopupMenu(int position) {
        mScreenshotInfo = getScreenshotInfos(position);
        if (mScreenshotInfo == null)
            return null;
        return PopupMenuType.Screenshot;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.gif_item_share:
                String subjectDate = DateFormat.getDateTimeInstance().format(new Date(System.currentTimeMillis()));
                String subject = String.format(SCREENSHOT_SHARE_SUBJECT_TEMPLATE, subjectDate);
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("image/png");
                sharingIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(mScreenshotInfo.getPath())));
                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
                Intent chooserIntent = Intent.createChooser(sharingIntent, null);
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                mActivity.startActivity(chooserIntent);
                return true;
            case R.id.gif_item_delete:

                return true;
            default:
                break;
        }
        return super.onMenuItemClick(item);
    }
}
