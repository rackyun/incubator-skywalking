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

import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.sampling.ErrorSamplingService;
import org.apache.skywalking.apm.agent.core.sampling.SlowTraceSamplingService;

import java.util.LinkedList;
import java.util.List;

/**
 * The <code>IgnoredTracerContext</code> represent a context should be ignored.
 * So it just maintains the stack with an integer depth field.
 *
 * All operations through this will be ignored, and keep the memory and gc cost as low as possible.
 *
 * @author wusheng
 */
public class IgnoredTracerContext implements AbstractTracerContext {

    private int stackDepth;
    private TracingContext delegate;
    private boolean errorOccurred = false;
    private ErrorSamplingService errorSamplingService;
    private SlowTraceSamplingService slowTraceSamplingService;

    public IgnoredTracerContext() {
        this.stackDepth = 0;
        delegate = new TracingContext();
        errorSamplingService = ServiceManager.INSTANCE.findService(ErrorSamplingService.class);
        slowTraceSamplingService = ServiceManager.INSTANCE.findService(SlowTraceSamplingService.class);
    }

    @Override
    public void inject(ContextCarrier carrier) {
        delegate.inject(carrier);
        carrier.setSampled(false);
    }

    @Override
    public void extract(ContextCarrier carrier) {
        delegate.extract(carrier);
    }

    @Override public ContextSnapshot capture() {
        ContextSnapshot snapshot = delegate.capture();
        snapshot.setSample(false);
        return snapshot;
    }

    @Override public void continued(ContextSnapshot snapshot) {
        delegate.continued(snapshot);
    }

    @Override
    public String getReadableGlobalTraceId() {
        return delegate.getReadableGlobalTraceId();
    }

    @Override
    public AbstractSpan createEntrySpan(String operationName) {
        stackDepth++;
        return delegate.createEntrySpan(operationName);
    }

    @Override
    public AbstractSpan createLocalSpan(String operationName) {
        stackDepth++;
        return delegate.createLocalSpan(operationName);
    }

    @Override
    public AbstractSpan createExitSpan(String operationName, String remotePeer) {
        stackDepth++;
        return delegate.createExitSpan(operationName, remotePeer);
    }

    @Override
    public AbstractSpan activeSpan() {
        return delegate.activeSpan();
    }

    @Override
    public void stopSpan(AbstractSpan span) {
        stackDepth--;
        if (span.isErrorOccurred()) {
            this.errorOccurred = true;
        }
        if (isCompleted(stackDepth)
                && !isSampledError(errorOccurred, errorSamplingService)
                && !isSampledSlowTrace(span, slowTraceSamplingService)) {
            ListenerManager.notifyFinish(this);
        } else {
            delegate.stopSpan(span);
        }
    }

    private static boolean isCompleted(int stackDepth) {
        return stackDepth == 0;
    }

    private static boolean isSampledError(boolean errorOccurred, ErrorSamplingService errorSamplingService) {
        return errorOccurred && errorSamplingService.trySampling();
    }

    private static boolean isSampledSlowTrace(AbstractSpan span, SlowTraceSamplingService slowTraceSamplingService) {
        return slowTraceSamplingService.trySampling(span.durationTime());
    }

    public static class ListenerManager {
        private static List<IgnoreTracerContextListener> LISTENERS = new LinkedList<IgnoreTracerContextListener>();

        /**
         * Add the given {@link IgnoreTracerContextListener} to {@link #LISTENERS} list.
         *
         * @param listener the new listener.
         */
        public static synchronized void add(IgnoreTracerContextListener listener) {
            LISTENERS.add(listener);
        }

        /**
         * Notify the {@link IgnoredTracerContext.ListenerManager} about the given {@link IgnoredTracerContext} have
         * finished. And trigger {@link IgnoredTracerContext.ListenerManager} to notify all {@link #LISTENERS} 's {@link
         * IgnoreTracerContextListener#afterFinished(IgnoredTracerContext)}
         *
         * @param ignoredTracerContext
         */
        static void notifyFinish(IgnoredTracerContext ignoredTracerContext) {
            for (IgnoreTracerContextListener listener : LISTENERS) {
                listener.afterFinished(ignoredTracerContext);
            }
        }

        /**
         * Clear the given {@link IgnoreTracerContextListener}
         */
        public static synchronized void remove(IgnoreTracerContextListener listener) {
            LISTENERS.remove(listener);
        }
    }
}
