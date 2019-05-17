package org.apache.skywalking.apm.plugin.kmonitor.enhance.hystrix;

import com.keep.monitor.agent.core.Probe;
import com.keep.monitor.agent.core.utils.LogResolver;
import com.keep.monitor.agent.core.utils.StringUtils;
import com.keep.monitor.metrics.TagsGauge;
import com.netflix.hystrix.*;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisherCommand;
import com.netflix.hystrix.util.HystrixRollingNumberEvent;
import org.apache.skywalking.apm.plugin.kmonitor.common.MonitorContext;
import rx.functions.Func0;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by chunpengwen on 2018/5/9.
 */
public class KeepHystrixMetricsPublisherCommand implements HystrixMetricsPublisherCommand {
    private static final Logger LOGGER =
            LogResolver.getLogger(KeepHystrixMetricsPublisherCommand.class);
    private final HystrixCommandKey key;
    private final HystrixCommandGroupKey commandGroupKey;
    private final HystrixCommandMetrics metrics;
    private final HystrixCircuitBreaker circuitBreaker;
    private final HystrixCommandProperties properties;
    private final String metricGroup;
    private final String metricType;

    public KeepHystrixMetricsPublisherCommand(HystrixCommandKey commandKey,
            HystrixCommandGroupKey commandGroupKey, HystrixCommandMetrics metrics,
            HystrixCircuitBreaker circuitBreaker, HystrixCommandProperties properties) {
        this.key = commandKey;
        this.commandGroupKey = commandGroupKey;
        this.metrics = metrics;
        this.circuitBreaker = circuitBreaker;
        this.properties = properties;
        this.metricGroup = StringUtils.normalized(commandGroupKey.name().replaceAll(":", ""));
        this.metricType = StringUtils.normalized(key.name());

    }

    @Override
    public void initialize() {
        Probe probe = (Probe) MonitorContext.getMetrics().probe();
        probe.tag("commandGroup", metricGroup).tag("commandKey", metricType);

        String name = createMetricName("isCircuitBreakerOpen");
        probe.registerGauge(name, new TagsGauge(name) {
            @Override
            public Object getValue() {
                return circuitBreaker.isOpen() ? 1 : 0;
            }
        });

        // rolling counts
        safelyCreateRollingCountForEvent(probe, "rollingCountBadRequests", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.BAD_REQUEST;
            }
        });
        safelyCreateRollingCountForEvent(probe, "rollingCountCollapsedRequests", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.COLLAPSED;
            }
        });
        safelyCreateRollingCountForEvent(probe, "rollingCountEmit", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.EMIT;
            }
        });
        safelyCreateRollingCountForEvent(probe, "rollingCountExceptionsThrown", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.EXCEPTION_THROWN;
            }
        });
        safelyCreateRollingCountForEvent(probe, "rollingCountFailure", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.FAILURE;
            }
        });
        safelyCreateRollingCountForEvent(probe, "rollingCountFallbackEmit", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.FALLBACK_EMIT;
            }
        });
        safelyCreateRollingCountForEvent(probe, "rollingCountFallbackFailure", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.FALLBACK_FAILURE;
            }
        });

        safelyCreateRollingCountForEvent(probe, "rollingCountFallbackMissing", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.FALLBACK_MISSING;
            }
        });
        safelyCreateRollingCountForEvent(probe, "rollingCountFallbackRejection", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.FALLBACK_REJECTION;
            }
        });
        safelyCreateRollingCountForEvent(probe, "rollingCountFallbackSuccess", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.FALLBACK_SUCCESS;
            }
        });
        safelyCreateRollingCountForEvent(probe, "rollingCountResponsesFromCache", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.RESPONSE_FROM_CACHE;
            }
        });
        safelyCreateRollingCountForEvent(probe, "rollingCountSemaphoreRejected", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.SEMAPHORE_REJECTED;
            }
        });
        safelyCreateRollingCountForEvent(probe, "rollingCountShortCircuited", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.SHORT_CIRCUITED;
            }
        });
        safelyCreateRollingCountForEvent(probe, "rollingCountSuccess", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.SUCCESS;
            }
        });
        safelyCreateRollingCountForEvent(probe, "rollingCountThreadPoolRejected", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.THREAD_POOL_REJECTED;
            }
        });
        safelyCreateRollingCountForEvent(probe, "rollingCountTimeout", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.TIMEOUT;
            }
        });

        name = createMetricName("executionSemaphorePermitsInUse");
        // the number of executionSemaphorePermits in use right now
        probe.registerGauge(name, new TagsGauge<Integer>(name) {
            @Override
            public Integer getValue() {
                return metrics.getCurrentConcurrentExecutionCount();
            }
        });

        name = createMetricName("errorPercentage");
        // error percentage derived from current metrics
        probe.registerGauge(name, new TagsGauge<Integer>(name) {
            @Override
            public Integer getValue() {
                return metrics.getHealthCounts().getErrorPercentage();
            }
        });

        name = createMetricName("latencyExecute_mean");
        // latency metrics
        probe.registerGauge(name, new TagsGauge<Integer>(name) {
            @Override
            public Integer getValue() {
                return metrics.getExecutionTimeMean();
            }
        });

        name = createMetricName("latencyExecute_percentile_90");
        probe.registerGauge(name, new TagsGauge<Integer>(name) {
            @Override
            public Integer getValue() {
                return metrics.getExecutionTimePercentile(90);
            }
        });

        name = createMetricName("latencyExecute_percentile_99");
        probe.registerGauge(name, new TagsGauge<Integer>(name) {
            @Override
            public Integer getValue() {
                return metrics.getExecutionTimePercentile(99);
            }
        });

        name = createMetricName("latencyExecute_percentile_995");
        probe.registerGauge(name, new TagsGauge<Integer>(name) {
            @Override
            public Integer getValue() {
                return metrics.getExecutionTimePercentile(99.5);
            }
        });

        name = createMetricName("latencyTotal_mean");
        probe.registerGauge(name, new TagsGauge<Integer>(name) {
            @Override
            public Integer getValue() {
                return metrics.getTotalTimeMean();
            }
        });

        name = createMetricName("latencyTotal_percentile_90");
        probe.registerGauge(name, new TagsGauge<Integer>(name) {
            @Override
            public Integer getValue() {
                return metrics.getTotalTimePercentile(90);
            }
        });

        name = createMetricName("latencyTotal_percentile_99");
        probe.registerGauge(name, new TagsGauge<Integer>(name) {
            @Override
            public Integer getValue() {
                return metrics.getTotalTimePercentile(99);
            }
        });

        name = createMetricName("latencyTotal_percentile_995");
        probe.registerGauge(name, new TagsGauge<Integer>(name) {
            @Override
            public Integer getValue() {
                return metrics.getTotalTimePercentile(99.5);
            }
        });


    }

    private String createMetricName(String name) {
        return name;
    }

    private void safelyCreateRollingCountForEvent(Probe probe, final String name,
                                                  final Func0<HystrixRollingNumberEvent> eventThunk) {
        String metricName = createMetricName(name);
        probe.registerGauge(metricName, new TagsGauge<Long>(metricName) {
            @Override
            public Long getValue() {
                try {
                    return metrics.getRollingCount(eventThunk.call());
                } catch (NoSuchFieldError error) {
                    LOGGER.log(Level.SEVERE,
                            "While publishing CodaHale metrics, error looking up eventType for : {0}.  Please check that all Hystrix versions are the same!",
                            name);
                    LOGGER.log(Level.SEVERE, "Error: ", error);
                    return 0L;
                } catch (RuntimeException error) {
                    LOGGER.log(Level.SEVERE,
                            "While publishing CodaHale metrics, error looking up eventType for : {0}.  Please check that all Hystrix versions are the same!",
                            name);
                    LOGGER.log(Level.SEVERE, "Error: ", error);
                    return 0L;
                }
            }
        });
    }
}
