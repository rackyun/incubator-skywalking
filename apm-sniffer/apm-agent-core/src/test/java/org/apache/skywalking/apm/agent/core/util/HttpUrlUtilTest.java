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

import org.apache.skywalking.apm.util.OperationNameUtil;
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

        String compStr = "/internal/social/like/liverun/574af9fe8d62d33b05b02575_city0795_1552902898443/likes";
        assertEquals("/internal/social/like/liverun/ID_cityID_ID/likes", OperationNameUtil.normalizeUrl(compStr));
        assertEquals("/internal/social/like/liverun/ID_cityID_ID/likes",
                OperationNameUtil.normalizeUrl("/internal/social/like/liverun/ID_cityID_ID/likes"));

        String dubboStr = "com.gotokeep.keepevent.api.service.QueryEventListService.findAllMyOngoingEventList(String)";
        assertEquals(dubboStr, OperationNameUtil.normalizeUrl(dubboStr));

        String qiniuStr = "/mkfile/ID/mimeType/YXBwbGljYXRpb24vb2N0ZXQtc3RyZWFt/key/NGVlN2UyMmVmMDJjM2Q5MjU2Zjk1MmRlMWRmMTVlMTYwNWY1OWI5ZV9zZWcuZ2lm";
        assertEquals("/mkfile/ID/mimeType/ID/key/ID", OperationNameUtil.normalizeUrl(qiniuStr));

    }

    @Test
    public void testUrlEncode() {
        String operationName = "Hystrix/GET /hello?cc={cc}/Execution";
        assertEquals("url encode error", "Hystrix/GET_/hello/Execution",
                OperationNameUtil.operationEncode(operationName));
    }
}