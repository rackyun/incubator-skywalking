package org.apache.skywalking.apm.plugin.kmonitor.enhance.spring4.boot;

import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.plugin.kmonitor.enhance.MonitorContext;
import org.apache.skywalking.apm.plugin.kmonitor.enhance.hystrix.KeepHystrixMetricsPublisher;

import java.lang.reflect.Method;

/**
 * @author yunhai.hu
 * at 2019/5/9
 */
public class SpringBootRunInterceptor implements InstanceMethodsAroundInterceptor {

    private static final ILog logger = LogManager.getLogger(SpringBootRunInterceptor.class);

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                             Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        MonitorContext.getMetrics();
        registerHystrixMonitor();
    }

    private void registerHystrixMonitor() {
        KeepHystrixMetricsPublisher.register();
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                              Class<?>[] argumentsTypes, Object ret) throws Throwable {
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {

    }
}
