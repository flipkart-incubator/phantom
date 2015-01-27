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

package com.flipkart.phantom.runtime.impl.spring.web;

import com.flipkart.phantom.runtime.impl.hystrix.HystrixMetricsAggregator;
import com.flipkart.phantom.task.spi.AbstractHandler;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

/**
 * The <code>HystrixMetricsSnapshotController</code> is a controller for providing Hystrix snapshot metrics.
 * <p/>
 * Class summary from original source, modified suitably for package name changes:
 * <p/>
 * provides Hystrix metrics snapshot of last 10*N seconds as per configuration in
 * {@link com.flipkart.phantom.runtime.impl.hystrix.HystrixMetricsAggregator#frequency} in json format.
 * <p/>
 * Install by:
 * <p/>
 * 1) Including runtime-*.jar in your classpath.
 * <p/>
 * 2) Adding the following to controller-context.xml:
 * <pre>{@code
 * <bean class="com.flipkart.phantom.runtime.impl.spring.web.HystrixMetricsSnapshotController">
 *     <property name="hystrixMetricsAggregator" ref="hystrixMetricsAggregator"/>
 * </bean>
 * } </pre>
 *
 * @author Ishwar Kumar
 * @version 1.0, 15 Jan 2015
 */
@Controller
public class HystrixMetricsSnapshotController<T extends AbstractHandler> {

    /**
     * Logger instance for this class
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(HystrixMetricsSnapshotController.class);

    /* instance of aggregator which can be a thread based implementation in case of frequency > 1
     * or else a stand alone implementation which just returns current metrics from HystrixCommandMetrics */
    HystrixMetricsAggregator hystrixMetricsAggregator = null;

    /**
     * Controller for providing the last 10*N seconds metrics snapshot
     * @throws java.io.IOException
     * @return json response
     */
    @RequestMapping(value = {"/hystrix.snapshot.global"}, method = RequestMethod.GET)
    public
    @ResponseBody
    String handleRequest(ModelMap model, HttpServletRequest request) throws IOException {
        final JsonFactory jsonFactory = new JsonFactory();
        StringWriter responseJsonString = new StringWriter();
        JsonGenerator responseJson = jsonFactory.createJsonGenerator(responseJsonString);
        responseJson.writeStartObject();

        /* Get the metrics of last duration from Aggregator */
        Map<String, Map<String, Map<String, Long>>> lastOneMinuteMetrics = hystrixMetricsAggregator.getMetricsSnapshotReporter().getMetricsLastDuration();
        /* HystrixCommand value is an array of multiple objects; an object for each command */
        responseJson.writeObjectFieldStart("HystrixCommand");

        if (lastOneMinuteMetrics != null && lastOneMinuteMetrics.get("HystrixCommand") != null) {
            for (String commandName : lastOneMinuteMetrics.get("HystrixCommand").keySet()) {
                Map<String, Long> commandMetrics = lastOneMinuteMetrics.get("HystrixCommand").get(commandName);

                String a[] = commandName.split("\\.");
                responseJson.writeObjectFieldStart(commandName);

                responseJson.writeStringField("name", a[1]);
                responseJson.writeStringField("group", a[0]);

                responseJson.writeNumberField("errorCount", commandMetrics.get("errorCount"));
                responseJson.writeNumberField("requestCount", commandMetrics.get("requestCount"));
                if (commandMetrics.get("requestCount") > 0)
                    responseJson.writeNumberField("errorPercent", 100.0 * commandMetrics.get("errorCount") / commandMetrics.get("requestCount"));
                else
                    responseJson.writeNumberField("errorPercent", 0.);

                responseJson.writeNumberField("rollingCountFailure", commandMetrics.get("rollingCountFailure"));
                responseJson.writeNumberField("rollingCountSemaphoreRejected", commandMetrics.get("rollingCountSemaphoreRejected"));
                responseJson.writeNumberField("rollingCountShortCircuited", commandMetrics.get("rollingCountShortCircuited"));
                responseJson.writeNumberField("rollingCountThreadPoolRejected", commandMetrics.get("rollingCountThreadPoolRejected"));
                responseJson.writeNumberField("rollingCountSuccess", commandMetrics.get("rollingCountSuccess"));
                responseJson.writeNumberField("rollingCountTimeout", commandMetrics.get("rollingCountTimeout"));

                responseJson.writeNumberField("latencyTotal_mean", commandMetrics.get("latencyTotal_mean"));
                responseJson.writeObjectFieldStart("latencyTotal");
                responseJson.writeNumberField("0", commandMetrics.get("0"));
                responseJson.writeNumberField("25", commandMetrics.get("25"));
                responseJson.writeNumberField("50", commandMetrics.get("50"));
                responseJson.writeNumberField("75", commandMetrics.get("75"));
                responseJson.writeNumberField("90", commandMetrics.get("90"));
                responseJson.writeNumberField("95", commandMetrics.get("95"));
                responseJson.writeNumberField("99", commandMetrics.get("99"));
                responseJson.writeNumberField("99.5", commandMetrics.get("99.5"));
                responseJson.writeNumberField("100", commandMetrics.get("100"));
                responseJson.writeEndObject();

                responseJson.writeEndObject();
            }
        }
        responseJson.writeEndObject();

        responseJson.writeObjectFieldStart("HystrixThreadPool");
        /* thread pool metrics: an array of multiple objects; an object for each method */

        if (lastOneMinuteMetrics != null && lastOneMinuteMetrics.get("HystrixThreadPool") != null) {
            for (String commandName : lastOneMinuteMetrics.get("HystrixThreadPool").keySet()) {
                responseJson.writeObjectFieldStart(commandName);

                responseJson.writeStringField("name", commandName);

                responseJson.writeNumberField("currentActiveCount", lastOneMinuteMetrics.get("HystrixThreadPool").get(commandName).get("currentActiveCount"));
                responseJson.writeNumberField("currentQueueSize", lastOneMinuteMetrics.get("HystrixThreadPool").get(commandName).get("currentQueueSize"));
                responseJson.writeEndObject();
            }
        }
        responseJson.writeEndObject();

        responseJson.writeEndObject();
        responseJson.close();

        return responseJsonString.getBuffer().toString() + "\n";
    }

    public void setHystrixMetricsAggregator(HystrixMetricsAggregator hystrixMetricsAggregator) {
        this.hystrixMetricsAggregator = hystrixMetricsAggregator;
    }
}