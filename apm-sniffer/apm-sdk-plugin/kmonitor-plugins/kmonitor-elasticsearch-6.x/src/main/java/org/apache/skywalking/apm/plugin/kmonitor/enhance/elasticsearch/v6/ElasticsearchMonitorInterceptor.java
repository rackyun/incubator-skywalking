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

package org.apache.skywalking.apm.plugin.kmonitor.enhance.elasticsearch.v6;

import com.keep.monitor.interfaces.IProbe;
import com.keep.monitor.interfaces.metrics.ITimerContext;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.plugin.elasticsearch.v6.ElasticSearchEnhanceInfo;
import org.apache.skywalking.apm.plugin.kmonitor.common.MonitorContext;
import org.apache.skywalking.apm.util.StringUtil;

import java.lang.reflect.Method;

/**
 * @author yunhai.hu
 */
public class ElasticsearchMonitorInterceptor implements InstanceMethodsAroundInterceptor {

    private static final String PROBE_CONTEXT = "elasticsearch.v6.probe.context";

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                             Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        String clusterName = getClusterName(objInst);
        IProbe probe = MonitorContext.getMetrics().probe().tag("method", method.getName());
        if (!StringUtil.isEmpty(clusterName)) {
            probe.tag("clusterName", clusterName).tag("action", allArguments[1].getClass().getSimpleName());
        }
        ITimerContext context = probe.timer("elasticsearch.client").time();
        ContextManager.getRuntimeContext().put(PROBE_CONTEXT, context);
    }

    private String getClusterName(EnhancedInstance objInst) {
        if (objInst.getSkyWalkingDynamicField() instanceof EnhancedInstance) {
            ElasticSearchEnhanceInfo enhanceInfo = (ElasticSearchEnhanceInfo) ((EnhancedInstance) objInst.getSkyWalkingDynamicField()).getSkyWalkingDynamicField();
            if (enhanceInfo != null) {
                return enhanceInfo.getClusterName();
            }
        }
        return null;
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
        String clusterName = getClusterName(objInst);
        IProbe probe = MonitorContext.getMetrics().probe().tag("method", method.getName());
        if (!StringUtil.isEmpty(clusterName)) {
            probe.tag("clusterName", clusterName).tag("action", allArguments[1].getClass().getSimpleName());
        }
        probe.counter("elasticsearch.client.error").inc();
    }
}
