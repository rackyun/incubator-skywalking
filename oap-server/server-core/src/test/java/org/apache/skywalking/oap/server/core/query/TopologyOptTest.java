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

package org.apache.skywalking.oap.server.core.query;

import com.google.gson.Gson;
import org.apache.skywalking.oap.server.core.query.entity.Topology;
import org.junit.Test;

import java.io.FileReader;

import static org.junit.Assert.assertEquals;

/**
 * @author yunhai.hu
 * at 2019/4/24
 */
public class TopologyOptTest {


    @Test
    public void optimizeTest() {
        try (FileReader reader =
                     new FileReader(TopologyOptTest.class.getClassLoader().getResource("test.json").getFile())) {
            Gson gson = new Gson();
            Topology topology = gson.fromJson(reader, Topology.class);

            TopologyQueryService queryService = new TopologyQueryService(null);
            Topology newTopology = queryService.simplifyTopology(topology);
            assertEquals(119, newTopology.getNodes().size());
            assertEquals(696, newTopology.getCalls().size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
