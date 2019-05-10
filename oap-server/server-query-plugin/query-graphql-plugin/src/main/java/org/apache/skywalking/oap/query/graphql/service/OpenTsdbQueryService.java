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

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import org.apache.skywalking.apm.util.StringUtil;
import org.apache.skywalking.oap.query.graphql.entity.MetricRecord;
import org.apache.skywalking.oap.query.graphql.entity.MetricValues;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import org.apache.skywalking.oap.server.library.module.Service;
import org.apache.skywalking.oap.server.library.util.http.HttpClient;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author yunhai.hu
 * at 2019/4/28
 */
public class OpenTsdbQueryService implements Service {

    private static final Logger logger = LoggerFactory.getLogger(OpenTsdbQueryService.class);
    private final ModuleManager moduleManager;
    private String baseUrl;
    private Gson gson;

    public OpenTsdbQueryService(ModuleManager moduleManager, String baseUrl) {
        this.moduleManager = moduleManager;
        this.baseUrl = baseUrl;
        this.gson = new Gson();
    }

    public Map<String, MetricValues> queryMetric(long startTime, List<String> services) {
        String[] filteredServices = filterServices(services).toArray(new String[0]);

        Map<String, MetricValues> queryRet = new HashMap<>();
        int idx = 0;
        int step = 50;
        List<String> queryConditionJsonList = new ArrayList<>();
        while (idx < filteredServices.length - 1) {
            if (filteredServices.length - idx < step) {
                step = filteredServices.length - idx;
            }
            QueryCondition queryCondition = createQueryCondition(startTime,
                String.join("|", Arrays.copyOfRange(filteredServices, idx, idx + step)));
            queryConditionJsonList.add(gson.toJson(queryCondition));
            idx += step;
        }

        List<String> respContentList = HttpClient.multiJsonPost(baseUrl + "/api/query", null, queryConditionJsonList);
        for (String respContent : respContentList) {
            mapperResult(queryRet, respContent);
        }
        return queryRet;
    }

    private Map<String, Boolean> serviceNameCache = new HashMap<>();

    public List<String> filterServices(List<String> services) {
        List<String> notCachedServices = new ArrayList<>(services.size());
        List<String> queryServices = new ArrayList<>();
        for (String serviceName : services) {
            Boolean exist = serviceNameCache.get(serviceName);
            if (exist == null) {
                notCachedServices.add(serviceName);
            } else if (exist) {
                queryServices.add(serviceName);
            }
        }
        if (!notCachedServices.isEmpty()) {
            Map<String, Boolean> queryResult = checkServiceNameWithQuery(notCachedServices);
            serviceNameCache.putAll(queryResult);
            for (Map.Entry<String, Boolean> entry : queryResult.entrySet()) {
                if (entry.getValue()) {
                    queryServices.add(entry.getKey());
                }
            }
        }
        return queryServices;
    }

    private Map<String, Boolean> checkServiceNameWithQuery(List<String> services) {
        List<String> queryContents = new ArrayList<>();
        for (String serviceName : services) {
            Map<String, String> params = new HashMap<>();
            params.put("type", "tagv");
            params.put("q", serviceName);
            queryContents.add(gson.toJson(params));
        }
        Set<String> appSets = new HashSet<>();
        List<String> respContentList = HttpClient.multiJsonPost(baseUrl + "/api/suggest", null, queryContents);
        for (String respContent : respContentList) {
            String[] serviceNames = gson.fromJson(respContent, String[].class);
            if (serviceNames != null) {
                appSets.addAll(Arrays.asList(serviceNames));
            }
        }
        Map<String, Boolean> rets = new HashMap<>();
        for (String serviceName : services) {
            rets.put(serviceName, appSets.contains(serviceName));
        }
        return rets;
    }


    private void mapperResult(Map<String, MetricValues> queryRet, String respContent) {
        MetricRecord[] records = gson.fromJson(respContent, MetricRecord[].class);
        if (records != null) {
            for (MetricRecord record : records) {
                if (!StringUtil.isEmpty(record.getTags().getApp())) {
                    MetricValues metricValues = queryRet.get(record.getTags().getApp());
                    if (metricValues == null) {
                        metricValues = new MetricValues();
                        queryRet.put(record.getTags().getApp(), metricValues);
                    }
                    final MetricValues copy = metricValues;
                    if (record.getMetric().contains("error.counter")) {
                        record.getDps().forEach((k, v) -> copy.addErrorCpm(v));
                    } else if (record.getMetric().contains("server.timer.count")) {
                        record.getDps().forEach((k, v) -> copy.addSuccessCpm(v));
                    } else if (record.getMetric().contains("server.timer.mean")) {
                        record.getDps().forEach((k, v) -> copy.setResponseTime(v));
                    }
                }
            }
        }
    }

    @NotNull
    private QueryCondition createQueryCondition(long startTime, String servicesStr) {
        FilterEntry filterEntry = new FilterEntry();
        filterEntry.setFilter(servicesStr);
        filterEntry.setGroupBy(true);
        filterEntry.setTagk("app");
        filterEntry.setType("literal_or");
        List<FilterEntry> filterEntries = Lists.newArrayList(filterEntry);

        QueryCondition queryCondition = new QueryCondition();
        queryCondition.setStart(startTime);
        queryCondition.setEnd(startTime + 900000);
        queryCondition.setMsResolution(false);
        queryCondition.setGlobalAnnotations(true);
        queryCondition.setShowQuery(false);
        queryCondition.setQueries(Lists.newArrayList());

        for (String metric : metrics) {
            QueryEntry queryEntry = new QueryEntry();
            queryEntry.setMetric(metric);
            if (metric.endsWith("count")) {
                queryEntry.setRate(true);
                queryEntry.setAggregator("sum");
                RateOptions rateOptions = new RateOptions();
                rateOptions.setCounter(true);
                rateOptions.setResetValue(900000);
                rateOptions.setDropResets(true);
                queryEntry.setRateOptions(rateOptions);
            } else {
                queryEntry.setAggregator("avg");
            }
            queryEntry.setDownsample("15m-avg");
            queryEntry.setFilters(filterEntries);
            queryCondition.getQueries().add(queryEntry);
        }

        return queryCondition;
    }

    private String[] metrics = new String[]{"http.server.timer.count", "rpc.server.timer.count", "http.server.timer.mean", "rpc.server.timer.mean", "error.count"};

    @Getter
    @Setter
    private static class RateOptions {
        private boolean counter;
        private int resetValue;
        private boolean dropResets;
    }

    @Getter
    @Setter
    private static class FilterEntry {
        private String filter;
        private boolean groupBy;
        private String tagk;
        private String type;

    }

    @Getter
    @Setter
    private static class QueryEntry {
        private String metric;
        private String aggregator;
        private String downsample;
        private List<FilterEntry> filters;
        private boolean rate = false;
        private RateOptions rateOptions;

    }

    @Getter
    @Setter
    private static class QueryCondition {
        private long start;
        private long end;
        private List<QueryEntry> queries;
        private boolean msResolution;
        private boolean globalAnnotations;
        private boolean showQuery;

    }
}
