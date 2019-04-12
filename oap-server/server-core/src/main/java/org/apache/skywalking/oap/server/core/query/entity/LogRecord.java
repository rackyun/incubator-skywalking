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


package org.apache.skywalking.oap.server.core.query.entity;

import lombok.Getter;
import lombok.Setter;
import org.apache.skywalking.apm.util.StringUtil;
import org.apache.skywalking.oap.server.core.analysis.record.Record;
import org.apache.skywalking.oap.server.core.storage.StorageBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yunhai.hu
 * at 2019/4/9
 */
@Getter
@Setter
public class LogRecord extends Record {

    public static final String TIME_ID = "@timestamp";
    public static final String LEVEL_ID = "level";
    public static final String THREAD_ID = "thread";
    public static final String LOCATION_ID = "location";
    public static final String TRACE_ID_ID = "mdc.traceId";
    public static final String SPAN_ID = "mdc.span";
    public static final String HOSTNAME_ID = "hostname";
    public static final String APPNAME_ID = "appname";
    public static final String MESSAGE_ID = "message";
    public static final String STACK_ID = "stack";
    private static final String KEEP_APPLOG = "keep-applog";

    private String time;
    private LoggerLevel level;
    private String thread;
    private String location;
    private String traceId;
    private int span;
    private String hostname;
    private String appname;
    private String message;
    private String stack;

    @Override
    public String id() {
        return traceId + "|" + location + "|" + time;
    }

    @Override
    public String toString() {
        return "LogRecord{" +
                "time='" + time + '\'' +
                ", level=" + level +
                ", thread='" + thread + '\'' +
                ", location='" + location + '\'' +
                ", traceId='" + traceId + '\'' +
                ", span=" + span +
                ", hostname='" + hostname + '\'' +
                ", appname='" + appname + '\'' +
                ", message='" + message + '\'' +
                ", stack='" + stack + '\'' +
                "} " + super.toString();
    }

    public static String getIndex(String date) {
        return "applog-" + date;
    }

    public static String getType() {
        return KEEP_APPLOG;
    }

    public static class Builder implements StorageBuilder<LogRecord> {

        @Override
        public LogRecord map2Data(Map<String, Object> dbMap) {
            LogRecord record = new LogRecord();
            record.setTime((String) dbMap.get(TIME_ID));
            record.setLevel(LoggerLevel.valueOf(((String) dbMap.get(LEVEL_ID)).toUpperCase()));
            record.setThread((String) dbMap.get(THREAD_ID));
            record.setLocation((String) dbMap.get(LOCATION_ID));

            Map mdc = (Map) dbMap.get("mdc");
            if (mdc != null) {

                record.setTraceId((String) mdc.get("traceId"));
                String spanId = (String) mdc.get("span");
                if (spanId != null && StringUtil.isNumeric(spanId)) {
                    record.setSpan(Integer.valueOf(spanId));
                }
            }

            record.setHostname((String) dbMap.get(HOSTNAME_ID));
            record.setAppname((String) dbMap.get(APPNAME_ID));
            record.setMessage((String) dbMap.get(MESSAGE_ID));
            record.setStack((String) dbMap.get(STACK_ID));
            return record;
        }

        @Override
        public Map<String, Object> data2Map(LogRecord storageData) {
            Map<String, Object> map = new HashMap<>();
            map.put(TIME_ID, storageData.getTime());
            map.put(LEVEL_ID, storageData.getLevel());
            map.put(THREAD_ID, storageData.getThread());
            map.put(LOCATION_ID, storageData.getLocation());
            map.put(TRACE_ID_ID, storageData.getTraceId());
            map.put(SPAN_ID, storageData.getSpan());
            map.put(HOSTNAME_ID, storageData.getHostname());
            map.put(APPNAME_ID, storageData.getAppname());
            map.put(MESSAGE_ID, storageData.getMessage());
            map.put(STACK_ID, storageData.getStack());
            return map;
        }
    }

}
