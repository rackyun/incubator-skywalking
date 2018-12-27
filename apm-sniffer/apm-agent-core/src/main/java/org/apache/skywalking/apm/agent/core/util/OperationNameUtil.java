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

package org.apache.skywalking.apm.agent.core.util;


import org.apache.skywalking.apm.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author yunhai.hu
 * at 2018/11/29
 */
public class OperationNameUtil {
    private static final String PLACEHOLDER = "ID";
    private static final String SEPARATOR = "/";

    public static String normalizeUrl(String url) {
        List<String> pathSegments = Arrays.asList(url.split(SEPARATOR));
        return normalizeUrl(pathSegments);
    }

    public static String normalizeUrl(List<String> pathSegments) {
        List<String> newPathSegments = new ArrayList<String>(pathSegments.size());
        for (String pathSegment : pathSegments) {
            if (isNumber(pathSegment)) {
                pathSegment = PLACEHOLDER;
            }
            newPathSegments.add(pathSegment);
        }
        return StringUtil.join(SEPARATOR.charAt(0), newPathSegments.toArray(new String[0]));
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
}
