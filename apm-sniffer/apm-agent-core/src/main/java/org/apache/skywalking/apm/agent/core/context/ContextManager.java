/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.apm.agent.core.context;

import org.apache.skywalking.apm.agent.core.boot.BootService;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.conf.RemoteDownstreamConfig;
import org.apache.skywalking.apm.agent.core.context.logging.SpanLog;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.dictionary.DictionaryUtil;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.os.OSUtil;
import org.apache.skywalking.apm.agent.core.sampling.SamplingService;
import org.apache.skywalking.apm.util.StringUtil;

/**
 * {@link ContextManager} controls the whole context of {@link TraceSegment}. Any {@link TraceSegment} relates to
 * single-thread, so this context use {@link ThreadLocal} to maintain the context, and make sure, since a {@link
 * TraceSegment} starts, all ChildOf spans are in the same context. <p> What is 'ChildOf'?
 * https://github.com/opentracing/specification/blob/master/specification.md#references-between-spans
 *
 * <p> Also, {@link ContextManager} delegates to all {@link AbstractTracerContext}'s major methods.
 *
 * @author wusheng
 */
public class ContextManager implements TracingContextListener, BootService, IgnoreTracerContextListener, SkipedTracerContextListener {
    private static final ILog logger = LogManager.getLogger(ContextManager.class);
    private static ThreadLocal<AbstractTracerContext> CONTEXT = new ThreadLocal<AbstractTracerContext>();
    private static ThreadLocal<RuntimeContext> RUNTIME_CONTEXT = new ThreadLocal<RuntimeContext>();
    private static ContextManagerExtendService EXTEND_SERVICE;
    private static SpanLog SPANLOG = null;

    private static AbstractTracerContext getOrCreate(String operationName, boolean forceSampling, boolean forceIgnore) {
        AbstractTracerContext context = CONTEXT.get();
        if (EXTEND_SERVICE == null) {
            EXTEND_SERVICE = ServiceManager.INSTANCE.findService(ContextManagerExtendService.class);
        }
        if (context == null) {
            if (StringUtil.isEmpty(operationName)) {
                if (logger.isDebugEnable()) {
                    logger.debug("No operation name, ignore this trace.");
                }
                context = new IgnoredTracerContext();
            } else {
                if (RemoteDownstreamConfig.Agent.SERVICE_ID != DictionaryUtil.nullValue()
                        && RemoteDownstreamConfig.Agent.SERVICE_INSTANCE_ID != DictionaryUtil.nullValue()
                        && !forceIgnore
                ) {
                    context = EXTEND_SERVICE.createTraceContext(operationName, forceSampling);
                } else {
                    /**
                     * Can't register to collector, no need to trace anything.
                     */
                    context = new IgnoredTracerContext();
                }
            }
            CONTEXT.set(context);
        }
        return context;
    }

    private static AbstractTracerContext getOrCreate(String operationName, boolean forceSampling) {
        return getOrCreate(operationName, forceSampling, false);
    }

    private static AbstractTracerContext get() {
        return CONTEXT.get();
    }

    /**
     * @return the first global trace id if needEnhance. Otherwise, "N/A".
     */
    public static String getGlobalTraceId() {
        AbstractTracerContext segment = CONTEXT.get();
        if (segment == null) {
            return "";
        } else {
            return segment.getReadableGlobalTraceId();
        }
    }

    /**
     * @return the current span id if have. Otherwise, empty string "".
     */
    public static String getCurrentSpanId() {
        AbstractTracerContext segment = CONTEXT.get();
        if (segment == null) {
            return "";
        } else {
            return segment.activeSpan() != null ? String.valueOf(segment.activeSpan().getSpanId()) : "";
        }
    }

    public static AbstractSpan createEntrySpan(String operationName, ContextCarrier carrier) {
        SamplingService samplingService = ServiceManager.INSTANCE.findService(SamplingService.class);
        AbstractSpan span;
        AbstractTracerContext context;
        if (carrier != null && carrier.isValid()) {
            if (carrier.isSampled()) {
                samplingService.forceSampled();
                context = getOrCreate(operationName, true);
            } else {
                context = getOrCreate(operationName, false, true);
            }
            span = context.createEntrySpan(operationName);
            context.extract(carrier);
        } else {
            context = getOrCreate(operationName, false);
            span = context.createEntrySpan(operationName);
        }
        setServiceInstanceTag(span);
        logStartedSpan(span);
        return span;
    }

    public static AbstractSpan createLocalSpan(String operationName) {
        return createLocalSpan(operationName, false);
    }

    public static AbstractSpan createLocalSpan(String operationName, boolean forceSampling) {
        AbstractTracerContext context = getOrCreate(operationName, forceSampling);
        AbstractSpan span = context.createLocalSpan(operationName);
        setServiceInstanceTag(span);
        logStartedSpan(span);
        return span;
    }

    public static AbstractSpan createExitSpan(String operationName, ContextCarrier carrier, String remotePeer) {
        if (carrier == null) {
            throw new IllegalArgumentException("ContextCarrier can't be null.");
        }
        AbstractTracerContext context = getOrCreate(operationName, false);
        AbstractSpan span = context.createExitSpan(operationName, remotePeer);
        context.inject(carrier);
        setServiceInstanceTag(span);
        logStartedSpan(span);
        return span;
    }

    public static AbstractSpan createExitSpan(String operationName, String remotePeer) {
        AbstractTracerContext context = getOrCreate(operationName, false);
        AbstractSpan span = context.createExitSpan(operationName, remotePeer);
        setServiceInstanceTag(span);
        logStartedSpan(span);
        return span;
    }

    private static void setServiceInstanceTag(AbstractSpan span) {
        Tags.SERVICE_INSTANCE.set(span, OSUtil.getHostName() + "-" + Config.Agent.SERVICE_NAME);
    }

    public static void inject(ContextCarrier carrier) {
        get().inject(carrier);
    }

    public static void extract(ContextCarrier carrier) {
        if (carrier == null) {
            throw new IllegalArgumentException("ContextCarrier can't be null.");
        }
        if (carrier.isValid()) {
            get().extract(carrier);
        }
    }

    public static ContextSnapshot capture() {
        return get().capture();
    }

    public static void continued(ContextSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("ContextSnapshot can't be null.");
        }
        if (snapshot.isValid() && !snapshot.isFromCurrent()) {
            get().continued(snapshot);
        }
    }

    public static AbstractSpan activeSpan() {
        return get().activeSpan();
    }

    public static void stopSpan() {
        if (get() != null && get().activeSpan() != null) {
            AbstractSpan span = get().activeSpan();
            logger.debug("stop span {}, spanId {}", span.getOperationName(), span.getSpanId());
        } else {
            logger.debug("stop a empty span");
        }
        stopSpan(activeSpan());
    }

    public static void stopSpan(AbstractSpan span) {
        logStoppedSpan(span);
        get().stopSpan(span);
    }

    @Override
    public void prepare() throws Throwable {

    }

    @Override
    public void boot() {
        ContextManagerExtendService service = ServiceManager.INSTANCE.findService(ContextManagerExtendService.class);
        service.registerListeners(this);
    }

    @Override
    public void onComplete() throws Throwable {

    }

    @Override public void shutdown() throws Throwable {

    }

    @Override
    public void afterFinished(TraceSegment traceSegment) {
        CONTEXT.remove();
    }

    @Override
    public void afterFinished(IgnoredTracerContext traceSegment) {
        CONTEXT.remove();
    }

    @Override
    public void afterFinished(SkipedTracerContext traceSegment) {
        CONTEXT.remove();
    }

    public static boolean isActive() {
        return get() != null;
    }

    public static RuntimeContext getRuntimeContext() {
        RuntimeContext runtimeContext = RUNTIME_CONTEXT.get();
        if (runtimeContext == null) {
            runtimeContext = new RuntimeContext(RUNTIME_CONTEXT);
            RUNTIME_CONTEXT.set(runtimeContext);
        }

        return runtimeContext;
    }

    public static void setSPANLOG(SpanLog spanLog) {
        SPANLOG = spanLog;
    }

    private static void logStartedSpan(AbstractSpan span) {
        if (SPANLOG != null) {
            SPANLOG.logStartedSpan(getGlobalTraceId(), span);
        }
    }

    private static void logStoppedSpan(AbstractSpan span) {
        AbstractSpan parentSpan = null;
        if (get() != null) {
            parentSpan = get().activeSpan();
        }
        if (SPANLOG != null) {
            SPANLOG.logStoppedSpan(getGlobalTraceId(), span, parentSpan);
        }
    }
}
