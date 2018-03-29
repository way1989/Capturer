package com.way.capture.utils;

import android.util.Log;

/**
 * Created by android on 16-2-3.
 */
public class FfmpegUtil {

    public static final int MAX_GIF_LENGTH = 30;

    public static String[] getVideo2gifCommand(long start, long length, int frame,
                                               String sourcePath, String outPath, int width, int height) {
        StringBuilder command = new StringBuilder("-ss ");
        command.append(start);
        command.append(" -t ");
        command.append(length);
        command.append(" -i ");
        command.append(sourcePath);
        command.append(" -s ");
        command.append(width + "x" + height);
        command.append(" -f ");
        command.append("gif");
        command.append(" -r ");
        command.append(frame);
        command.append(" ");
        command.append(outPath);
        Log.i("broncho", "command = " + command.toString());
        return command.toString().split(" ");
    }

}
