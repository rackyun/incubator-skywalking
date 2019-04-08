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

package org.apache.skywalking.apm.plugin.elasticsearch.v6;

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchResponse;

import java.lang.reflect.Method;

import static org.apache.skywalking.apm.plugin.elasticsearch.v6.Constants.*;

/**
 * @author yunhai.hu
 * at 2019/1/16
 */
public class PlainActionFutureInterceptor implements InstanceMethodsAroundInterceptor {

    private static final ILog logger = LogManager.getLogger(PlainActionFutureInterceptor.class);

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        AbstractSpan span = ContextManager.createLocalSpan(ELASTICSEARCH_DB_OP_PREFIX + BASE_FUTURE_METHOD);
        span.setComponent(ComponentsDefine.TRANSPORT_CLIENT);
        Tags.DB_TYPE.set(span, DB_TYPE);
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                              Class<?>[] argumentsTypes, Object ret) throws Throwable {
        AbstractSpan span = ContextManager.activeSpan();
        if (logger.isDebugEnable()) {
            if (span == null) {
                logger.debug("after execute span is null.");
            } else {
                logger.debug("after execute spanId={}, operationName={}. ", span.getSpanId(), span.getOperationName());
            }
        }
        if (span == null || !span.getOperationName().contains(ELASTICSEARCH_DB_OP_PREFIX)) {
            return ret;
        }
        if (ret instanceof SearchResponse) {
            SearchResponse response = (SearchResponse) ret;
            span.tag(ES_TOOK_MILLIS, Long.toString(response.getTook().getMillis()));
            span.tag(ES_TOTAL_HITS, Long.toString(response.getHits().getTotalHits()));
        } else if (ret instanceof BulkResponse) {
            BulkResponse response = (BulkResponse) ret;
            span.tag(ES_TOOK_MILLIS, Long.toString(response.getTook().getMillis()));
            span.tag(ES_INGEST_TOOK_MILLIS, Long.toString(response.getIngestTookInMillis()));
        }
        ContextManager.stopSpan();
        if (logger.isDebugEnable()) {
            logger.debug("after execute span stopped.");
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
        ContextManager.activeSpan().errorOccurred().log(t);
    }
}
