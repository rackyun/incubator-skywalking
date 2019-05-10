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
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.plugin.kmonitor.enhance.MonitorContext;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author yunhai.hu
 * at 2019/5/5
 */
public class TomcatConfigInterceptor implements InstanceMethodsAroundInterceptor {

    private static final ILog logger = LogManager.getLogger(TomcatConfigInterceptor.class);

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                             Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {

    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                              Class<?>[] argumentsTypes, Object ret) throws Throwable {
        try {
            KeepMetrics metrics = MonitorContext.getMetrics();
            SpringMonitorInterceptor interceptor = new SpringMonitorInterceptor(metrics);
            List<HandlerInterceptor> handlerInterceptors = (List<HandlerInterceptor>) allArguments[0];
            if (handlerInterceptors.size() == 0) {
                handlerInterceptors.add(interceptor);
            } else {
                handlerInterceptors.add(1, interceptor);
            }

        } catch (Exception e) {
            logger.error(e, "Add Spring Interceptor Exception.");
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {

    }
}
