package org.apache.skywalking.apm.plugin.kmonitor.common;

import com.keep.monitor.agent.core.KeepMetrics;
import com.keep.monitor.agent.core.config.Config;
import com.keep.monitor.agent.core.utils.StringUtils;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;

/**
 * @author yunhai.hu
 * at 2019/5/6
 */
public class MonitorContext {

    private static final ILog logger = LogManager.getLogger(MonitorContext.class);
    private static volatile KeepMetrics INSTANCE = null;

    public static KeepMetrics getMetrics() {
        ensureInitialize();
        return INSTANCE;
    }

    private static void ensureInitialize() {
        if (INSTANCE == null) {

            synchronized (MonitorContext.class) {
                Config.Agent.APPLICATION_NAME = org.apache.skywalking.apm.agent.core.conf.Config.Agent.SERVICE_NAME;
                Config.Reporter.FALCON_HOST =
                        org.apache.skywalking.apm.agent.core.conf.Config.Monitor.MONITOR_SERVER_URL;
                logger.info("Try init keep metrics. Application name: {}",
                        Config.Agent.APPLICATION_NAME);
                if (INSTANCE == null && StringUtils.isNotEmpty(Config.Agent.APPLICATION_NAME)) {

                    INSTANCE = new KeepMetrics(Config.Agent.APPLICATION_NAME);
                }
            }
        }
    }
}
