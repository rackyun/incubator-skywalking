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

package org.apache.skywalking.oap.query.graphql.resolver;

import com.coxautodev.graphql.tools.GraphQLQueryResolver;
import org.apache.skywalking.oap.server.core.CoreModule;
import org.apache.skywalking.oap.query.graphql.service.LogQueryService;
import org.apache.skywalking.oap.server.core.query.TraceQueryService;
import org.apache.skywalking.oap.server.core.query.entity.LogBrief;
import org.apache.skywalking.oap.server.core.query.entity.Span;
import org.apache.skywalking.oap.server.core.query.entity.Trace;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author yunhai.hu
 * at 2019/4/9
 */
public class LogQuery implements GraphQLQueryResolver {

    private static final Logger logger = LoggerFactory.getLogger(LogQuery.class);
    private static final String DATE_FORMAT = "yyyy.MM.dd";
    private final ModuleManager moduleManager;
    private LogQueryService logQueryService;
    private TraceQueryService traceQueryService;

    public LogQuery(ModuleManager moduleManager, String logUrl) {
        this.moduleManager = moduleManager;
        this.logQueryService = new LogQueryService(moduleManager, logUrl);
    }

    private LogQueryService getLogQueryService() {
        return logQueryService;
    }

    private TraceQueryService getTraceQueryService() {
        if (traceQueryService == null) {
            this.traceQueryService = moduleManager.find(CoreModule.NAME).provider().getService(TraceQueryService.class);
        }
        return traceQueryService;
    }

    public LogBrief queryLog(final String traceId) throws IOException {
        logger.debug("log query request, traceId {}", traceId);
        Trace trace = getTraceQueryService().queryTrace(traceId);
        logger.debug("query trace result {}", trace);
        LogBrief logBrief = new LogBrief();
        if (trace == null) {
            logBrief.setLogRecords(Collections.emptyList());
            return logBrief;
        }

        Set<String> services = new HashSet<>();
        for (Span span : trace.getSpans()) {
            services.add(span.getServiceCode());
        }
        long startTime = trace.getSpans().get(0).getStartTime();
//        String date = DateFormatUtils.format(trace.getSpans().get(0).getStartTime(), DATE_FORMAT);
        logBrief.setLogRecords(getLogQueryService().queryLog(traceId, startTime, services));
        logger.debug("log query result {}", logBrief.getLogRecords());
        return logBrief;
    }
}
