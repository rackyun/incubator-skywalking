package org.apache.skywalking.apm.plugin.kmonitor.enhance.hystrix;

import com.netflix.hystrix.*;
import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;
import com.netflix.hystrix.strategy.executionhook.HystrixCommandExecutionHook;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisher;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisherCollapser;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisherCommand;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisherThreadPool;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;

/**
 * Created by chunpengwen on 2018/5/9.
 */
public class KeepHystrixMetricsPublisher extends HystrixMetricsPublisher {
    @Override
    public HystrixMetricsPublisherCommand getMetricsPublisherForCommand(
            HystrixCommandKey commandKey, HystrixCommandGroupKey commandGroupKey,
            HystrixCommandMetrics metrics, HystrixCircuitBreaker circuitBreaker,
            HystrixCommandProperties properties) {
        return new KeepHystrixMetricsPublisherCommand(commandKey, commandGroupKey, metrics,
                circuitBreaker, properties);
    }

    @Override
    public HystrixMetricsPublisherThreadPool getMetricsPublisherForThreadPool(
            HystrixThreadPoolKey threadPoolKey, HystrixThreadPoolMetrics metrics,
            HystrixThreadPoolProperties properties) {
        return new KeepHystrixPublisherThreadPool(threadPoolKey, metrics, properties);
    }

    @Override
    public HystrixMetricsPublisherCollapser getMetricsPublisherForCollapser(
            HystrixCollapserKey collapserKey, HystrixCollapserMetrics metrics,
            HystrixCollapserProperties properties) {
        return new KeepHystrixPublisherCollapser(collapserKey, metrics, properties);
    }

    private static boolean REGISTERED = false;

    public static boolean isRegistered() {
        return REGISTERED;
    }

    public static void register() {
        KeepHystrixMetricsPublisher publisher = new KeepHystrixMetricsPublisher();
        HystrixCommandExecutionHook commandExecutionHook = HystrixPlugins.getInstance().getCommandExecutionHook();
        HystrixEventNotifier eventNotifier = HystrixPlugins.getInstance().getEventNotifier();
        HystrixPropertiesStrategy propertiesStrategy = HystrixPlugins.getInstance().getPropertiesStrategy();
        HystrixConcurrencyStrategy concurrencyStrategy = HystrixPlugins.getInstance().getConcurrencyStrategy();
        HystrixPlugins.reset();
        HystrixPlugins.getInstance().registerConcurrencyStrategy(concurrencyStrategy);
        HystrixPlugins.getInstance().registerCommandExecutionHook(commandExecutionHook);
        HystrixPlugins.getInstance().registerEventNotifier(eventNotifier);
        HystrixPlugins.getInstance().registerMetricsPublisher(publisher);
        HystrixPlugins.getInstance().registerPropertiesStrategy(propertiesStrategy);
        REGISTERED = true;
    }
}
