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

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;
import static org.apache.skywalking.apm.agent.core.plugin.bytebuddy.ArgumentTypeNameMatch.takesArgumentWithType;
import static org.apache.skywalking.apm.agent.core.plugin.match.NameMatch.byName;

/**
 * {@link MongoDBCollectionMonitorInstrumentation} define that the MongoDB Java Driver 2.13.x-2.14.x plugin intercepts the
 * following methods in the {@link com.mongodb.DBCollection}class:
 * 1. aggregate
 * 2. findAndModify
 * 3. getCount
 *
 * 4. drop
 * 5. dropIndexes
 * 6. rename
 * 7. group
 * 8. distinct
 * 9. mapReduce
 *
 * @author yunhai.hu
 */
public class MongoDBCollectionMonitorInstrumentation extends AbstractMongoDBInstrumentation {

    private static final String ENHANCE_CLASS = "com.mongodb.DBCollection";

    private static final String TRACE_INTERCET_CLASS = "org.apache.skywalking.apm.plugin.mongodb.v2.MongoDBCollectionMethodInterceptor";
    private static final String MONITOR_INTERCEPTOR =
            "org.apache.skywalking.apm.plugin.kmonitor.enhance.mongodb.v2.MongoDBCollectionMethodMonitorInterceptor";


    @Override
    protected ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[0];
    }

    @Override
    protected MultiInstanceMethodsInterceptPoint[] getMonitorInstanceMethodsInterceptPoints() {
        return generateMultiInterceptPoints(new String[]{TRACE_INTERCET_CLASS, MONITOR_INTERCEPTOR},
            named("aggregate").and(takesArgumentWithType(1, "com.mongodb.ReadPreference")),
            named("findAndModify").and(takesArguments(9)),
            named("getCount").and(takesArgumentWithType(6, "java.util.concurrent.TimeUnit")),
            ElementMatchers.<MethodDescription>named("drop"),
            ElementMatchers.<MethodDescription>named("dropIndexes"),
            named("rename").and(takesArgumentWithType(1, "boolean")),
            named("group").and(takesArguments(2).and(takesArgumentWithType(1, "com.mongodb.ReadPreference"))),
            named("group").and(takesArguments(1).and(takesArgumentWithType(0, "com.mongodb.DBObject"))),
            named("distinct").and(takesArgumentWithType(2, "com.mongodb.ReadPreference")),
            named("mapReduce").and(takesArgumentWithType(0, "com.mongodb.MapReduceCommand")),
            named("mapReduce").and(takesArgumentWithType(0, "com.mongodb.DBObject")),
            ElementMatchers.<MethodDescription>named("explainAggregate")
        );
    }

    @Override
    protected ClassMatch enhanceClass() {
        return byName(ENHANCE_CLASS);
    }

}
