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


package org.apache.skywalking.apm.agent.core.sampling;

import org.apache.skywalking.apm.agent.core.boot.BootService;
import org.apache.skywalking.apm.agent.core.boot.DefaultImplementor;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.util.RateLimiter;

/**
 * The <code>SamplingService</code> take charge of how to sample the {@link TraceSegment}. Every {@link TraceSegment}s
 * have been traced, but, considering CPU cost of serialization/deserialization, and network bandwidth, the agent do NOT
 * send all of them to collector, if SAMPLING is on.
 * <p>
 * By default, SAMPLING is on, and  {@link Config.Agent#SAMPLE_N_PER_3_SECS }
 *
 * @author wusheng
 */
@DefaultImplementor
public class SamplingService implements BootService {
    private static final ILog logger = LogManager.getLogger(SamplingService.class);

    private volatile boolean on = false;
    private RateLimiter rateLimiter;

    @Override
    public void prepare() throws Throwable {

    }

    @Override
    public void boot() throws Throwable {
        if (rateLimiter != null) {
            /**
             * If {@link #boot()} invokes twice, mostly in test cases,
             * cancel the old one.
             */
            return;
        }
        if (Config.Agent.SAMPLE_N_PER_3_SECS > 0) {
            on = true;
            rateLimiter = new RateLimiter((double) Config.Agent.SAMPLE_N_PER_3_SECS / 3,
                    (double) Config.Agent.SAMPLE_N_PER_3_SECS);
            logger.debug("Agent sampling mechanism started. Sample {} traces in 3 seconds.", Config.Agent.SAMPLE_N_PER_3_SECS);
        }
    }

    @Override
    public void onComplete() throws Throwable {

    }

    @Override
    public void shutdown() throws Throwable {

    }

    /**
     * @return true, if sampling mechanism is on, and getDefault the sampling factor successfully.
     */
    public boolean trySampling() {
        if (on) {
            return rateLimiter.checkCredit();
        }
        return true;
    }

    /**
     * Increase the sampling factor by force,
     * to avoid sampling too many traces.
     * If many distributed traces require sampled,
     * the trace beginning at local, has less chance to be sampled.
     */
    public void forceSampled() {
        if (on) {
            rateLimiter.forceSampled();
        }
    }

}
