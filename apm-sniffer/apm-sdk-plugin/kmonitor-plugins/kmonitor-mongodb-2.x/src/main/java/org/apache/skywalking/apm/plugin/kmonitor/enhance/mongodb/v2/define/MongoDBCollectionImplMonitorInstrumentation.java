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
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.MultiInstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.agent.core.plugin.match.NameMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

/**
 * {@link MongoDBCollectionImplMonitorInstrumentation} define that the MongoDB Java Driver 2.13.x-2.14.x plugin intercepts the
 * following methods in the com.mongodb.DBCollectionImpl class:
 * 1. find 
 * 2. insert 
 * 3. insertImpl 
 * 4. update 
 * 5. updateImpl 
 * 6. remove 
 * 7. createIndex 
 *
 * @author yunhai.hu
 */
public class MongoDBCollectionImplMonitorInstrumentation extends AbstractMongoDBInstrumentation {

    private static final String ENHANCE_CLASS = "com.mongodb.DBCollectionImpl";

    private static final String TRACE_INTERCET_CLASS = "org.apache.skywalking.apm.plugin.mongodb.v2.MongoDBCollectionMethodInterceptor";

    private static final String MONITOR_INTERCEPTOR_CLASS = "org.apache.skywalking.apm.plugin.kmonitor.enhance" +
            ".mongodb.v2.MongoDBCollectionMethodMonitorInterceptor";

    @Override
    protected ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[0];
    }

    @Override
    protected MultiInstanceMethodsInterceptPoint[] getMonitorInstanceMethodsInterceptPoints() {
        return generateMultiInterceptPoints(new String[]{TRACE_INTERCET_CLASS, MONITOR_INTERCEPTOR_CLASS},
            named("find").and(takesArguments(9)),
            named("insert").and(takesArguments(4)),
            ElementMatchers.<MethodDescription>named("insertImpl"),
            ElementMatchers.<MethodDescription>named("update"),
            ElementMatchers.<MethodDescription>named("updateImpl"),
            ElementMatchers.<MethodDescription>named("createIndex"),
            named("remove").and(takesArguments(4))
        );
    }

    @Override
    protected ClassMatch enhanceClass() {
        return NameMatch.byName(ENHANCE_CLASS);
    }

}
