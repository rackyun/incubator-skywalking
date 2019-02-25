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


package org.apache.skywalking.apm.plugin.spring.kafka.v1.match;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.agent.core.plugin.match.HierarchyMatch;
import org.apache.skywalking.apm.agent.core.plugin.match.IndirectMatch;
import org.apache.skywalking.apm.util.StringUtil;

import java.util.Arrays;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;

/**
 * @author yunhai.hu
 * at 2019/1/25
 */
public class HierarchyAndExcludeMatch implements IndirectMatch {

    private HierarchyMatch hierarchyMatch;
    private List<String> excludeTypes;

    private HierarchyAndExcludeMatch(String[] parentTypes, String[] excludeTypes) {
        hierarchyMatch = (HierarchyMatch) HierarchyMatch.byHierarchyMatch(parentTypes);
        if (excludeTypes == null || excludeTypes.length == 0) {
            throw new IllegalArgumentException("excludeTypes is null.");
        }
        this.excludeTypes = Arrays.asList(excludeTypes);
    }

    @Override
    public ElementMatcher.Junction buildJunction() {
        ElementMatcher.Junction junction = hierarchyMatch.buildJunction();
        for (String excludeTypeName : excludeTypes) {
            if (!StringUtil.isEmpty(excludeTypeName)) {
                junction.and(not(named(excludeTypeName)));
            }
        }
        return junction;
    }

    @Override
    public boolean isMatch(TypeDescription typeDescription) {
        return (!excludeTypes.contains(typeDescription.getTypeName())) && hierarchyMatch.isMatch(typeDescription);
    }

    public static ClassMatch byHierarchyAndExcludeMatch(String[] parentTypes, String[] excludeTypes) {
        return new HierarchyAndExcludeMatch(parentTypes, excludeTypes);
    }
}
