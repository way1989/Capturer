package com.way.captain.screenshot;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.OutputStream;

/**
 * Created by android on 16-3-8.
 */
public class ShellCmdUtils {
    private static final String TAG = "ShellCmdUtils";

    public static boolean isDeviceRoot() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            Log.i(TAG, "isDeviceRoot: process = " + process + ", process.getOutputStream() = " + process.getOutputStream());
            if (process != null && process.getOutputStream() != null)
                return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean execShellCmd(String cmd) {
        try {
            Process process = Runtime.getRuntime().exec("su");
            OutputStream outputStream = process.getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(
                    outputStream);
            dataOutputStream.writeBytes(cmd);
            dataOutputStream.flush();
            dataOutputStream.close();
            outputStream.close();
        } catch (Exception e) {
            return false;
            //e.printStackTrace();
        }
        return true;
    }

    public static String getSwipeCmd(int startX, int startY, int endX, int endY, long duration) {
        StringBuilder sb = new StringBuilder("input swipe");
        sb.append(" ");
        sb.append(startX);
        sb.append(" ");
        sb.append(startY);
        sb.append(" ");
        sb.append(endX);
        sb.append(" ");
        sb.append(endY);
        if (duration > 0) {
            sb.append(" ");
            sb.append(duration);
        }
        return sb.toString();
    }
}
