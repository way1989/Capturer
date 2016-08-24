package com.way.capture.module.screenshot;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by android on 16-3-8.
 */
public class ScrollUtils {
    private static final int CMD_START_MOVE_X = 10;

    public static void scrollToNextScreen(int screenHeight, long duration) throws IOException {
        int cmdStartY = screenHeight / 2;
        int cmdEndY = screenHeight / 5;
        ScrollUtils.execShellCmd(ScrollUtils.getSwipeCmd(CMD_START_MOVE_X, cmdStartY,
                CMD_START_MOVE_X, cmdEndY, duration));
    }

    public static void execShellCmd(String cmd) throws IOException {
        Process process = Runtime.getRuntime().exec("su");
        OutputStream outputStream = process.getOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        dataOutputStream.writeBytes(cmd);
        dataOutputStream.flush();
        dataOutputStream.close();
        outputStream.close();
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
