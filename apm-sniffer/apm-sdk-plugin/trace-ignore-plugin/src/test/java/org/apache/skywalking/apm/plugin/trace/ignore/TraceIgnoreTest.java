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

package org.apache.skywalking.apm.plugin.trace.ignore;

import java.util.Properties;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.context.*;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.plugin.trace.ignore.conf.IgnoreConfig;
import org.apache.skywalking.apm.util.*;
import org.junit.*;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author liujc [liujunc1993@163.com]
 */
public class TraceIgnoreTest {

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables().set("SW_AGENT_TRACE_IGNORE_PATH", "path_test");

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();

    @Test
    public void testServiceOverrideFromPlugin() {
        ContextManagerExtendService service = ServiceManager.INSTANCE.findService(ContextManagerExtendService.class);
        Assert.assertEquals(TraceIgnoreExtendService.class, service.getClass());
    }

    @Test
    public void testTraceIgnore() {
        ContextManagerExtendService service = ServiceManager.INSTANCE.findService(ContextManagerExtendService.class);
        IgnoreConfig.Trace.IGNORE_PATH = "/eureka/**,Kafka/zipkin2/*,/health,*/health";
        AbstractTracerContext ignoredTracerContext = service.createTraceContext("/eureka/apps", false);
        Assert.assertEquals(SkipedTracerContext.class, ignoredTracerContext.getClass());

        AbstractTracerContext traceContext = service.createTraceContext("/consul/apps", false);
        Assert.assertEquals(TracingContext.class, traceContext.getClass());

        AbstractTracerContext zipkin2Context = service.createTraceContext("Kafka/zipkin2/Producer", false);
        Assert.assertEquals(SkipedTracerContext.class, zipkin2Context.getClass());

        AbstractTracerContext healthContext = service.createTraceContext(OperationNameUtil.normalizeUrl("/health"), false);
        Assert.assertEquals(SkipedTracerContext.class, healthContext.getClass());

        AbstractTracerContext mvcHealthContext = service.createTraceContext(OperationNameUtil.normalizeUrl("MVC/health"), false);
        Assert.assertEquals(SkipedTracerContext.class, mvcHealthContext.getClass());

        AbstractTracerContext nginxHealthContext = service.createTraceContext(OperationNameUtil.normalizeUrl("Nginx/health"), false);
        Assert.assertEquals(SkipedTracerContext.class, nginxHealthContext.getClass());
    }

    @Test
    public void testTraceIgnoreConfigOverridingFromSystemEnv() throws IllegalAccessException {
        Properties properties = new Properties();
        properties.put("trace.ignore_path", "${SW_AGENT_TRACE_IGNORE_PATH:/path/eureka/**}");
        properties.put("trace.ignore_path", PropertyPlaceholderHelper.INSTANCE.replacePlaceholders((String)properties.get("trace.ignore_path"), properties));
        ConfigInitializer.initialize(properties, IgnoreConfig.class);
        assertThat(IgnoreConfig.Trace.IGNORE_PATH, is("path_test"));
    }
}
