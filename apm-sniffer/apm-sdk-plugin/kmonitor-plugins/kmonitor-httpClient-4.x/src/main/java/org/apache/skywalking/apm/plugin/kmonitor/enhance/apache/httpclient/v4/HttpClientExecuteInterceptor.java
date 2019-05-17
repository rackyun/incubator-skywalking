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

package org.apache.skywalking.apm.plugin.kmonitor.enhance.apache.httpclient.v4;

import com.keep.monitor.interfaces.metrics.ITimerContext;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.plugin.kmonitor.common.MonitorContext;

import java.lang.reflect.Method;

public class HttpClientExecuteInterceptor implements InstanceMethodsAroundInterceptor {

    private static final String PROBE_CONTEXT = "http.client.probe.context";

    @Override public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        if (allArguments[0] == null || allArguments[1] == null) {
            // illegal args, can't trace. ignore.
            return;
        }
        final HttpHost httpHost = (HttpHost)allArguments[0];
        HttpRequest httpRequest = (HttpRequest)allArguments[1];
        ITimerContext context = MonitorContext.getMetrics().probe()
                .tag("method", httpRequest.getRequestLine().getMethod())
                .tag("target.host", httpHost.getHostName()).timer("http.client").time();
        ContextManager.getRuntimeContext().put(PROBE_CONTEXT, context);
    }

    @Override public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, Object ret) throws Throwable {
        if (allArguments[0] == null || allArguments[1] == null) {
            return ret;
        }

        ITimerContext context = (ITimerContext) ContextManager.getRuntimeContext().get(PROBE_CONTEXT);
        if (context != null) {
            context.stop();
            ContextManager.getRuntimeContext().remove(PROBE_CONTEXT);
        }

        if (ret != null) {
            HttpResponse response = (HttpResponse)ret;
            StatusLine responseStatusLine = response.getStatusLine();
            final HttpHost httpHost = (HttpHost)allArguments[0];
            HttpRequest httpRequest = (HttpRequest)allArguments[1];
            if (responseStatusLine != null) {
                int statusCode = responseStatusLine.getStatusCode();
                if (statusCode >= 400) {
                    MonitorContext.getMetrics().probe().tag("method", httpRequest.getRequestLine().getMethod())
                        .tag("target.host", httpHost.getHostName())
                        .tag("httpCode", String.valueOf(statusCode)).counter("http.client.error").inc();
                }
            }
        }

        return ret;
    }

    @Override public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, Throwable t) {
        if (allArguments[0] == null || allArguments[1] == null) {
            // illegal args, can't trace. ignore.
            return;
        }
        final HttpHost httpHost = (HttpHost)allArguments[0];
        HttpRequest httpRequest = (HttpRequest)allArguments[1];
        MonitorContext.getMetrics().probe().tag("method", httpRequest.getRequestLine().getMethod())
                .tag("target.host", httpHost.getHostName()).counter("http.client.error").inc();
    }

}
