package org.apache.skywalking.apm.plugin.kmonitor.enhance.hystrix;

import com.keep.monitor.agent.core.Probe;
import com.keep.monitor.agent.core.utils.LogResolver;
import com.keep.monitor.metrics.TagsGauge;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.HystrixThreadPoolMetrics;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisherThreadPool;
import com.netflix.hystrix.util.HystrixRollingNumberEvent;
import org.apache.skywalking.apm.plugin.kmonitor.enhance.MonitorContext;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by chunpengwen on 2018/5/9.
 */
public class KeepHystrixPublisherThreadPool implements HystrixMetricsPublisherThreadPool {
    private static final Logger LOGGER =
            LogResolver.getLogger(KeepHystrixPublisherThreadPool.class);
    private final HystrixThreadPoolKey key;
    private final HystrixThreadPoolMetrics metrics;
    private final HystrixThreadPoolProperties properties;
    private final String metricGroup;
    private final String metricType;

    public KeepHystrixPublisherThreadPool(HystrixThreadPoolKey threadPoolKey, HystrixThreadPoolMetrics metrics, HystrixThreadPoolProperties properties) {
        this.key = threadPoolKey;
        this.metrics = metrics;
        this.properties = properties;
        this.metricGroup = "HystrixThreadPool";
        this.metricType = key.name();
    }

    @Override
    public void initialize() {
        Probe probe = (Probe) MonitorContext.getMetrics().probe();
        probe.tag("threadPoolGroup", metricGroup).tag("threadPoolKey", metricType);
        String name = "";

        name = createMetricName("threadActiveCount");
        probe.registerGauge(name, new TagsGauge<Number>(name) {
            @Override
            public Number getValue() {
                return metrics.getCurrentActiveCount();
            }
        });

        name = createMetricName("completedTaskCount");
        probe.registerGauge(name, new TagsGauge<Number>(name) {
            @Override
            public Number getValue() {
                return metrics.getCurrentCompletedTaskCount();
            }
        });

        name = createMetricName("largestPoolSize");
        probe.registerGauge(name, new TagsGauge<Number>(name) {
            @Override
            public Number getValue() {
                return metrics.getCurrentLargestPoolSize();
            }
        });

        name = createMetricName("totalTaskCount");
        probe.registerGauge(name, new TagsGauge<Number>(name) {
            @Override
            public Number getValue() {
                return metrics.getCurrentTaskCount();
            }
        });

        name = createMetricName("queueSize");
        probe.registerGauge(name, new TagsGauge<Number>(name) {
            @Override
            public Number getValue() {
                return metrics.getCurrentQueueSize();
            }
        });

        name = createMetricName("rollingMaxActiveThreads");
        probe.registerGauge(name, new TagsGauge<Number>(name) {
            @Override
            public Number getValue() {
                return metrics.getRollingMaxActiveThreads();
            }
        });

        name = createMetricName("countThreadsExecuted");
        probe.registerGauge(name, new TagsGauge<Number>(name) {
            @Override
            public Number getValue() {
                return metrics.getCumulativeCountThreadsExecuted();
            }
        });

        name = createMetricName("rollingCountCommandsRejected");
        probe.registerGauge(name, new TagsGauge<Number>(name) {
            @Override
            public Number getValue() {
                try {
                    return metrics.getRollingCount(HystrixRollingNumberEvent.THREAD_POOL_REJECTED);
                } catch (NoSuchFieldError error) {
                    LOGGER.log(Level.SEVERE, "While publishing CodaHale metrics, error looking up eventType for : rollingCountCommandsRejected.  Please check that all Hystrix versions are the same!");
                    return 0L;
                }
            }
        });

        name = createMetricName("rollingCountThreadsExecuted");
        probe.registerGauge(name, new TagsGauge<Number>(name) {
            @Override
            public Number getValue() {
                return metrics.getRollingCountThreadsExecuted();
            }
        });

    }

    private String createMetricName(String name) {
        return name;
    }

}
