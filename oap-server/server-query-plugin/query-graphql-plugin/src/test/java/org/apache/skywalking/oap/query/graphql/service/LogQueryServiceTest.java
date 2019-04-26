package org.apache.skywalking.oap.query.graphql.service;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.skywalking.oap.server.core.query.entity.LogRecord;
import org.junit.Test;

import java.text.ParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author yunhai.hu
 * at 2019/4/23
 */
public class LogQueryServiceTest {



    @Test
    public void queryLog() {
        Set<String> services = new HashSet<>();
        services.add("account-rpc");
        LogQueryService logQueryService = new LogQueryService(null, "http://localhost:3100/api/prom/query");
        try {
            List<LogRecord> logRecords = logQueryService.queryLog("169ec3978b0354961cb6571dab82f788",
                    DateUtils.parseDate("2019-04-23 15:00:15", "yyyy-MM-dd HH:mm:ss").getTime(), services);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}