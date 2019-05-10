package org.apache.skywalking.apm.plugin.kmonitor.enhance.hystrix;

import com.keep.monitor.agent.core.Probe;
import com.keep.monitor.agent.core.utils.LogResolver;
import com.keep.monitor.metrics.TagsGauge;
import com.netflix.hystrix.HystrixCollapserKey;
import com.netflix.hystrix.HystrixCollapserMetrics;
import com.netflix.hystrix.HystrixCollapserProperties;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisherCollapser;
import com.netflix.hystrix.util.HystrixRollingNumberEvent;
import org.apache.skywalking.apm.plugin.kmonitor.enhance.MonitorContext;
import rx.functions.Func0;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by chunpengwen on 2018/5/9.
 */
public class KeepHystrixPublisherCollapser implements HystrixMetricsPublisherCollapser {
    private static final Logger LOGGER =
            LogResolver.getLogger(KeepHystrixPublisherCollapser.class);
    private final HystrixCollapserKey key;
    private final HystrixCollapserMetrics metrics;
    private final HystrixCollapserProperties properties;
    private final String metricType;

    public KeepHystrixPublisherCollapser(HystrixCollapserKey collapserKey,
                                         HystrixCollapserMetrics metrics, HystrixCollapserProperties properties) {
        this.key = collapserKey;
        this.metrics = metrics;
        this.properties = properties;
        this.metricType = key.name();
    }

    @Override
    public void initialize() {
        Probe probe = (Probe) MonitorContext.getMetrics().probe();
        probe.tag("commandKey", metricType);

        // rolling counts
        safelyCreateRollingCountForEvent(probe, "rollingRequestsBatched",
            new Func0<HystrixRollingNumberEvent>() {

                @Override
                public HystrixRollingNumberEvent call() {
                    return HystrixRollingNumberEvent.COLLAPSER_REQUEST_BATCHED;
                }
            });
        safelyCreateRollingCountForEvent(probe, "rollingBatches",
            new Func0<HystrixRollingNumberEvent>() {

                @Override
                public HystrixRollingNumberEvent call() {
                    return HystrixRollingNumberEvent.COLLAPSER_BATCH;
                }
            });
        safelyCreateRollingCountForEvent(probe, "rollingCountResponsesFromCache",
            new Func0<HystrixRollingNumberEvent>() {

                @Override
                public HystrixRollingNumberEvent call() {
                    return HystrixRollingNumberEvent.RESPONSE_FROM_CACHE;
                }
            });

        // batch size metrics
        String name = createMetricName("batchSize_mean");
        probe.registerGauge(name, new TagsGauge<Integer>(name) {
            @Override
            public Integer getValue() {
                return metrics.getBatchSizeMean();
            }
        });

        name = createMetricName("batchSize_percentile_90");
        probe.registerGauge(name, new TagsGauge<Integer>(name) {
            @Override
            public Integer getValue() {
                return metrics.getBatchSizePercentile(90);
            }
        });

        name = createMetricName("batchSize_percentile_99");
        probe.registerGauge(name, new TagsGauge<Integer>(name) {
            @Override
            public Integer getValue() {
                return metrics.getBatchSizePercentile(99);
            }
        });

        name = createMetricName("batchSize_percentile_995");
        probe.registerGauge(name, new TagsGauge<Integer>(name) {
            @Override
            public Integer getValue() {
                return metrics.getBatchSizePercentile(99.5);
            }
        });

        // shard size metrics
        name = createMetricName("shardSize_mean");
        probe.registerGauge(name, new TagsGauge<Integer>(name) {
            @Override
            public Integer getValue() {
                return metrics.getShardSizeMean();
            }
        });

        name = createMetricName("shardSize_percentile_90");
        probe.registerGauge(name, new TagsGauge<Integer>(name) {
            @Override
            public Integer getValue() {
                return metrics.getShardSizePercentile(90);
            }
        });

        name = createMetricName("shardSize_percentile_99");
        probe.registerGauge(name, new TagsGauge<Integer>(name) {
            @Override
            public Integer getValue() {
                return metrics.getShardSizePercentile(99);
            }
        });

        name = createMetricName("shardSize_percentile_995");
        probe.registerGauge(name, new TagsGauge<Integer>(name) {
            @Override
            public Integer getValue() {
                return metrics.getShardSizePercentile(99.5);
            }
        });

    }

    private String createMetricName(String name) {
        return name;
    }

    private void safelyCreateRollingCountForEvent(Probe probe, final String name,
                                                  final Func0<HystrixRollingNumberEvent> eventThunk) {
        String metricName = createMetricName(name);
        probe.registerGauge(name, new TagsGauge<Long>(name) {
            @Override
            public Long getValue() {
                try {
                    return metrics.getRollingCount(eventThunk.call());
                } catch (NoSuchFieldError error) {
                    LOGGER.log(Level.SEVERE,
                            "While publishing CodaHale metrics, error looking up eventType for : {0}.  Please check " +
                                    "that all Hystrix versions are the same!",
                            name);
                    return 0L;
                }
            }
        });
    }
}
