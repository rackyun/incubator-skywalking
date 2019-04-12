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


package org.apache.skywalking.oap.server.storage.plugin.elasticsearch.query;

import org.apache.skywalking.oap.server.core.query.entity.LogRecord;
import org.apache.skywalking.oap.server.core.storage.query.ILogQueryDAO;
import org.apache.skywalking.oap.server.library.client.elasticsearch.ElasticSearchClient;
import org.apache.skywalking.oap.server.storage.plugin.elasticsearch.base.EsDAO;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yunhai.hu
 * at 2019/4/9
 */
public class LogQueryEsDAO extends EsDAO implements ILogQueryDAO {

    private LogRecord.Builder builder;

    public LogQueryEsDAO(ElasticSearchClient elasticSearchClient) {
        super(elasticSearchClient);
        builder = new LogRecord.Builder();
    }


    @Override
    public List<LogRecord> queryLog(String traceId, String date) throws IOException {
        SearchSourceBuilder sourceBuilder = SearchSourceBuilder.searchSource();
        sourceBuilder.query(QueryBuilders.termQuery(LogRecord.TRACE_ID_ID, traceId));
        sourceBuilder.size(50);

        SearchResponse response = getClient().search(LogRecord.getIndex(date), LogRecord.getType(), sourceBuilder);

        List<LogRecord> logRecords = new ArrayList<>();
        for (SearchHit searchHit : response.getHits().getHits()) {
            LogRecord logRecord = builder.map2Data(searchHit.getSourceAsMap());
            logRecords.add(logRecord);
        }

        return logRecords;
    }
}
