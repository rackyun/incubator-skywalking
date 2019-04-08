/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.skywalking.apm.plugin.elasticsearch.v6;

import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;

/**
 * @author yunhai.hu
 * at 2019/1/16
 */
public class ActionRequestBuilderInterceptor implements InstanceConstructorInterceptor {
    private static final ILog logger = LogManager.getLogger(ActionRequestBuilderInterceptor.class);

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {

        /*logger.debug("elasticsearch requestBuilder construct be invoke, class is {}", objInst.getClass().toString());

        if (allArguments.length < 2 || !(allArguments[1] instanceof EnhancedInstance)) {
            if (logger.isDebugEnable()) {
                logger.debug("elasticsearch requestBuilder action is not EnhanceInstance type.");
            }
            return;
        }
        String opType = allArguments[1].getClass().getSimpleName();
        EnhancedInstance enhancedInstance = (EnhancedInstance) allArguments[1];
        ElasticSearchEnhanceInfo enhanceInfo = (ElasticSearchEnhanceInfo) enhancedInstance.getSkyWalkingDynamicField();
        if (enhanceInfo == null) {
            if (logger.isDebugEnable()) {
                logger.debug("elasticsearch action {} is not be set enhanceInfo, will be skip.", opType);
            }
            return;
        }
        String operationName = ELASTICSEARCH_DB_OP_PREFIX + opType;
        AbstractSpan span = ContextManager.createExitSpan(operationName, enhanceInfo.transportAddresses());
        span.setComponent(ComponentsDefine.TRANSPORT_CLIENT);
        Tags.DB_TYPE.set(span, DB_TYPE);
        Tags.DB_INSTANCE.set(span, enhanceInfo.getClusterName());
        if (TRACE_DSL) {
            Tags.DB_STATEMENT.set(span, enhanceInfo.getSource());
        }
        span.tag(ES_INDEX, wrapperNullStringValue(enhanceInfo.getIndices()));
        span.tag(ES_TYPE, wrapperNullStringValue(enhanceInfo.getTypes()));
        SpanLayer.asDB(span);
        if (logger.isDebugEnable()) {
            logger.debug("create span {} success, spanId = {}.", operationName, span.getSpanId());
        }*/
    }
}
