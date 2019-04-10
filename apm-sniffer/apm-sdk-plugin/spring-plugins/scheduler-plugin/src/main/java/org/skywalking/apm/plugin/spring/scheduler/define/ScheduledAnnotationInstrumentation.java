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


package org.skywalking.apm.plugin.spring.scheduler.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.agent.core.plugin.match.MethodAnnotationMatch;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * @author yunhai.hu
 * at 2019/4/8
 */
public class ScheduledAnnotationInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    private static final String ENHANCE_INTERCEPTOR = "org.skywalking.apm.plugin.spring.scheduler" +
            ".ScheduledAnnotationInterceptor";
    private static final String ENHANCE_ANNOTATION = "org.springframework.scheduling.annotation.Scheduled";

    @Override
    protected ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[0];
    }

    @Override
    protected InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[]{
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return isAnnotatedWith(named(ENHANCE_ANNOTATION));
                }

                @Override
                public String getMethodsInterceptor() {
                    return ENHANCE_INTERCEPTOR;
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
        return MethodAnnotationMatch.byMethodAnnotationMatch(getEnhanceAnnotations());
    }

    private String[] getEnhanceAnnotations() {
        return new String[]{ENHANCE_ANNOTATION};
    }
}
