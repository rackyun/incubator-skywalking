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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author yunhai.hu
 * at 2018/12/17
 */
public class HttpUrlUtilTest {

    @Test
    public void normalizeUrl() {
        String url = "/hello/123";
        String ret = OperationNameUtil.normalizeUrl(url);
        assertEquals("/hello/ID", ret);

        String wxUrl = "/hello/wxfcf4d8d8ad24b86a";
        String wxRet = OperationNameUtil.normalizeUrl(wxUrl);
        assertEquals("/hello/ID", wxRet);
    }

    @Test
    public void testUrlEncode() {
        String operationName = "Hystrix/GET /hello?cc={cc}/Execution";
        assertEquals("url encode error", "Hystrix/GET_/hello/Execution", OperationNameUtil.operationEncode(operationName));
    }
}