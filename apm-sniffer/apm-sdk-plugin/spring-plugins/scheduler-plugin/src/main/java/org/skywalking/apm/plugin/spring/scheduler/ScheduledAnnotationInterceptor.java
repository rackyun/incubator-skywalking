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


package org.skywalking.apm.plugin.spring.scheduler;

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.StringTag;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;

/**
 * @author yunhai.hu
 * at 2019/4/8
 */
public class ScheduledAnnotationInterceptor implements InstanceMethodsAroundInterceptor {

    private static final String SCHEDULE_PREFIX = "Scheduled/";
    private static final String SCHEDULE_INVOKE_FLAG = "SW_SCHEDULE_INVOKE_FLAG";

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {

        Scheduled scheduled = method.getAnnotation(Scheduled.class);
        if (scheduled != null && !ContextManager.isActive()) {
            ContextManager.getRuntimeContext().put(SCHEDULE_INVOKE_FLAG, Boolean.TRUE);
            String operationName = SCHEDULE_PREFIX + method.getName();
            AbstractSpan span = ContextManager.createEntrySpan(operationName, null);
            span.tag(new StringTag("scheduled.cron"), scheduled.cron());
            span.setComponent(ComponentsDefine.SPRING_SCHEDULED);
            SpanLayer.asScheduled(span);
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                              Class<?>[] argumentsTypes, Object ret) throws Throwable {
        Boolean scheduleInvoked = (Boolean) ContextManager.getRuntimeContext().get(SCHEDULE_INVOKE_FLAG);
        try {
            if (scheduleInvoked == null || !scheduleInvoked) {
                return ret;
            }
            ContextManager.stopSpan();
            return ret;
        } finally {
            ContextManager.getRuntimeContext().remove(SCHEDULE_INVOKE_FLAG);
        }
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {
        ContextManager.activeSpan().log(t).errorOccurred();
    }
}
