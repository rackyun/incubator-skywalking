package org.apache.skywalking.apm.plugin.kmonitor.enhance.annotation.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.plugin.kmonitor.enhance.common.KmonitorClassEnhancePluginDefine;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.apache.skywalking.apm.agent.core.plugin.match.ClassAnnotationMatch.byClassAnnotationMatch;

/**
 * @author yunhai.hu
 * at 2019/5/5
 */
public class AnnotationDefine extends KmonitorClassEnhancePluginDefine {

    private static final String CLASS_ANNOTATION_NAME = "com.keep.monitor.annotation.EnableMonitor";
    private static final String TIMING_ANNOTATION_NAME = "com.keep.monitor.annotation.Timing";
    private static final String COUNTER_ANNOTATION_NAME = "com.keep.monitor.annotation.Counter";

    @Override
    protected ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[] {
            new ConstructorInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getConstructorMatcher() {
                    return any();
                }

                @Override
                public String getConstructorInterceptor() {
                    return "org.apache.skywalking.apm.plugin.kmonitor.enhance.annotation.GaugeInterceptor";
                }
            }
        };
    }

    @Override
    protected InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[] {
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return isAnnotatedWith(named(TIMING_ANNOTATION_NAME));
                }

                @Override
                public String getMethodsInterceptor() {
                    return "org.apache.skywalking.apm.plugin.kmonitor.enhance.annotation.TimingInterceptor";
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            },
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return isAnnotatedWith(named(COUNTER_ANNOTATION_NAME));
                }

                @Override
                public String getMethodsInterceptor() {
                    return "org.apache.skywalking.apm.plugin.kmonitor.enhance.annotation.CounterInterceptor";
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            }
        };
    }

    @Override
    protected ClassMatch enhanceClass() {
        return byClassAnnotationMatch(new String[]{CLASS_ANNOTATION_NAME});
    }

}
