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

import java.io.IOException;
import java.util.*;

import org.apache.skywalking.apm.util.StringUtil;
import org.apache.skywalking.oap.server.core.*;
import org.apache.skywalking.oap.server.core.cache.EndpointInventoryCache;
import org.apache.skywalking.oap.server.core.config.IComponentLibraryCatalogService;
import org.apache.skywalking.oap.server.core.query.entity.*;
import org.apache.skywalking.oap.server.core.source.DetectPoint;
import org.apache.skywalking.oap.server.core.storage.StorageModule;
import org.apache.skywalking.oap.server.core.storage.query.*;
import org.apache.skywalking.oap.server.library.module.Service;
import org.apache.skywalking.oap.server.library.module.*;
import org.apache.skywalking.oap.server.library.util.CollectionUtils;
import org.elasticsearch.common.Strings;
import org.slf4j.*;

/**
 * @author peng-yongsheng
 */
public class TopologyQueryService implements Service {

    private static final Logger logger = LoggerFactory.getLogger(TopologyQueryService.class);

    private final ModuleManager moduleManager;
    private ITopologyQueryDAO topologyQueryDAO;
    private IMetadataQueryDAO metadataQueryDAO;
    private EndpointInventoryCache endpointInventoryCache;
    private IComponentLibraryCatalogService componentLibraryCatalogService;
    private Map<String, Node> conjectureNodes;

    public TopologyQueryService(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
        conjectureNodes = initConjectureNodes();
    }

    private IMetadataQueryDAO getMetadataQueryDAO() {
        if (metadataQueryDAO == null) {
            metadataQueryDAO = moduleManager.find(StorageModule.NAME).provider().getService(IMetadataQueryDAO.class);
        }
        return metadataQueryDAO;
    }

    private ITopologyQueryDAO getTopologyQueryDAO() {
        if (topologyQueryDAO == null) {
            topologyQueryDAO = moduleManager.find(StorageModule.NAME).provider().getService(ITopologyQueryDAO.class);
        }
        return topologyQueryDAO;
    }

    private IComponentLibraryCatalogService getComponentLibraryCatalogService() {
        if (componentLibraryCatalogService == null) {
            componentLibraryCatalogService = moduleManager.find(CoreModule.NAME).provider().getService(IComponentLibraryCatalogService.class);
        }
        return componentLibraryCatalogService;
    }

    private EndpointInventoryCache getEndpointInventoryCache() {
        if (endpointInventoryCache == null) {
            endpointInventoryCache = moduleManager.find(CoreModule.NAME).provider().getService(EndpointInventoryCache.class);
        }
        return endpointInventoryCache;
    }

    public Topology getGlobalTopology(final Step step, final long startTB, final long endTB, final long startTimestamp,
        final long endTimestamp) throws IOException {
        logger.debug("step: {}, startTimeBucket: {}, endTimeBucket: {}", step, startTB, endTB);
        List<Call> serviceRelationServerCalls = getTopologyQueryDAO().loadServerSideServiceRelations(step, startTB, endTB);
        List<Call> serviceRelationClientCalls = getTopologyQueryDAO().loadClientSideServiceRelations(step, startTB, endTB);

        List<org.apache.skywalking.oap.server.core.query.entity.Service> serviceList = getMetadataQueryDAO().searchServices(startTimestamp, endTimestamp, Const.EMPTY_STRING);

        TopologyBuilder builder = new TopologyBuilder(moduleManager);
        Topology topology = builder.build(serviceRelationClientCalls, serviceRelationServerCalls);

        serviceList.forEach(service -> {
            boolean contains = false;
            for (Node node : topology.getNodes()) {
                if (service.getId() == node.getId()) {
                    contains = true;
                    break;
                }
            }

            if (!contains) {
                Node newNode = new Node();
                newNode.setId(service.getId());
                newNode.setName(service.getName());
                newNode.setReal(true);
                newNode.setType(Const.UNKNOWN);
                topology.getNodes().add(newNode);
            }
        });

        return simplifyNginxTopology(topology);
    }

    public Topology getServiceTopology(final Step step, final long startTB, final long endTB,
        final int serviceId) throws IOException {
        List<Integer> serviceIds = new ArrayList<>();
        serviceIds.add(serviceId);

        List<Call> serviceRelationClientCalls = getTopologyQueryDAO().loadSpecifiedClientSideServiceRelations(step, startTB, endTB, serviceIds);
        List<Call> serviceRelationServerCalls = getTopologyQueryDAO().loadSpecifiedServerSideServiceRelations(step, startTB, endTB, serviceIds);

        TopologyBuilder builder = new TopologyBuilder(moduleManager);
        Topology topology = builder.build(serviceRelationClientCalls, serviceRelationServerCalls);

        List<Integer> sourceServiceIds = new ArrayList<>();
        serviceRelationClientCalls.forEach(call -> sourceServiceIds.add(call.getSource()));
        if (CollectionUtils.isNotEmpty(sourceServiceIds)) {
            List<Call> sourceCalls = getTopologyQueryDAO().loadSpecifiedServerSideServiceRelations(step, startTB, endTB, sourceServiceIds);
            topology.getNodes().forEach(node -> {
                if (Strings.isNullOrEmpty(node.getType())) {
                    for (Call call : sourceCalls) {
                        if (node.getId() == call.getTarget()) {
                            node.setType(getComponentLibraryCatalogService().getComponentName(call.getComponentId()));
                            break;
                        }
                    }
                }
            });
        }

        return topology;
    }

    public Topology getEndpointTopology(final Step step, final long startTB, final long endTB,
        final int endpointId) throws IOException {
        List<Call> serverSideCalls = getTopologyQueryDAO().loadSpecifiedDestOfServerSideEndpointRelations(step, startTB, endTB, endpointId);
        serverSideCalls.forEach(call -> call.setDetectPoint(DetectPoint.SERVER));

        serverSideCalls.forEach(call -> call.setCallType(Const.EMPTY_STRING));

        Topology topology = new Topology();
        topology.getCalls().addAll(serverSideCalls);

        Set<Integer> nodeIds = new HashSet<>();
        serverSideCalls.forEach(call -> {
            if (!nodeIds.contains(call.getSource())) {
                topology.getNodes().add(buildEndpointNode(call.getSource()));
                nodeIds.add(call.getSource());
            }
            if (!nodeIds.contains(call.getTarget())) {
                topology.getNodes().add(buildEndpointNode(call.getTarget()));
                nodeIds.add(call.getTarget());
            }
        });

        return topology;
    }

    private Node buildEndpointNode(int endpointId) {
        Node node = new Node();
        node.setId(endpointId);
        node.setName(getEndpointInventoryCache().get(endpointId).getName());
        node.setType(Const.EMPTY_STRING);
        node.setReal(true);
        return node;
    }

    public Topology simplifyNginxTopology(Topology topology) {

        List<Call> simplifiedCalls = new ArrayList<>();
        topology.getCalls().forEach(call -> {
            String[] ids = call.getId().split(Const.ID_SPLIT);
            if (ids[0].equals(ids[1])) {
                return;
            }
            simplifiedCalls.add(call);
        });

        return new Topology(topology.getNodes(), simplifiedCalls);
    }

    public Topology simplifyTopology(Topology topology) {
        Map<Integer, Node> idNodeMap = new HashMap<>();
        List<Node> simplifiedNodes = new ArrayList<>();
        topology.getNodes().forEach(node -> {
            if (StringUtil.isEmpty(node.getType())) {
                simplifiedNodes.add(node);
                return;
            }
            Node conNode = conjectureNodes.get(node.getType());
            if (conNode != null) {
                idNodeMap.put(node.getId(), conNode);
            } else {
                simplifiedNodes.add(node);
            }
        });

        simplifiedNodes.addAll(conjectureNodes.values());

        List<Call> simplifiedCalls = new ArrayList<>();
        topology.getCalls().forEach(call -> {
            String[] ids = call.getId().split(Const.ID_SPLIT);
            if (ids[0].equals(ids[1])) {
                return;
            }
            if (DetectPoint.CLIENT.equals(call.getDetectPoint())) {
                Node compNode = idNodeMap.get(call.getTarget());
                if (compNode != null) {
                    call.setTarget(compNode.getId());
                    call.setId(call.getSource() + Const.ID_SPLIT + call.getTarget() + Const.ID_SPLIT + ids[2]);
                }
            }
            simplifiedCalls.add(call);
        });

        return new Topology(simplifiedNodes, simplifiedCalls);
    }

    private Map<String, Node> initConjectureNodes() {
        Map<String, Node> conjectureNodes = new HashMap<>();
        conjectureNodes.put("Redis", buildConjectureNode(10001, "Redis"));
        conjectureNodes.put("Mysql", buildConjectureNode(10002, "Mysql"));
        conjectureNodes.put("MongoDB", buildConjectureNode(10003, "MongoDB"));
        conjectureNodes.put("hbase", buildConjectureNode(10004, "hbase"));
        conjectureNodes.put("Elasticsearch", buildConjectureNode(10005, "Elasticsearch"));
        conjectureNodes.put("Kafka", buildConjectureNode(10006, "Kafka"));
        conjectureNodes.put("RocketMQ", buildConjectureNode(10007, "RocketMQ"));
        return Collections.unmodifiableMap(conjectureNodes);
    }

    private Node buildConjectureNode(int id, String type) {
        Node node = new Node();
        node.setId(id);
        node.setType(type);
        node.setReal(false);
        node.setName(type);
        return node;
    }
}
