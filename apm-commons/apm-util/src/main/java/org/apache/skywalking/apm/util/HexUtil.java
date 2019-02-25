package org.apache.skywalking.apm.util;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

/**
 * @author yunhai.hu
 * at 2018/12/10
 */
public final class HexUtil {

    private HexUtil() {
    }

    private static byte hexToByte(String hexString) {
        int firstDigit = toDigit(hexString.charAt(0));
        int secondDigit = toDigit(hexString.charAt(1));
        return (byte) ((firstDigit << 4) + secondDigit);
    }

    private static int toDigit(char hexChar) {
        int digit = Character.digit(hexChar, 16);
        if (digit == -1) {
            throw new IllegalArgumentException(
                    "Invalid Hexadecimal Character: " + hexChar);
        }
        return digit;
    }

    private static byte[] decodeHexString(String hexString) {
        if (hexString.length() % 2 == 1) {
            throw new IllegalArgumentException(
                    "Invalid hexadecimal String supplied.");
        }

        byte[] bytes = new byte[hexString.length() / 2];
        for (int i = 0; i < hexString.length(); i += 2) {
            bytes[i / 2] = hexToByte(hexString.substring(i, i + 2));
        }
        return bytes;
    }

    //byte convert to byte
    private static int byteArrayToInt(byte[] b, int begin) {
        return b[begin + 3] & 0xFF |
                (b[begin + 2] & 0xFF) << 8 |
                (b[begin + 1] & 0xFF) << 16 |
                (b[begin] & 0xFF) << 24;
    }

    //byte convert to byte
    private static long longFrom8Bytes(byte[] input, int offset, boolean littleEndian) {
        long value = 0;
        for (int count = 0; count < 8; ++count) {
            int shift = (littleEndian ? count : (7 - count)) << 3;
            value |= ((long) 0xff << shift) & ((long) input[offset + count] << shift);
        }
        return value;
    }

    public static String traceIdToString(long part1, long part2, long part3) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(intHexString((int) part1, 8));
        stringBuilder.append(intHexString((int) part2, 8));
        stringBuilder.append(longHexString(part3, 16));
        return stringBuilder.toString();
    }

    private static String intHexString(int value, int length) {
        String hexString = Integer.toHexString(value);
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < length - hexString.length(); i++) {
            stringBuilder.append(0);
        }
        return stringBuilder.append(hexString).toString();
    }

    private static String longHexString(long value, int length) {
        String hexStr = Long.toHexString(value);
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < length - hexStr.length(); i++) {
            stringBuilder.append(0);
        }
        return stringBuilder.append(hexStr).toString();
    }

    public static long[] stringToIDParts(String hexStr) {
        long[] result = new long[3];
        if (hexStr != null && hexStr.length() == 32) {
            result[0] = (long) HexUtil.byteArrayToInt(HexUtil.decodeHexString(hexStr.substring(0, 8)), 0);
            result[1] = (long) HexUtil.byteArrayToInt(HexUtil.decodeHexString(hexStr.substring(8, 16)), 0);
            result[2] = longFrom8Bytes(HexUtil.decodeHexString(hexStr.substring(16, 32)), 0, false);
        }
        return result;
    }
}
