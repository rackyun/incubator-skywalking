package org.apache.skywalking.apm.plugin.kmonitor.enhance.annotation;

import com.keep.monitor.agent.core.KeepMetrics;
import com.keep.monitor.annotation.Timing;
import com.keep.monitor.interfaces.metrics.ITimerContext;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.plugin.kmonitor.enhance.MonitorContext;

import java.lang.reflect.Method;

/**
 * @author yunhai.hu
 * at 2019/5/5
 */
public class TimingInterceptor implements InstanceMethodsAroundInterceptor {

    private static final ILog logger = LogManager.getLogger(TimingInterceptor.class);
    private static final String PROBE_CONTEXT_KEY = "annotation.probe.context";

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                             Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        KeepMetrics metrics = MonitorContext.getMetrics();
        String className = method.getClass().getName();
        String methodName = method.getName();
        Timing timing = method.getAnnotation(Timing.class);
        if (timing != null) {
            ITimerContext context = metrics.probe().tag("class", className)
                    .tag("method", methodName).timer(timing.value()).time();
            ContextManager.getRuntimeContext().put(PROBE_CONTEXT_KEY, context);
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                              Class<?>[] argumentsTypes, Object ret) throws Throwable {
        ITimerContext context = (ITimerContext) ContextManager.getRuntimeContext().get(PROBE_CONTEXT_KEY);
        if (context != null) {
            context.stop();
            ContextManager.getRuntimeContext().remove(PROBE_CONTEXT_KEY);
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {

    }

}
