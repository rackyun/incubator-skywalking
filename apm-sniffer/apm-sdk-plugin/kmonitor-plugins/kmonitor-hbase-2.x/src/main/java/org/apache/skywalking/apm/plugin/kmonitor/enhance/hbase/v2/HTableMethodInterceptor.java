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

package org.apache.skywalking.apm.plugin.kmonitor.enhance.hbase.v2;

import com.keep.monitor.interfaces.metrics.ITimerContext;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.plugin.hbase.v2.HbaseEnhanceRequiredInfo;
import org.apache.skywalking.apm.plugin.kmonitor.common.MonitorContext;

import java.lang.reflect.Method;

/**
 * @author yunhai.hu
 * at 2019/3/20
 */
public class HTableMethodInterceptor implements InstanceMethodsAroundInterceptor {

    private static final ILog logger = LogManager.getLogger(HTableMethodInterceptor.class);

    private static final String PROBE_CONTEXT = "hbase.v2.probe.context";


    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                             Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {

        String executeMethod = method.getName();
        HbaseEnhanceRequiredInfo requiredInfo = (HbaseEnhanceRequiredInfo) objInst.getSkyWalkingDynamicField();
        if (requiredInfo == null) {
            return;
        }
        String tableName = requiredInfo.getTableName();
        ITimerContext context = MonitorContext.getMetrics().probe().tag("method", executeMethod)
                .tag("table.name", tableName).timer("hbase.client").time();
        ContextManager.getRuntimeContext().put(PROBE_CONTEXT, context);
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                              Class<?>[] argumentsTypes, Object ret) throws Throwable {
        ITimerContext context = (ITimerContext) ContextManager.getRuntimeContext().get(PROBE_CONTEXT);
        if (context != null) {
            context.stop();
            ContextManager.getRuntimeContext().remove(PROBE_CONTEXT);
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
        HbaseEnhanceRequiredInfo enhanceRequiredInfo = (HbaseEnhanceRequiredInfo) objInst.getSkyWalkingDynamicField();
        if (enhanceRequiredInfo == null) {
            return;
        }
        String tableName = enhanceRequiredInfo.getTableName();
        MonitorContext.getMetrics().probe().tag("method", method.getName()).tag("table.name", tableName)
                .counter("hbase.client.error").inc();
    }

}
