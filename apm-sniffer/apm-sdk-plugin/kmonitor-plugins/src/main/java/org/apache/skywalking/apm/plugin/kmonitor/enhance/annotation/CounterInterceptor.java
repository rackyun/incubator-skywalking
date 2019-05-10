package org.apache.skywalking.apm.plugin.kmonitor.enhance.annotation;

import com.keep.monitor.agent.core.KeepMetrics;
import com.keep.monitor.annotation.Counter;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.plugin.kmonitor.enhance.MonitorContext;

import java.lang.reflect.Method;

/**
 * @author yunhai.hu
 * at 2019/5/5
 */
public class CounterInterceptor implements InstanceMethodsAroundInterceptor {


    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                             Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {

    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                              Class<?>[] argumentsTypes, Object ret) throws Throwable {
        Counter counter = method.getAnnotation(Counter.class);
        if (counter != null) {
            KeepMetrics metrics = MonitorContext.getMetrics();
            metrics.probe().counter(counter.value()).inc();
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {

    }
}
