package org.apache.skywalking.apm.plugin.kmonitor.enhance.annotation;

import com.keep.monitor.agent.core.KeepMetrics;
import com.keep.monitor.annotation.Gauge;
import com.keep.monitor.annotation.GaugeType;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.plugin.kmonitor.enhance.MonitorContext;

import java.lang.reflect.Method;

/**
 * @author yunhai.hu
 * at 2019/5/5
 */
public class GaugeInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                             Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {

    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                              Class<?>[] argumentsTypes, Object ret) throws Throwable {
        monitorGauges(objInst);
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {

    }

    private static void monitorGauges(Object object) {
        for (final Method method : object.getClass().getDeclaredMethods()) {
            final Gauge gauge = method.getAnnotation(Gauge.class);
            // only create gauge, if method takes no parameters and is non-void
            if (gauge != null && methodTakesNoParamsAndIsNonVoid(method)) {
                method.setAccessible(true);

                registerGauge(object, method, gauge);
            }
        }
    }

    private static void registerGauge(final Object object, final Method method,
                                      final Gauge gaugeAnnotation) {
        KeepMetrics metrics = MonitorContext.getMetrics();
        String name = gaugeAnnotation.value();
        GaugeType type = gaugeAnnotation.type();
        if (GaugeType.Gauge.equals(type)) {
            metrics.probe().gauge(name, method, object);
        } else {
            metrics.probe().countGauge(name, method, object);
        }
    }

    private static boolean methodTakesNoParamsAndIsNonVoid(Method method) {
        return method.getGenericParameterTypes().length == 0
                && method.getReturnType() != Void.class;
    }
}
