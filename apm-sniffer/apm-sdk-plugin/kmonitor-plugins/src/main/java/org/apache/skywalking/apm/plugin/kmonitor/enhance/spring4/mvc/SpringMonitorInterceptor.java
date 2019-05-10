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

package org.apache.skywalking.apm.plugin.kmonitor.enhance.spring4.mvc;

import com.keep.monitor.agent.core.KeepMetrics;
import com.keep.monitor.agent.core.utils.StringUtils;
import com.keep.monitor.interfaces.metrics.ITimerContext;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

public class SpringMonitorInterceptor extends HandlerInterceptorAdapter {
    private static final ILog logger = LogManager.getLogger(SpringMonitorInterceptor.class);
    private static final String MONITOR_API_KEY = "KMONITOR_API";
    private static final String MONITOR_CONTEXT_KEY = "KMONITOR_CONTEXT";
    private static final String MONITOR_INFO_KEY = "KMONITOR_INFO";
    private static final String ERROR404 = "__404_NOT_FOUND";

    private KeepMetrics metrics;

    SpringMonitorInterceptor(KeepMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String[] methodAndPath = parseMthodAndPath((HandlerMethod) handler);
        logger.debug("preHandle method: {} path: {}", methodAndPath[0], methodAndPath[1]);

        request.setAttribute(MONITOR_API_KEY, methodAndPath[1]);
        int status = getStatus(response);
        if (status >= 400) {
            if (status == 404) {
                request.setAttribute(MONITOR_API_KEY, ERROR404);
            }
            return true;
        }

        ITimerContext context = metrics.probe().tag("method", methodAndPath[0]).tag("api", methodAndPath[1]).timer("http.server").time();
        request.setAttribute(MONITOR_CONTEXT_KEY, context);
        request.setAttribute(MONITOR_INFO_KEY, methodAndPath[0] + " " + methodAndPath[1]);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object o,
                                Exception e) throws Exception {
        String info = (String) request.getAttribute(MONITOR_INFO_KEY);
        logger.debug("afterCompletion {}", info);
        ITimerContext context = (ITimerContext) request.getAttribute(MONITOR_CONTEXT_KEY);
        if (context != null) {
            context.stop();
        }

        int status = getStatus(response);
        if (status >= 400) {
            String api = (String) request.getAttribute(MONITOR_API_KEY);
            String method = request.getMethod();
            metrics.probe().tag("api", api).tag("method", method).tag("error.code", String.valueOf(status)).counter("error").inc();
        }
    }

    private String[] parseMthodAndPath(HandlerMethod handlerMethod) {
        Method method = handlerMethod.getMethod();
        String[] methodMapping = parseMethodMapping(method);
        methodMapping[1] = getFullUrlPath(method, methodMapping);
        return methodMapping;
    }

    private String[] parseMethodMapping(Method method) {
        String[] methodMapping = new String[]{RequestMethod.GET.name(), ""};
        String[] values;

        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
        if (requestMapping != null) {
            RequestMethod[] methods = requestMapping.method();
            if (methods != null && methods.length > 0) {
                methodMapping[0] = methods[0].name();
            }
            values = requestMapping.value();
            if (values.length > 0) {
                methodMapping[1] = values[0];
            }
            return methodMapping;
        }

        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        if (getMapping != null) {
            values = getMapping.value();
            if (values.length > 0) {
                methodMapping[1] = values[0];
            }
            return methodMapping;
        }

        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        if (postMapping != null) {
            methodMapping[0] = RequestMethod.POST.name();
            values = postMapping.value();
            if (values.length > 0) {
                methodMapping[1] = values[0];
            }
            return methodMapping;
        }

        PutMapping putMapping = method.getAnnotation(PutMapping.class);
        if (putMapping != null) {
            methodMapping[0] = RequestMethod.PUT.name();
            values = putMapping.value();
            if (values.length > 0) {
                methodMapping[1] = values[0];
            }
            return methodMapping;
        }

        DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
        if (deleteMapping != null) {
            methodMapping[0] = RequestMethod.DELETE.name();
            values = deleteMapping.value();
            if (values.length > 0) {
                methodMapping[1] = values[0];
            }
            return methodMapping;
        }

        return methodMapping;
    }

    private String getFullUrlPath(Method method, String[] methodMapping) {
        RequestMapping clazzMapping = getClassRequestMapping(method);
        String clazzPath = "";

        if (clazzMapping != null) {
            String[] values = clazzMapping.value();
            if (values.length > 0) {
                clazzPath = formatUri(values[0]);
            }
        }

        return clazzPath + formatUri(methodMapping[1]);
    }

    private RequestMapping getClassRequestMapping(Method method) {
        return method.getDeclaringClass().getAnnotation(RequestMapping.class);
    }

    private String formatUri(String uri) {
        return "/" + StringUtils.strip(uri, "/");
    }

    private int getStatus(HttpServletResponse response) {
        try {
            Method method = HttpServletResponse.class.getDeclaredMethod("getStatus");
            return (Integer) method.invoke(response);
        } catch (Exception e) {
            logger.error(e, "get response error");
        }
        return 200;
    }
}
