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

import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.ClusterConnection;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

import java.lang.reflect.Method;

/**
 * @author yunhai.hu
 * at 2019/3/20
 */
public class HbaseMethodInterceptor implements InstanceMethodsAroundInterceptor, InstanceConstructorInterceptor {

    private static final String DB_TYPE = "HBaseDB";

    private static final String HBASE_OP_PREFIX = "Hbase/";

    private static final String HBASE_HOST_PREFIX = "hbase:";

    private static final String SKIP_HOST = "hbase:meta";


    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                             Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {

        String executeMethod = method.getName();
        String tableName = ((HbaseEnhanceRequiredInfo) objInst.getSkyWalkingDynamicField()).getTableName();
        if (isSkip(tableName)) {
            return;
        }
        AbstractSpan span = ContextManager.createExitSpan(HBASE_OP_PREFIX + executeMethod, new ContextCarrier(),
                HBASE_HOST_PREFIX + tableName);
        span.setComponent(ComponentsDefine.HBASE);
        Tags.DB_TYPE.set(span, DB_TYPE);
        SpanLayer.asDB(span);

    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                              Class<?>[] argumentsTypes, Object ret) throws Throwable {
        HbaseEnhanceRequiredInfo requiredInfo = (HbaseEnhanceRequiredInfo) objInst.getSkyWalkingDynamicField();
        if (requiredInfo == null || !isSkip(requiredInfo.getTableName())) {
            ContextManager.stopSpan();
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
        ContextManager.activeSpan().errorOccurred().log(t);
    }


    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {
        TableName tableName = (TableName) allArguments[0];
        HbaseEnhanceRequiredInfo requiredInfo = new HbaseEnhanceRequiredInfo();
        if (tableName != null) {
            String tableNameStr = tableName.getNameAsString();
            requiredInfo.setTableName(tableNameStr);
        }
        ClusterConnection connection = (ClusterConnection) allArguments[1];
        if (connection != null) {
            String zkAdr = connection.getConfiguration().get(HConstants.ZOOKEEPER_QUORUM);
            requiredInfo.setZkAddr(zkAdr);
        }
        objInst.enSetSkyWalkingDynamicField(requiredInfo);
    }

    private boolean isSkip(String tableName) {
        return SKIP_HOST.equals(tableName);
    }
}
