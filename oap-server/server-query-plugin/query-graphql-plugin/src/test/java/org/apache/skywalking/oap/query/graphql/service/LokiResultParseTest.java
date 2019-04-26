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

package org.apache.skywalking.oap.query.graphql.service;

import com.google.gson.Gson;
import org.apache.skywalking.oap.query.graphql.entity.LokiResult;
import org.apache.skywalking.oap.server.core.query.entity.LogRecord;
import org.junit.Test;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author yunhai.hu
 * at 2019/4/22
 */
public class LokiResultParseTest {

    @Test
    public void lokiResultParseTest() {
        Pattern labelsPattern = Pattern.compile("\\{hostname=\"((\\w+-?)+)\", appname=\"(\\w+-?)+\"\\}");
        String log = "{\"streams\":[{\"labels\":\"{hostname=\\\"bj-pre-04\\\", appname=\\\"account-rpc\\\"}\"," +
                "\"entries\":[{\"ts\":\"2019-04-22T03:12:08.714Z\",\"line\":\"DEBUG [DubboServerHandler-172.20.4" +
                ".250:20882-thread-176] c.k.u.a.r.p.UserRpcServiceProvider:106 rpc.auth success, " +
                "userId:5b6c15e19e098c49aa96e4c8, checkState:true, consumer:bj-pre-tc-03.ali.keep\\n\"}," +
                "{\"ts\":\"2019-04-22T03:01:53.641Z\",\"line\":\"DEBUG [DubboServerHandler-172.20.4" +
                ".250:20882-thread-146] c.k.u.a.r.p.UserRpcServiceProvider:106 rpc.auth success, " +
                "userId:5b6c15e19e098c49aa96e4c8, checkState:true, consumer:bj-pre-07.ali.keep\\n\"}]}," +
                "{\"labels\":\"{hostname=\\\"bj-pre-04\\\", appname=\\\"account-rpc\\\"}\"," +
                "\"entries\":[{\"ts\":\"2019-04-22T02:59:54.589Z\",\"line\":\"DEBUG [DubboServerHandler-172.20.4" +
                ".243:20882-thread-385] c.k.u.a.r.p.UserRpcServiceProvider:106 rpc.auth success, " +
                "userId:5b6c15e19e098c49aa96e4c8, checkState:true, consumer:bj-pre-03.ali.keep\\n\"}," +
                "{\"ts\":\"2019-04-22T02:59:51.487Z\",\"line\":\"DEBUG [DubboServerHandler-172.20.4" +
                ".243:20882-thread-385] c.k.u.a.r.p.UserRpcServiceProvider:106 rpc.auth success, " +
                "userId:5b6c15e19e098c49aa96e4c8, checkState:true, consumer:bj-pre-03.ali.keep\\n\"}]}]}";
        LokiResult lokiResult = new Gson().fromJson(log, LokiResult.class);
        List<LogRecord> logRecords = new ArrayList<>();
        for (LokiResult.Stream stream : lokiResult.getStreams()) {
            Matcher matcher = labelsPattern.matcher(stream.getLabels());
            String hostName = null;
            if (matcher.matches()) {
                hostName = matcher.group(1);
            }
            Assert.assertEquals("bj-pre-04", hostName);
            for (LokiResult.Entry entry : stream.getEntries()) {
                LogRecord logRecord = new LogRecord();
                logRecord.setHostname(hostName);
                logRecord.setTime(entry.getTs());
                logRecord.setMessage(entry.getLine());
                logRecords.add(logRecord);
            }
        }

        Assert.assertTrue(logRecords.size() > 0);

    }
}
