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

package org.apache.skywalking.apm.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author yunhai.hu
 * at 2018/11/29
 */
public class OperationNameUtil {
    private static final String PLACEHOLDER = "ID";
    private static final String TEXT_PLACEHOLDER = "Text";
    private static final String SEPARATOR = "/";
    private static final Pattern PATTERN = Pattern.compile("([0-9a-fA-F]{8,})|([0-9]{4,})");
    private static final Pattern WORD_PATTERN = Pattern.compile("^[a-zA-Z.()_-]*$");
    private static final Pattern BASE64_PATTERN =
            Pattern.compile("^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$");
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[^\\x00-\\xff]+");

    public static String normalizeUrl(String url) {
        List<String> pathSegments = Arrays.asList(url.split(SEPARATOR));
        if (pathSegments.size() <= 1) {
            return url;
        }
        return normalizeUrl(pathSegments);
    }

    public static String normalizeUrl(List<String> pathSegments) {
        List<String> newPathSegments = new ArrayList<String>(pathSegments.size());
        String replacedPath;
        for (String pathSegment : pathSegments) {
            if (StringUtil.isEmpty(pathSegment) || isWord(pathSegment)) {
                //do nothing
            } else if (isNumber(pathSegment) || isPlaceHolder(pathSegment)) {
                pathSegment = PLACEHOLDER;
            } else if ((replacedPath = complicatedStringReplace(pathSegment)) != null) {
                pathSegment = replacedPath;
            } else if ((replacedPath = base64Replace(pathSegment)) != null) {
                pathSegment = replacedPath;
            } else if (isChinese(pathSegment)) {
                pathSegment = TEXT_PLACEHOLDER;
            }
            newPathSegments.add(pathSegment);
        }
        return StringUtil.join(SEPARATOR.charAt(0), newPathSegments.toArray(new String[0]));
    }

    private static boolean isWord(String path) {
        Matcher matcher = WORD_PATTERN.matcher(path);
        return matcher.find();
    }

    private static String base64Replace(String path) {
        Matcher matcher = BASE64_PATTERN.matcher(path);
        if (matcher.find()) {
            return PLACEHOLDER;
        }
        return null;
    }

    private static boolean isChinese(String path) {
        Matcher matcher = CHINESE_PATTERN.matcher(path);
        return matcher.find();
    }

    private static String complicatedStringReplace(String path) {
        Matcher matcher = PATTERN.matcher(path);
        if (matcher.find()) {
            return matcher.replaceAll(PLACEHOLDER);
        }
        return null;
    }

    private static boolean isPlaceHolder(String data) {
        return data.startsWith("{") && data.endsWith("}");
    }

    private static boolean isHexOrUUID(String data) {
        char[] chars = data.toCharArray();
        return isHexOrUUID(chars, 0, chars.length);
    }

    private static boolean isHexOrUUID(char[] data, int begin, int end) {
        for (int i = begin; i < end; i++) {
            char c = data[i];
            if ((c >= '0') && (c <= '9')) continue;
            if ((c >= 'a') && (c <= 'f')) continue;
            if ((c >= 'A') && (c <= 'F')) continue;
            if (c == '-') continue;
            return false;
        }
        return true;
    }

    private static boolean isWxId(String data) {
        if (data.startsWith("wx") && data.length() == 18) {
            char[] charArray = data.toCharArray();
            return isHexOrUUID(charArray, 2, charArray.length);
        }
        return false;
    }

    private static boolean isNumber(String pathSegment) {
        //todo optimize
        if (pathSegment.length() == 0) {
            return false;
        }
        if (pathSegment.length() % 2 > 0 && pathSegment.length() < 8) {
            return StringUtil.isNumeric(pathSegment);
        } else {
            return isHexOrUUID(pathSegment) || isWxId(pathSegment);
        }
    }

    public static String operationEncode(String operationName) {
        String[] pathSegments = operationName.split("/");
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < pathSegments.length; i++) {
            stringBuilder.append(segmentEncode(pathSegments[i]));
            if (i != pathSegments.length - 1) {
                stringBuilder.append("/");
            }
        }
        return stringBuilder.toString();
    }

    private static final Map<Character, Character> ENCODE_MAP;

    static {
        ENCODE_MAP = new HashMap<Character, Character>();
        ENCODE_MAP.put(' ', '_');
    }


    private static String segmentEncode(String segment) {
        int idx;
        if ((idx = segment.indexOf("?")) > -1) {
            segment = segment.substring(0, idx);
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (char chr : segment.toCharArray()) {
            if (ENCODE_MAP.containsKey(chr)) {
                stringBuilder.append(ENCODE_MAP.get(chr));
            } else {
                stringBuilder.append(chr);
            }
        }
        return stringBuilder.toString();
    }
}