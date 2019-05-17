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

package org.apache.skywalking.apm.plugin.kmonitor.enhance.elasticsearch.v5.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.MultiInstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.plugin.kmonitor.common.KmonitorClassEnhancePluginDefine;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.apache.skywalking.apm.agent.core.plugin.match.NameMatch.byName;

/**
 * @author yunhai.hu
 */
public class TransportActionNodeProxyInstrumentation extends KmonitorClassEnhancePluginDefine {

    private static final String TRACE_INTERCEPTOR_CLASS = "org.apache.skywalking.apm.plugin.elasticsearch.v5.TransportActionNodeProxyInterceptor";
    private static final String ENHANCE_CLASS = "org.elasticsearch.action.TransportActionNodeProxy";
    private static final String MONITOR_INTERCEPTOR_CLASS = "org.apache.skywalking.apm.plugin.kmonitor.enhance.elasticsearch.v5.ElasticsearchMonitorInterceptor";

    @Override
    protected ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[0];
    }

    @Override
    protected MultiInstanceMethodsInterceptPoint[] getMonitorInstanceMethodsInterceptPoints() {
        return new MultiInstanceMethodsInterceptPoint[] {
            new MultiInstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named("execute");
                }

                @Override
                public String[] getMethodsInterceptors() {
                    return new String[] {TRACE_INTERCEPTOR_CLASS, MONITOR_INTERCEPTOR_CLASS};
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            }
        };
    }

    @Override
    protected ClassMatch enhanceClass() {
        return byName(ENHANCE_CLASS);
    }

    @Override
    protected String[] witnessClasses() {
        return new String[] {"org.elasticsearch.common.transport.TransportAddressSerializers"};
    }
}
