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

import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.NoopSpan;

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
public class SkipedTracerContext implements AbstractTracerContext {
    private static final NoopSpan NOOP_SPAN = new NoopSpan();

    private int stackDepth;

    public SkipedTracerContext() {
        this.stackDepth = 0;
    }

    @Override
    public void inject(ContextCarrier carrier) {
    }

    @Override
    public void extract(ContextCarrier carrier) {
    }

    @Override public ContextSnapshot capture() {
        return new ContextSnapshot(null, -1, null);
    }

    @Override public void continued(ContextSnapshot snapshot) {

    }

    @Override
    public String getReadableGlobalTraceId() {
        return "";
    }

    @Override
    public AbstractSpan createEntrySpan(String operationName) {
        stackDepth++;
        return NOOP_SPAN;
    }

    @Override
    public AbstractSpan createLocalSpan(String operationName) {
        stackDepth++;
        return NOOP_SPAN;
    }

    @Override
    public AbstractSpan createExitSpan(String operationName, String remotePeer) {
        stackDepth++;
        return NOOP_SPAN;
    }

    @Override
    public AbstractSpan activeSpan() {
        return NOOP_SPAN;
    }

    @Override
    public void stopSpan(AbstractSpan span) {
        stackDepth--;
        if (stackDepth == 0) {
            SkipedTracerContext.ListenerManager.notifyFinish(this);
        }
    }

    public static class ListenerManager {
        private static List<SkipedTracerContextListener> LISTENERS = new LinkedList<SkipedTracerContextListener>();

        /**
         * Add the given {@link SkipedTracerContextListener} to {@link #LISTENERS} list.
         *
         * @param listener the new listener.
         */
        public static synchronized void add(SkipedTracerContextListener listener) {
            LISTENERS.add(listener);
        }

        /**
         * Notify the {@link SkipedTracerContext.ListenerManager} about the given {@link SkipedTracerContext} have
         * finished. And trigger {@link SkipedTracerContext.ListenerManager} to notify all {@link #LISTENERS} 's {@link
         * SkipedTracerContextListener#afterFinished(SkipedTracerContext)}
         *
         * @param skipedTracerContext
         */
        static void notifyFinish(SkipedTracerContext skipedTracerContext) {
            for (SkipedTracerContextListener listener : LISTENERS) {
                listener.afterFinished(skipedTracerContext);
            }
        }

        /**
         * Clear the given {@link SkipedTracerContextListener}
         */
        public static synchronized void remove(SkipedTracerContextListener listener) {
            LISTENERS.remove(listener);
        }
    }
}
