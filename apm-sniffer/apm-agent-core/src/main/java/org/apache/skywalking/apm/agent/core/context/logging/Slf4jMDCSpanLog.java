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

package org.apache.skywalking.apm.agent.core.context.logging;

import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.slf4j.MDC;

/**
 * @author yunhai.hu
 * at 2018/12/14
 */
public class Slf4jMDCSpanLog implements SpanLog {

    private static final String TRACE_ID_NAME = "traceid";
    private static final String SPAN_ID_NAME = "span";

    @Override
    public void logStartedSpan(String globalTraceId, AbstractSpan span) {
        if (globalTraceId != null && globalTraceId.length() > 0 && span != null) {
            MDC.put(TRACE_ID_NAME, globalTraceId);
            MDC.put(SPAN_ID_NAME, String.valueOf(span.getSpanId()));
        }
    }

    @Override
    public void logStoppedSpan(String globalTraceId, AbstractSpan stoppedSpan, AbstractSpan parentSpan) {
        if (parentSpan != null) {
            MDC.put(SPAN_ID_NAME, String.valueOf(parentSpan.getSpanId()));
        } else {
            MDC.remove(TRACE_ID_NAME);
            MDC.remove(TRACE_ID_NAME);
        }
    }
}
