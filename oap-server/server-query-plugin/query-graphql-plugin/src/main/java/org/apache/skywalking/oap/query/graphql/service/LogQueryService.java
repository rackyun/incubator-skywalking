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


package org.apache.skywalking.oap.query.graphql.service;

import com.google.gson.Gson;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.skywalking.apm.util.StringUtil;
import org.apache.skywalking.oap.query.graphql.entity.LokiResult;
import org.apache.skywalking.oap.server.core.query.entity.LogRecord;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import org.apache.skywalking.oap.server.library.module.Service;
import org.apache.skywalking.oap.server.library.util.http.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author yunhai.hu
 * at 2019/4/9
 */
public class LogQueryService implements Service {

    private static final Logger logger = LoggerFactory.getLogger(LogQueryService.class);
    private final ModuleManager moduleManager;
    private String baseUrl;
    private Gson gson;
    private Pattern labelsPattern = Pattern.compile("\\{.*hostname=\"((\\w+-?)+)\".*");

    public LogQueryService(ModuleManager moduleManager, String baseUrl) {
        this.moduleManager = moduleManager;
        this.baseUrl = baseUrl;
        this.gson = new Gson();
    }


    public List<LogRecord> queryLog(String traceId, long startTime, Set<String> services) {
        List<LogRecord> logRecordList = new ArrayList<>();
        //query: a logQL query
        //limit: max number of entries to return
        //start: the start time for the query, as a nanosecond Unix epoch (nanoseconds since 1970)
        //end: the end time for the query, as a nanosecond Unix epoch (nanoseconds since 1970)
        //direction: forward or backward, useful when specifying a limit
        //regexp: a regex to filter the returned results, will eventually be rolled into the query language
        List<NameValuePair> baseParams = new ArrayList<>();
        baseParams.add(new BasicNameValuePair("limit", "100"));
        baseParams.add(new BasicNameValuePair("start", String.valueOf(startTime - 120 * 1000) + "000000"));
        baseParams.add(new BasicNameValuePair("end", String.valueOf(startTime + 120 * 1000) + "000000"));
        baseParams.add(new BasicNameValuePair("direction", "forward"));
        baseParams.add(new BasicNameValuePair("regexp", traceId));
        services.parallelStream().forEach(serviceCode -> {
            List<NameValuePair> requestParams = new ArrayList<>(baseParams.size() + 1);
            requestParams.addAll(baseParams);
            requestParams.add(new BasicNameValuePair("query", "{appname=\"" + serviceCode + "\"}"));

            String result = null;
            try {
                result = HttpClient.get(baseUrl, requestParams);
            } catch (IOException e) {
                logger.error("log query " + baseUrl + " error", e);
            }
            if (!StringUtil.isEmpty(result)) {
                mapper(logRecordList, traceId, serviceCode, result);
            }
        });
        return logRecordList;
    }

    private void mapper(List<LogRecord> logRecordList, String traceId, String serviceCode, String result) {
        LokiResult lokiResult = gson.fromJson(result, LokiResult.class);
        if (lokiResult == null || lokiResult.getStreams() == null) {
            return;
        }
        for (LokiResult.Stream stream : lokiResult.getStreams()) {
            Matcher matcher = labelsPattern.matcher(stream.getLabels());
            String hostName = null;
            if (matcher.matches()) {
                hostName = matcher.group(1);
            }
            for (LokiResult.Entry entry : stream.getEntries()) {
                LogRecord logRecord = new LogRecord();
                logRecord.setTraceId(traceId);
                logRecord.setHostname(hostName);
                logRecord.setAppname(serviceCode);
                logRecord.setTime(entry.getTs());
                logRecord.setMessage(entry.getLine());
                logRecordList.add(logRecord);
            }
        }
    }
}
