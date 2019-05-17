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


package org.apache.skywalking.apm.plugin.kmonitor.enhance.okhttp.v3;

import com.keep.monitor.interfaces.metrics.ITimerContext;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.plugin.kmonitor.common.MonitorContext;

import java.lang.reflect.Method;

/**
 * {@link RealCallInterceptor} intercept the synchronous http calls by the discovery of okhttp.
 *
 * @author peng-yongsheng
 */
public class RealCallInterceptor implements InstanceMethodsAroundInterceptor {

    private static final String PROBE_CONTEXT = "okhttp.realCall.probe.context";

    /**
     * Get the {@link okhttp3.Request} from {@link EnhancedInstance}, then create {@link AbstractSpan} and set host,
     * port, kind, component, url from {@link okhttp3.Request}.
     * Through the reflection of the way, set the http header of context data into {@link okhttp3.Request#headers}.
     *
     * @param method
     * @param result change this result, if you want to truncate the method.
     * @throws Throwable
     */
    @Override public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        Request request = (Request)objInst.getSkyWalkingDynamicField();
        HttpUrl requestUrl = request.url();
        ITimerContext context = MonitorContext.getMetrics().probe().tag("method", request.method())
                .tag("target.host", requestUrl.host()).timer("okhttp").time();
        ContextManager.getRuntimeContext().put(PROBE_CONTEXT, context);
    }

    /**
     * Get the status code from {@link Response}, when status code greater than 400, it means there was some errors in
     * the server.
     * Finish the {@link AbstractSpan}.
     *
     * @param method
     * @param ret the method's original return value.
     * @return
     * @throws Throwable
     */
    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, Object ret) throws Throwable {
        ITimerContext context = (ITimerContext) ContextManager.getRuntimeContext().get(PROBE_CONTEXT);
        if (context != null) {
            context.stop();
            ContextManager.getRuntimeContext().remove(PROBE_CONTEXT);
        }

        Response response = (Response)ret;
        Request request = response.request();
        HttpUrl requestUrl = request.url();
        if (response != null) {
            int statusCode = response.code();
            if (statusCode >= 400) {
                MonitorContext.getMetrics().probe().tag("method", request.method()).tag("target.host", requestUrl.host())
                    .tag("httpCode", String.valueOf(statusCode)).counter("okhttp.error").inc();
            }
        }

        return ret;
    }

    @Override public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, Throwable t) {
        Request request = (Request)objInst.getSkyWalkingDynamicField();
        HttpUrl requestUrl = request.url();
        MonitorContext.getMetrics().probe().tag("method", request.method()).tag("target.host", requestUrl.host())
                .counter("okhttp.error").inc();
    }
}
