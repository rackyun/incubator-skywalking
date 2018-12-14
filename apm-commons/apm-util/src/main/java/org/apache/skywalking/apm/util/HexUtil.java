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

    private HexUtil() {}

    public static String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit(num & 0xF, 16);
        return new String(hexDigits);
    }

    public static String encodeHexString(byte[] byteArray) {
        StringBuffer hexStringBuffer = new StringBuffer();
        for (int i = 0; i < byteArray.length; i++) {
            hexStringBuffer.append(byteToHex(byteArray[i]));
        }
        return hexStringBuffer.toString();
    }

    public static byte hexToByte(String hexString) {
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

    public static byte[] decodeHexString(String hexString) {
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

    //integer convert to byte
    public static byte intToByte(int x) {
        return (byte) x;
    }

    public static int byteToInt(byte b) {
        return b & 0xFF;
    }

    //byte convert to byte
    public static int byteArrayToInt(byte[] b, int begin) {
        return   b[begin + 3] & 0xFF |
                (b[begin + 2] & 0xFF) << 8 |
                (b[begin + 1] & 0xFF) << 16 |
                (b[begin] & 0xFF) << 24;
    }

    //byte convert to byte
    public static long byteArrayToLong(byte[] b, int begin) {
        return  (long) b[begin + 7] & 0xFF |
                (long) (b[begin + 6] & 0xFF) << 8 |
                (long) (b[begin + 5] & 0xFF) << 16 |
                (long) (b[begin + 4] & 0xFF) << 24 |
                (long) (b[begin + 3] & 0xFF) << 32 |
                (long) (b[begin + 2] & 0xFF) << 40 |
                (long) (b[begin + 1] & 0xFF) << 48 |
                (long) (b[begin] & 0xFF) << 56;
    }

    public static byte[] intToByteArray(int a) {
        return new byte[]{(byte) ((a >> 24) & 0xFF), (byte) ((a >> 16) & 0xFF), (byte) ((a >> 8) & 0xFF), (byte) (a & 0xFF)};
    }

    public static byte[] longToByteArray(long a) {
        return new byte[]{(byte) ((a >> 54) & 0xFF), (byte) ((a >> 48) & 0xFF), (byte) ((a >> 40) & 0xFF), (byte) ((a >> 32) & 0xFF), (byte) ((a >> 24) & 0xFF), (byte) ((a >> 16) & 0xFF), (byte) ((a >> 8) & 0xFF), (byte) (a & 0xFF)};
    }

    public static String traceIdToString(long part1, long part2, long part3) {
        StringBuilder stringBuilder = new StringBuilder();
        byte[] part1Byte = HexUtil.intToByteArray((int) part1);
        stringBuilder.append(encodeHexString(part1Byte));
        byte[] part2Byte = HexUtil.intToByteArray((int) part2);
        stringBuilder.append(encodeHexString(part2Byte));
        byte[] part3byte = HexUtil.longToByteArray(part3);
        stringBuilder.append(encodeHexString(part3byte));
        return stringBuilder.toString();
    }

    public static long[] stringToIDParts(String hexStr) {
        long[] result = new long[3];
        if (hexStr != null && hexStr.length() == 32) {
            result[0] = (long) HexUtil.byteArrayToInt(HexUtil.decodeHexString(hexStr.substring(0, 8)), 0);
            result[1] = (long) HexUtil.byteArrayToInt(HexUtil.decodeHexString(hexStr.substring(8, 16)), 0);
            result[2] = HexUtil.byteArrayToLong(HexUtil.decodeHexString(hexStr.substring(16, 32)), 0);
        }
        return result;
    }
}
