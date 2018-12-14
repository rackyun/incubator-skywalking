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


package org.apache.skywalking.apm.agent.core.context;

import com.google.common.collect.Lists;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.conf.RemoteDownstreamConfig;
import org.apache.skywalking.apm.agent.core.context.ids.DistributedTraceId;
import org.apache.skywalking.apm.agent.core.context.ids.GlobalIdGenerator;
import org.apache.skywalking.apm.agent.core.context.ids.ID;
import org.apache.skywalking.apm.agent.core.context.ids.PropagatedTraceId;
import org.apache.skywalking.apm.agent.core.dictionary.OperationNameDictionary;
import org.apache.skywalking.apm.agent.core.sampling.SamplingService;
import org.apache.skywalking.apm.util.HexUtil;


/**
 * @author wusheng
 */
public class XIdCarrierItem extends CarrierItem {
    public static final String HEADER_NAME = "x-request-id";
    public static final String DEVICE_NAME = "x-device-id";
    private static final String X_OPERATION_NAME_PREFIX = "Nginx";
    private ContextCarrier carrier;
    private XIdCarrierValue carrierValue;

    public XIdCarrierItem(ContextCarrier carrier, CarrierItem next, XIdCarrierValue xIdCarrierValue) {
        super(HEADER_NAME, carrier.serialize(), next);
        this.carrier = carrier;
        this.carrierValue = xIdCarrierValue;
    }

    @Override
    public void setHeadValue(String headValue) {
        long[] parts = HexUtil.stringToIDParts(headValue);
        ID id = new ID(parts[0], parts[1], parts[2]);
        carrier.setTraceSegmentId(GlobalIdGenerator.generate());
        carrier.setSpanId(0);
        carrier.setParentApplicationInstanceId(RemoteDownstreamConfig.Agent.APPLICATION_INSTANCE_ID);
        carrier.setEntryApplicationInstanceId(RemoteDownstreamConfig.Agent.APPLICATION_INSTANCE_ID);
        carrier.setPeerHost(carrierValue.peerHost);
        String operationName = X_OPERATION_NAME_PREFIX + carrierValue.url;
        carrier.setEntryOperationName(operationName);
        carrier.setParentOperationName(operationName);
        carrier.setDistributedTraceIds(Lists.newArrayList((DistributedTraceId) new PropagatedTraceId(id)));
        carrier.setSampled(ServiceManager.INSTANCE.findService(SamplingService.class).trySampling());
        OperationNameDictionary.INSTANCE.findOrPrepare4Register(RemoteDownstreamConfig.Agent.APPLICATION_ID,
                operationName, false, true);
    }

    public static class XIdCarrierValue {
        private String peerHost;
        private String url;
        private String deviceId;

        public XIdCarrierValue(String url, String peerHost, String deviceId) {
            this.peerHost = peerHost;
            this.url = url;
            this.deviceId = deviceId;
        }

        public String getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }

        public String getPeerHost() {
            return peerHost;
        }

        public void setPeerHost(String peerHost) {
            this.peerHost = peerHost;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
