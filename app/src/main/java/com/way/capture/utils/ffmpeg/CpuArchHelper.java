package com.way.capture.utils.ffmpeg;

import android.os.Build;
import android.text.TextUtils;

class CpuArchHelper {

    static CpuArch getCpuArch() {
        String[] abis = Build.SUPPORTED_ABIS;
        if (abis == null || abis.length < 1)
            return CpuArch.NONE;
        for (String cpuArch : abis) {
            if (TextUtils.equals(cpuArch, getx86CpuAbi()))
                return CpuArch.x86;
            else if (TextUtils.equals(cpuArch, getArmeabiv7CpuAbi()))
                return CpuArch.ARMv7;
            else if (TextUtils.equals(cpuArch, getArmeabiCpuAbi())) // TODO: 16/4/23 I don't konw if it's OK!
                return CpuArch.ARMv7;
        }
        return CpuArch.NONE;
    }

    static String getx86CpuAbi() {
        return "x86";
    }

    static String getArmeabiv7CpuAbi() {
        return "armeabi-v7a";
    }

    static String getArmeabiCpuAbi() {
        return "armeabi";
    }
}
