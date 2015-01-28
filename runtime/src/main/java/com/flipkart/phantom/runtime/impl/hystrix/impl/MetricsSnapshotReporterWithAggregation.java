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
 * The <code>MetricsSnapshotReporterImplV1</code> provides support
 * to collect hystrix metrics and store them from last N iterations
 *
 * @author Ishwar Kumar
 * @version 1.0, 15 Jan 2015
 */

public class MetricsSnapshotReporterWithAggregation implements Runnable, MetricsSnapshotReporter {
    public Map<String, Map<String, Map<String, Long>>> lastDurationMetrics = new HashMap<String, Map<String, Map<String, Long>>>();
    private Map<String, Map<String, Map<String, Long>>> currentMetrics = new HashMap<String, Map<String, Map<String, Long>>>();

    private int frequency;
    private int counter = 0;

    public MetricsSnapshotReporterWithAggregation(int frequency) {
        this.frequency = frequency;
    }

    /*
        * Keeps adding the metrics of last 10 seconds to currentMetrics and copies them to lastDurationMetrics when
        * counter equals frequency and flushes current metrics and resets counter.
        *
        * Collects the metrics from HystrixCommandMetrics.
        *
        * Saves in Map form and returns the map when called from HystrixMetricsSnapshotController.
        *
        */
    public void run() {
        counter++;

        for (HystrixCommandMetrics commandMetrics : HystrixCommandMetrics.getInstances()) {
            String commandName = commandMetrics.getCommandGroup().name() + "." + commandMetrics.getCommandKey().name();

            if (currentMetrics.get("HystrixCommand") == null) {
                currentMetrics.put("HystrixCommand", new HashMap<String, Map<String, Long>>());
            }
            Map<String, Long> currStats = currentMetrics.get("HystrixCommand").get(commandName);
            if (currStats == null) {
                currStats = new HashMap<String, Long>();
            }

            HystrixCommandMetrics.HealthCounts healthCounts = commandMetrics.getHealthCounts();

            currStats.put("errorCount", healthCounts.getErrorCount() + zeroIfNull(currStats.get("errorCount")));
            currStats.put("requestCount", healthCounts.getTotalRequests() + zeroIfNull(currStats.get("requestCount")));
            currStats.put("rollingCountFailure", commandMetrics.getRollingCount(HystrixRollingNumberEvent.FAILURE) + zeroIfNull(currStats.get("rollingCountFailure")));
            currStats.put("rollingCountSemaphoreRejected", commandMetrics.getRollingCount(HystrixRollingNumberEvent.SEMAPHORE_REJECTED) + zeroIfNull(currStats.get("rollingCountSemaphoreRejected")));
            currStats.put("rollingCountShortCircuited", commandMetrics.getRollingCount(HystrixRollingNumberEvent.SHORT_CIRCUITED) + zeroIfNull(currStats.get("rollingCountShortCircuited")));
            currStats.put("rollingCountSuccess", commandMetrics.getRollingCount(HystrixRollingNumberEvent.SUCCESS) + zeroIfNull(currStats.get("rollingCountSuccess")));
            currStats.put("rollingCountThreadPoolRejected", commandMetrics.getRollingCount(HystrixRollingNumberEvent.THREAD_POOL_REJECTED) + zeroIfNull(currStats.get("rollingCountThreadPoolRejected")));
            currStats.put("rollingCountTimeout", commandMetrics.getRollingCount(HystrixRollingNumberEvent.TIMEOUT) + zeroIfNull(currStats.get("rollingCountTimeout")));

            /* Divide below with frequency as they are latencies */
            currStats.put("latencyTotal_mean", (long) commandMetrics.getTotalTimeMean()/frequency + zeroIfNull(currStats.get("latencyTotal_mean")));
            currStats.put("0", (long) commandMetrics.getTotalTimePercentile(0)/frequency + zeroIfNull(currStats.get("0")));
            currStats.put("25", (long) commandMetrics.getTotalTimePercentile(25)/frequency + zeroIfNull(currStats.get("25")));
            currStats.put("50", (long) commandMetrics.getTotalTimePercentile(50)/frequency + zeroIfNull(currStats.get("50")));
            currStats.put("75", (long) commandMetrics.getTotalTimePercentile(75)/frequency + zeroIfNull(currStats.get("75")));
            currStats.put("90", (long) commandMetrics.getTotalTimePercentile(90)/frequency + zeroIfNull(currStats.get("90")));
            currStats.put("95", (long) commandMetrics.getTotalTimePercentile(95)/frequency + zeroIfNull(currStats.get("95")));
            currStats.put("99", (long) commandMetrics.getTotalTimePercentile(99)/frequency + zeroIfNull(currStats.get("99")));
            currStats.put("99.5", (long) commandMetrics.getTotalTimePercentile(99.5)/frequency + zeroIfNull(currStats.get("99.5")));
            currStats.put("100", (long) commandMetrics.getTotalTimePercentile(100)/frequency + zeroIfNull(currStats.get("100")));

            currentMetrics.get("HystrixCommand").put(commandName, currStats);
        }


        for (HystrixThreadPoolMetrics commandMetrics : HystrixThreadPoolMetrics.getInstances()) {
            String commandName = commandMetrics.getThreadPoolKey().name();

            if (currentMetrics.get("HystrixThreadPool") == null) {
                currentMetrics.put("HystrixThreadPool", new HashMap<String, Map<String, Long>>());
            }
            Map<String, Long> currStats = currentMetrics.get("HystrixThreadPool").get(commandName);
            if (currStats == null) {
                currStats = new HashMap<String, Long>();
            }

            currStats.put("currentActiveCount", commandMetrics.getCurrentActiveCount().intValue() + zeroIfNull(currStats.get("currentActiveCount")));
            currStats.put("currentQueueSize", (long) commandMetrics.getCurrentQueueSize().intValue() + zeroIfNull(currStats.get("currentQueueSize")));

            currentMetrics.get("HystrixThreadPool").put(commandName, currStats);
        }
        if (counter == frequency) {
                    /* copying metrics to last one min */
            lastDurationMetrics = new HashMap<String, Map<String, Map<String, Long>>>(currentMetrics);
            currentMetrics = new HashMap<String, Map<String, Map<String, Long>>>();
            counter = 0;
        }
    }

    /* returns the metrics collected by running thread */
    public Map<String, Map<String, Map<String, Long>>> getMetricsLastDuration() {
        return lastDurationMetrics;
    }

    private Long zeroIfNull(Long val) {
        return (val == null) ? 0 : val;
    }
}
