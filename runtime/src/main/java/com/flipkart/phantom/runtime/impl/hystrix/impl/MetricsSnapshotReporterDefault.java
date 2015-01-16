/*
 * Copyright 2012-2015, the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.flipkart.phantom.runtime.impl.hystrix.impl;

import com.flipkart.phantom.runtime.impl.hystrix.MetricsSnapshotReporter;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.HystrixThreadPoolMetrics;
import com.netflix.hystrix.util.HystrixRollingNumberEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * The <code>MetricsSnapshotReporterImplV2</code> just reports current hystrix metrics snapshot
 *
 * @author Ishwar Kumar
 * @version 1.0, 15 Jan 2015
 */
public class MetricsSnapshotReporterDefault implements MetricsSnapshotReporter {
    public Map<String, Map<String, Map<String, Long>>> lastOneMinMetrics = new HashMap<String, Map<String, Map<String, Long>>>();

    /* returns the current metrics as reported by HystrixCommandMetrics */
    public Map<String, Map<String, Map<String, Long>>> getMetricsLastDuration() {
        for (HystrixCommandMetrics commandMetrics : HystrixCommandMetrics.getInstances()) {
            String commandName = commandMetrics.getCommandGroup().name() + "." + commandMetrics.getCommandKey().name();

            if (lastOneMinMetrics.get("HystrixCommand") == null) {
                lastOneMinMetrics.put("HystrixCommand", new HashMap<String, Map<String, Long>>());
            }

            Map<String, Long> currStats = new HashMap<String, Long>();

            HystrixCommandMetrics.HealthCounts healthCounts = commandMetrics.getHealthCounts();

            currStats.put("errorCount", healthCounts.getErrorCount());
            currStats.put("requestCount", healthCounts.getTotalRequests());
            currStats.put("rollingCountFailure", commandMetrics.getRollingCount(HystrixRollingNumberEvent.FAILURE));
            currStats.put("rollingCountSemaphoreRejected", commandMetrics.getRollingCount(HystrixRollingNumberEvent.SEMAPHORE_REJECTED));
            currStats.put("rollingCountShortCircuited", commandMetrics.getRollingCount(HystrixRollingNumberEvent.SHORT_CIRCUITED));
            currStats.put("rollingCountSuccess", commandMetrics.getRollingCount(HystrixRollingNumberEvent.SUCCESS));
            currStats.put("rollingCountThreadPoolRejected", commandMetrics.getRollingCount(HystrixRollingNumberEvent.THREAD_POOL_REJECTED));
            currStats.put("rollingCountTimeout", commandMetrics.getRollingCount(HystrixRollingNumberEvent.TIMEOUT));
            currStats.put("latencyTotal_mean", (long) commandMetrics.getTotalTimeMean());

            currStats.put("0", (long) commandMetrics.getTotalTimePercentile(0));
            currStats.put("25", (long) commandMetrics.getTotalTimePercentile(25));
            currStats.put("50", (long) commandMetrics.getTotalTimePercentile(50));
            currStats.put("75", (long) commandMetrics.getTotalTimePercentile(75));
            currStats.put("90", (long) commandMetrics.getTotalTimePercentile(90));
            currStats.put("95", (long) commandMetrics.getTotalTimePercentile(95));
            currStats.put("99", (long) commandMetrics.getTotalTimePercentile(99));
            currStats.put("99.5", (long) commandMetrics.getTotalTimePercentile(99.5));
            currStats.put("100", (long) commandMetrics.getTotalTimePercentile(100));

            lastOneMinMetrics.get("HystrixCommand").put(commandName, currStats);
        }


        for (HystrixThreadPoolMetrics commandMetrics : HystrixThreadPoolMetrics.getInstances()) {
            String commandName = commandMetrics.getThreadPoolKey().name();

            if (lastOneMinMetrics.get("HystrixThreadPool") == null) {
                lastOneMinMetrics.put("HystrixThreadPool", new HashMap<String, Map<String, Long>>());
            }
            Map<String, Long> currStats = new HashMap<String, Long>();

            currStats.put("currentActiveCount", commandMetrics.getCurrentActiveCount().longValue());
            currStats.put("currentQueueSize", commandMetrics.getCurrentQueueSize().longValue());

            lastOneMinMetrics.get("HystrixThreadPool").put(commandName, currStats);
        }

        return lastOneMinMetrics;
    }
}