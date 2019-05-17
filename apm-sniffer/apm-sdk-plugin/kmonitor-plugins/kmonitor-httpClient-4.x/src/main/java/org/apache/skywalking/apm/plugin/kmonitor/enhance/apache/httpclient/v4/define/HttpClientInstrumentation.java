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

package org.apache.skywalking.apm.plugin.kmonitor.enhance.apache.httpclient.v4.define;

import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.plugin.kmonitor.common.KmonitorClassEnhancePluginDefine;

public abstract class HttpClientInstrumentation extends KmonitorClassEnhancePluginDefine {

    private static final String TRACE_INTERCEPT_CLASS = "org.apache.skywalking.apm.plugin.httpClient.v4" +
            ".HttpClientExecuteInterceptor";
    private static final String MONITOR_INTERCEPT_CLASS = "org.apache.skywalking.apm.plugin.kmonitor.enhance.apache" +
            ".httpclient.v4.HttpClientExecuteInterceptor";

    @Override
    protected ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return null;
    }

    protected String[] getInstanceMethodsInterceptors() {
        return new String[]{TRACE_INTERCEPT_CLASS, MONITOR_INTERCEPT_CLASS};
    }
}
