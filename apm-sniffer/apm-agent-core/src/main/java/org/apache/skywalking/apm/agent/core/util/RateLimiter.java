/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.skywalking.apm.agent.core.util;

public class RateLimiter {
    private final double creditsPerNanosecond;
    private double balance;
    private double maxBalance;
    private long lastTick;

    public RateLimiter(double creditsPerSecond, double maxBalance) {
        this.balance = maxBalance;
        this.maxBalance = maxBalance;
        this.creditsPerNanosecond = creditsPerSecond / 1.0e9;
    }

    public boolean checkCredit() {
        long currentTime = System.nanoTime();
        synchronized (this) {
            double elapsedTime = currentTime - lastTick;
            lastTick = currentTime;
            balance += elapsedTime * creditsPerNanosecond;
            if (balance > maxBalance) {
                balance = maxBalance;
            }
            if (balance >= 1L) {
                balance -= 1L;
                return true;
            }
            return false;
        }
    }

    public void forceSampled() {
        synchronized (this) {
            balance -= 1L;
        }
    }
}
