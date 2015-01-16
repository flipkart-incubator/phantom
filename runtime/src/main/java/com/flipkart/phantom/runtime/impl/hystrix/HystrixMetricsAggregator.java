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
package com.flipkart.phantom.runtime.impl.hystrix;

import com.flipkart.phantom.runtime.impl.hystrix.impl.MetricsSnapshotReporterDefault;
import com.flipkart.phantom.runtime.impl.hystrix.impl.MetricsSnapshotReporterWithAggregation;
import org.springframework.beans.factory.InitializingBean;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The <code>HystrixMetricsAggregator</code> provides aggregated metrics of last (10*frequency) seconds.
 * It internally hits {@link com.netflix.hystrix.HystrixCommandMetrics#getInstances()} which provides last 10 seconds metrics
 * It starts a thread only if frequency is more than 1 else it just returns current metrics
 *
 * @author Ishwar Kumar
 * @version 1.0, 15 Jan 2015
 */
public class HystrixMetricsAggregator implements InitializingBean {

    /* by default return aggregate of lastDurationMetrics 1 iterations. */
    private Integer frequency = 1;

    /* Can be set from service-proxy in case of custom implementation */
    MetricsSnapshotReporter metricsSnapshotReporter = null;

    public MetricsSnapshotReporter getMetricsSnapshotReporter() {
        return metricsSnapshotReporter;
    }

    public void setMetricsSnapshotReporter(MetricsSnapshotReporter metricsSnapshotReporter) {
        this.metricsSnapshotReporter = metricsSnapshotReporter;
    }

    public void setFrequency(String fre) {
        try {
            frequency = Integer.valueOf(fre);
        } catch (Exception e) {
            /* consume the exception so that frequency will remain 1(default) so thread wont start */
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (frequency > 1) {
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            metricsSnapshotReporter = new MetricsSnapshotReporterWithAggregation(frequency);
            scheduler.scheduleAtFixedRate((Runnable) metricsSnapshotReporter, 10, 10, TimeUnit.SECONDS);
        } else {
            metricsSnapshotReporter = new MetricsSnapshotReporterDefault();
        }
    }
}
