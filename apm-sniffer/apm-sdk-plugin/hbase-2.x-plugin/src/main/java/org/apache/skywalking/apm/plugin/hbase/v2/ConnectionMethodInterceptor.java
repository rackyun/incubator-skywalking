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

package org.apache.skywalking.apm.plugin.hbase.v2;

import org.apache.hadoop.hbase.TableName;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.util.StringUtil;

import java.lang.reflect.Method;

/**
 * @author yunhai.hu
 * at 2019/3/20
 */
public class ConnectionMethodInterceptor implements InstanceMethodsAroundInterceptor {

    private static final ILog logger = LogManager.getLogger(ConnectionMethodInterceptor.class);

    private static final String SKIP_HOST = "hbase:meta";


    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                             Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {

        TableName tableName = (TableName) allArguments[0];
        String tableNameStr = tableName.getNameAsString();
        objInst.enSetSkyWalkingDynamicField(tableNameStr);
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                              Class<?>[] argumentsTypes, Object ret) throws Throwable {
        String tableNameStr = (String) objInst.getSkyWalkingDynamicField();
        if (!isSkip(tableNameStr) && (ret instanceof EnhancedInstance)) {
            EnhancedInstance hbaseInstance = (EnhancedInstance) ret;
            HbaseEnhanceRequiredInfo requiredInfo = new HbaseEnhanceRequiredInfo();
            requiredInfo.setTableName(tableNameStr);
            hbaseInstance.enSetSkyWalkingDynamicField(requiredInfo);
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
    }


    private boolean isSkip(String tableName) {
        return StringUtil.isEmpty(tableName) || SKIP_HOST.equals(tableName);
    }
}
