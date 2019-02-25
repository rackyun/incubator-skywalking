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

import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.conf.RemoteDownstreamConfig;
import org.apache.skywalking.apm.agent.core.context.ids.DistributedTraceId;
import org.apache.skywalking.apm.agent.core.context.ids.ID;
import org.apache.skywalking.apm.agent.core.context.ids.PropagatedTraceId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ContextCarrierV1HeaderTest {

    @Before
    public void setup() {
        Config.Agent.ACTIVE_V1_HEADER = true;
        Config.Agent.ACTIVE_V2_HEADER = false;
    }

    @Test
    public void testCompatibleHeaderKeys() {

        ContextCarrier contextCarrier = new ContextCarrier();
        CarrierItem next = contextCarrier.items();
        boolean hasSW3 = false;
        boolean hasSW6 = false;
        while (next.hasNext()) {
            next = next.next();
            if (next.getHeadKey().equals("sw3")) {
                hasSW3 = true;
            } else if (next.getHeadKey().equals("sw6")) {
                hasSW6 = true;
            } else {
                Assert.fail("unexpected key");
            }
        }
        Assert.assertTrue(hasSW3);
        Assert.assertFalse(hasSW6);
    }

    @Test
    public void testDeserializeV1Header() {
        ContextCarrier contextCarrier = new ContextCarrier();
        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            if (next.getHeadKey().equals("sw3")) {
                next.setHeadValue("00023c0b000000db0036f6ee36cb5500|1|146443|146443|10|#Nginx/howAreYou|#Hystrix/GET /hello?cc={cc}/Execution|dc00113e23768e05329573caadb296a1|true");
            } else {
                Assert.fail("unexpected key");
            }
        }

        Assert.assertTrue(contextCarrier.isValid());
    }

    @Test
    public void testSerializeV1Header() {
        List<DistributedTraceId> distributedTraceIds = new ArrayList<DistributedTraceId>();
        distributedTraceIds.add(new PropagatedTraceId("dc00113e23768e05ca9573caadb296a0"));

        ContextCarrier contextCarrier = new ContextCarrier();
        contextCarrier.setTraceSegmentId(new ID(2513, 51, 15470347039300768L));
        contextCarrier.setDistributedTraceIds(distributedTraceIds);
        contextCarrier.setSpanId(2);
        contextCarrier.setEntryServiceInstanceId(2513);
        contextCarrier.setParentServiceInstanceId(2513);
        contextCarrier.setPeerId(62);
        contextCarrier.setEntryEndpointName("/portal");
        contextCarrier.setParentEndpointName("/portal");
        contextCarrier.setSampled(false);

        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            if (next.getHeadKey().equals("sw3")) {
                Assert.assertEquals("000009d1000000330036f632dbd45ca0|2|2513|2513|62|#/portal|#/portal|dc00113e23768e05ca9573caadb296a0|false", next.getHeadValue());
            } else {
                Assert.fail("unexpected key");
            }
        }

        Assert.assertTrue(contextCarrier.isValid());
    }

    @Test
    public void testXidCarrierItem() {
        ContextCarrier contextCarrier = new ContextCarrier();
        XIdCarrierItem.XIdCarrierValue carrierValue = new XIdCarrierItem.XIdCarrierValue("/test", "localhost", "");
        XIdCarrierItem xIdCarrierItem = new XIdCarrierItem(contextCarrier, null, carrierValue);
        String xRequestId = "dc00113e23768e05329573caadb296a0";
        RemoteDownstreamConfig.Agent.SERVICE_INSTANCE_ID = 1;
        xIdCarrierItem.setHeadValue(xRequestId);

    }

}
