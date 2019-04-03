package org.apache.skywalking.apm.toolkit.activation.log.keep.mdc;

import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;

/**
 * @author yunhai.hu
 * at 2019/4/3
 */
public abstract class LogMDCInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    @Override
    protected String[] witnessClasses() {
        return new String[]{"org.slf4j.MDC"};
    }
}
