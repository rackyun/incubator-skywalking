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

package org.apache.skywalking.apm.plugin.hystrix.v1;

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.util.OperationNameUtil;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

import java.lang.reflect.Method;

public class HystrixCommandRunInterceptor implements InstanceMethodsAroundInterceptor {
    private ILog logger = LogManager.getLogger(HystrixCommandRunInterceptor.class);

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
        MethodInterceptResult result) throws Throwable {
        // create a local span, and continued, The `execution method` running in other thread if the
        // hystrix strategy is `THREAD`.
        EnhanceRequireObjectCache enhanceRequireObjectCache = (EnhanceRequireObjectCache)objInst.getSkyWalkingDynamicField();
        ContextSnapshot snapshot = enhanceRequireObjectCache.getContextSnapshot();
        if (logger.isDebugEnable()) {
            logger.debug("hystrix interceptor entryOperationName={}, parentOperationName={}, spanId={}",
                    snapshot.getEntryOperationName(), snapshot.getParentOperationName(), snapshot.getSpanId());
        }

        String endpointName = OperationNameUtil.operationEncode(enhanceRequireObjectCache.getOperationNamePrefix() + "/Execution");
        AbstractSpan activeSpan = ContextManager.createLocalSpan(endpointName, snapshot != null && snapshot.isSample());
        activeSpan.setComponent(ComponentsDefine.HYSTRIX);
        if (snapshot != null) {
            ContextManager.continued(snapshot);
        }
        // Because of `fall back` method running in other thread. so we need capture concurrent span for tracing.
        enhanceRequireObjectCache.setContextSnapshot(ContextManager.capture());
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
        Object ret) throws Throwable {
        ContextManager.stopSpan();
        return ret;
    }

    @Override public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, Throwable t) {
        ContextManager.activeSpan().errorOccurred().log(t);
    }
}
