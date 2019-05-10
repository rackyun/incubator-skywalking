package org.apache.skywalking.apm.plugin.kmonitor.enhance.dubbo.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.agent.core.plugin.match.NameMatch;
import org.apache.skywalking.apm.plugin.kmonitor.enhance.common.KmonitorClassEnhancePluginDefine;

import static net.bytebuddy.matcher.ElementMatchers.any;

/**
 * @author yunhai.hu
 * at 2019/5/5
 */
public class DubboProviderMonitorInstrumentation extends KmonitorClassEnhancePluginDefine {

    private static final String ENHANCE_CLASS = "com.alibaba.dubbo.rpc.filter.ExceptionFilter";

    @Override
    protected ClassMatch enhanceClass() {
        return NameMatch.byName(ENHANCE_CLASS);
    }

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
                    return "org.apache.skywalking.apm.plugin.kmonitor.enhance.dubbo.DubboProviderConstructorInterceptor";
                }
            }
        };
    }

    @Override
    protected InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[0];
    }
}
