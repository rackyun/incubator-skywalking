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
import org.apache.skywalking.oap.query.graphql.entity.MetricValues;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author yunhai.hu
 * at 2019/4/28
 */
public class OpenTsdbQueryServiceTest {

    @Test
    public void queryMetric() {

        OpenTsdbQueryService queryService = new OpenTsdbQueryService(null, "https://opentsdb.sre.gotokeep" +
                ".com/api/query");

        String[] services = new String[]{"sanda-queue-product", "hahaha", "suit-offline", "activity-api", "payaccount", "logistics", "homepage", "summary", "venom", "frog", "social-user-worker", "snooker-queue", "snooker-admin", "activity-job", "live-worker", "moffline", "diet", "ks-pallas-config", "social-scheduler", "glutton-oms", "antispam", "auth-api", "sanda", "ads", "sanda-queue-normal", "userprofile-worker", "personal-data", "tradecenter", "butler", "running", "alpha", "suit", "social-network-worker", "wharf-webapp", "authority", "kit-step", "feed-rpc", "explore", "athena", "glue", "pay", "social-live-worker", "social-network", "social-worker", "caesar-webapp", "trip-queue", "pandora", "hyrule", "live", "erp", "social-user-rpc", "mo-recommend", "shopcenter", "engine-rpc", "hook-queue", "course-rpc", "kspallas", "lark", "bootcamp-queue", "antispam-admin", "userprofile-rpc", "course-queue", "frog-queue", "social-user", "lego-worker", "klass", "feed", "kit-server-treadmill", "personal-data-queue", "bootcamp-offline", "hyrule-queue", "sanda-daemon", "lego-home", "bgsearch", "ares-rpc", "search", "snail", "caliper", "risk", "notify", "pumpkin-worker", "community-rpc", "search-worker", "zeus-backend", "lego", "kit-server-walkingpad", "social-network-rpc", "course-profile", "config-worker", "artemis-api", "skynet", "social", "dungeon", "store", "account-worker", "running-worker", "stockcenter", "messenger", "bootcamp", "snooker", "pumpkin", "openapi", "achievements", "course-admin", "whale-worker", "risk-worker", "account-rpc", "storemisc", "kitbit", "hermes", "account", "bridge-worker", "hyrule-job", "storejob", "snowball", "mofeeds", "diamond", "booth", "morec", "box", "outdoor-marketing", "feed-worker", "rec", "tof", "config", "trip-web", "course-webapp", "suit-queue"};

        List<String> checkResults = queryService.filterServices(Arrays.asList(services));

        Assert.assertEquals(services.length - 2, checkResults.size());

        Map<String, MetricValues> ret = queryService.queryMetric(System.currentTimeMillis() - 1000000,
                Lists.newArrayList(services));
        Assert.assertNotNull(ret);
    }
}