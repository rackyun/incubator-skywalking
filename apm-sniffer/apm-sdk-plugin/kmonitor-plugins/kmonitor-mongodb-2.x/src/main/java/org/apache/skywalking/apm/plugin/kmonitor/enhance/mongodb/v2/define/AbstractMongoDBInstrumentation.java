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


package org.apache.skywalking.apm.plugin.kmonitor.enhance.mongodb.v2.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.MultiInstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.plugin.kmonitor.common.KmonitorClassEnhancePluginDefine;

/**
 * @author yunhai.hu
 * at 2019/5/10
 */
public abstract class AbstractMongoDBInstrumentation extends KmonitorClassEnhancePluginDefine {


    protected MultiInstanceMethodsInterceptPoint[] generateMultiInterceptPoints(final String[] interceptors,
                                                                           final ElementMatcher<MethodDescription>... methodsMatcher) {
        MultiInstanceMethodsInterceptPoint[] interceptPoints =
                new MultiInstanceMethodsInterceptPoint[methodsMatcher.length];
        for (int i = 0; i < methodsMatcher.length; i++) {
            final ElementMatcher<MethodDescription> elementMatcher = methodsMatcher[i];
            interceptPoints[i] = new MultiInstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return elementMatcher;
                }

                @Override
                public String[] getMethodsInterceptors() {
                    return interceptors;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            };
        }
        return interceptPoints;
    }

    @Override
    protected String[] witnessClasses() {
        /**
         * @see {@link com.mongodb.tools.ConnectionPoolStat}
         */
        return new String[]{"com.mongodb.tools.ConnectionPoolStat"};
    }
}
