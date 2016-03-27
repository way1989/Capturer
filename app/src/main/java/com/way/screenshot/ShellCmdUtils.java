package com.way.screenshot;

import java.io.DataOutputStream;
import java.io.OutputStream;

/**
 * Created by android on 16-3-8.
 */
public class ShellCmdUtils {

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
        if(duration > 0) {
            sb.append(" ");
            sb.append(duration);
        }
        return sb.toString();
    }
}
