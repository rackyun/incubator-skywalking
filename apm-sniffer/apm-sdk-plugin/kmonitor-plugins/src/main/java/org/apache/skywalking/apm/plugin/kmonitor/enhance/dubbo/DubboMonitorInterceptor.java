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

package org.apache.skywalking.apm.plugin.kmonitor.enhance.dubbo;

import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcContext;
import com.keep.monitor.agent.core.KeepMetrics;
import com.keep.monitor.interfaces.IProbe;
import com.keep.monitor.interfaces.metrics.ICounter;
import com.keep.monitor.interfaces.metrics.ITimerContext;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.plugin.kmonitor.enhance.MonitorContext;

import java.lang.reflect.Method;

/**
 * @author yunhai.hu
 * at 2019/5/5
 */
public class DubboMonitorInterceptor implements InstanceMethodsAroundInterceptor {

    private static final ILog logger = LogManager.getLogger(DubboMonitorInterceptor.class);
    private static final String PROBE_CONTEXT_KEY = "dubbo.probe.context";
    private static final String PROBE_COUNTER_KEY = "dubbo.probe.counter";

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                             Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        logger.debug("dubbo monitor invoke beforeMethod");
        KeepMetrics metrics = MonitorContext.getMetrics();
        ITimerContext context = null;
        ICounter counter = null;
        Invoker invoker = (Invoker) allArguments[0];
        Invocation invocation = (Invocation) allArguments[1];
        String methodName = invocation.getMethodName();
        String className = invoker.getInterface().getSimpleName();
        boolean isConsumer = RpcContext.getContext().isConsumerSide();
        logger.debug("dubbo monitor beforeMethod, {} className={} methodName={}",
                isConsumer ? "consumer" : "provider", className, methodName);
        IProbe probe = metrics.probe().tag("remote", className).tag("func", methodName);
        if (isConsumer) {
            context = probe.timer("rpc.client").time();
            counter = probe.counter("rpc.client.error");
        } else {
            context = probe.timer("rpc.server").time();
            counter = probe.counter("rpc.server.error");
        }
        ContextManager.getRuntimeContext().put(PROBE_CONTEXT_KEY, context);
        ContextManager.getRuntimeContext().put(PROBE_COUNTER_KEY, counter);
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                              Class<?>[] argumentsTypes, Object ret) throws Throwable {
        ITimerContext context = (ITimerContext) ContextManager.getRuntimeContext().get(PROBE_CONTEXT_KEY);
        ICounter counter = (ICounter) ContextManager.getRuntimeContext().get(PROBE_COUNTER_KEY);
        if (context != null) {
            context.stop();
            ContextManager.getRuntimeContext().remove(PROBE_CONTEXT_KEY);
        }
        if (counter != null) {
            ContextManager.getRuntimeContext().remove(PROBE_COUNTER_KEY);
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
        ICounter counter = (ICounter) ContextManager.getRuntimeContext().get(PROBE_COUNTER_KEY);
        if (counter != null) {
            counter.inc();
        }
    }
}
