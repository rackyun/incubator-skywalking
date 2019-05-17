package org.apache.skywalking.apm.plugin.kmonitor.enhance.dubbo;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.store.DataStore;
import com.keep.monitor.agent.core.Probe;
import com.keep.monitor.metrics.TagsGauge;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import org.apache.skywalking.apm.plugin.kmonitor.common.MonitorContext;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author yunhai.hu
 * at 2019/5/5
 */
public class DubboProviderConstructorInterceptor implements InstanceConstructorInterceptor {

    private static final ILog logger = LogManager.getLogger(DubboProviderConstructorInterceptor.class);

    private volatile ThreadPoolExecutor executor;
    private volatile int nullCount = 0;

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {
        registerMonitor();
    }

    private void registerMonitor() {
        Probe probe = (Probe) MonitorContext.getMetrics().probe();
        probe.tag("executor", "DubboServerExecutor");

        String name = createMetricName("maximumPoolSize");
        probe.registerGauge(name, new TagsGauge<Number>(name) {
            @Override
            public Number getValue() {
                return getExecutor().getMaximumPoolSize();
            }
        });

        name = createMetricName("corePoolSize");
        probe.registerGauge(name, new TagsGauge<Number>(name) {
            @Override
            public Number getValue() {
                return getExecutor().getCorePoolSize();
            }
        });

        name = createMetricName("largestPoolSize");
        probe.registerGauge(name, new TagsGauge<Number>(name) {
            @Override
            public Number getValue() {
                return getExecutor().getLargestPoolSize();
            }
        });

        name = createMetricName("activeCount");
        probe.registerGauge(name, new TagsGauge<Number>(name) {
            @Override
            public Number getValue() {
                return getExecutor().getActiveCount();
            }
        });

        name = createMetricName("taskCount");
        probe.registerGauge(name, new TagsGauge<Number>(name) {
            @Override
            public Number getValue() {
                return getExecutor().getTaskCount();
            }
        });

        name = createMetricName("queueCount");
        probe.registerGauge(name, new TagsGauge<Number>(name) {
            @Override
            public Number getValue() {
                return getExecutor().getQueue().size();
            }
        });
    }

    private ThreadPoolExecutor getExecutor() {
        // only try 10 times for null
        if (executor != null || nullCount > 10) {
            return executor;
        }

        synchronized (DubboProviderConstructorInterceptor.class) {
            if (executor != null) {
                return executor;
            }
            nullCount += 1;
            executor = loadDubboExecutor();
            return executor;
        }
    }

    private ThreadPoolExecutor loadDubboExecutor() {
        DataStore dataStore =
                ExtensionLoader.getExtensionLoader(DataStore.class).getDefaultExtension();
        Map<String, Object> executors = dataStore.get(Constants.EXECUTOR_SERVICE_COMPONENT_KEY);

        for (Map.Entry<String, Object> entry : executors.entrySet()) {
            ExecutorService executor = (ExecutorService) entry.getValue();

            if (executor instanceof ThreadPoolExecutor) {
                return (ThreadPoolExecutor) executor;
            }
        }
        return null;
    }

    private String createMetricName(String name) {
        return name;
    }
}
