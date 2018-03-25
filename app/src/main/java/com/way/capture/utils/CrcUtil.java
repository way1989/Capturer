package com.way.capture.utils;

import java.util.Arrays;

/**
 * Created by way on 2018/3/24.
 */

public class CrcUtil {

    /**
     * 为Byte数组添加两位CRC校验
     *
     * @param buf
     * @return
     */
    public static byte[] setParamCRC(byte[] buf) {
        int MASK = 0x0001, CRCSEED = 0x0810;
        int remain = 0;

        byte val;
        for (int i = 0; i < buf.length; i++) {
            val = buf[i];
            for (int j = 0; j < 8; j++) {
                if (((val ^ remain) & MASK) != 0) {
                    remain ^= CRCSEED;
                    remain >>= 1;
                    remain |= 0x8000;
                } else {
                    remain >>= 1;
                }
                val >>= 1;
            }
        }

        byte[] crcByte = new byte[2];
        crcByte[0] = (byte) ((remain >> 8) & 0xff);
        crcByte[1] = (byte) (remain & 0xff);

        // 将新生成的byte数组添加到原数据结尾并返回
        return concatAll(buf, crcByte);
    }

    /**
     * 根据起始和结束下标截取byte数组
     *
     * @param bytes
     * @param start
     * @param end
     * @return
     */
    private static byte[] getBytesByindex(byte[] bytes, int start, int end) {
        byte[] returnBytes = new byte[end - start + 1];
        for (int i = 0; i < returnBytes.length; i++) {
            returnBytes[i] = bytes[start + i];
        }
        return returnBytes;
    }

    /**
     * 对buf中offset以前crcLen长度的字节作crc校验，返回校验结果
     *
     * @param buf    byte[]
     * @param offset int
     * @param crcLen int　crc校验的长度
     * @return int　crc结果
     */
    private static int calcCRC(byte[] buf, int offset, int crcLen) {
        int MASK = 0x0001, CRCSEED = 0x0810;
        int start = offset;
        int end = offset + crcLen;
        int remain = 0;

        byte val;
        for (int i = start; i < end; i++) {
            val = buf[i];
            for (int j = 0; j < 8; j++) {
                if (((val ^ remain) & MASK) != 0) {
                    remain ^= CRCSEED;
                    remain >>= 1;
                    remain |= 0x8000;
                } else {
                    remain >>= 1;
                }
                val >>= 1;
            }
        }
        return remain;
    }

    /**
     * 对buf中offset以前crcLen长度的字节作crc校验，返回校验结果
     *
     * @param buf    byte[]
     * @param start  int
     * @param crcLen int　crc校验的长度
     * @return int　crc结果
     */
    private static int calcCRC(int[] buf, int start, int crcLen) {
        final int MASK = 0x0001, CRC_SEED = 0x0810;
        int end = start + crcLen;
        int remain = 0;

        int val;
        for (int i = start; i < end; i++) {
            val = buf[i];
            for (int j = 0; j < 8; j++) {
                if (((val ^ remain) & MASK) != 0) {
                    remain ^= CRC_SEED;
                    remain >>= 1;
                    remain |= 0x8000;
                } else {
                    remain >>= 1;
                }
                val >>= 1;
            }
        }
        return remain;
    }

    /***
     * CRC校验是否通过
     *
     * @param srcByte
     * @param length
     * @return
     */
    public static boolean isPassCRC(byte[] srcByte, int length) {
        // 取出除crc校验位的其他数组，进行计算，得到CRC校验结果
        int calcCRC = calcCRC(srcByte, 0, srcByte.length - length);

        // 取出CRC校验位，进行计算
        int receive = toInt(getBytesByindex(srcByte, srcByte.length - length, srcByte.length - 1));

        // 比较
        return calcCRC == receive;
    }

    /**
     * 多个数组合并
     *
     * @param first
     * @param rest
     * @return
     */
    public static byte[] concatAll(byte[] first, byte[]... rest) {
        int totalLength = first.length;
        for (byte[] array : rest) {
            totalLength += array.length;
        }
        byte[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (byte[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }

    /**
     * Byte转换为Int
     *
     * @param b
     * @return
     */
    public static int toInt(byte[] b) {
        return toInt(b, 0, 4);
    }

    /**
     * Byte转换为Int
     *
     * @param b
     * @param off
     * @param len
     * @return
     */
    public static int toInt(byte[] b, int off, int len) {
        int st = 0;
        if (off < 0)
            off = 0;
        if (len > 4)
            len = 4;
        for (int i = 0; i < len && (i + off) < b.length; i++) {
            st <<= 8;
            st += (int) b[i + off] & 0xff;
        }
        return st;
    }
}
