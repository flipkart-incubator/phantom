package com.flipkart.phantom.runtime.impl.hystrix;

import java.io.IOException;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;

import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.HystrixThreadPoolMetrics;
import com.netflix.hystrix.util.HystrixRollingNumberEvent;

/**
 * The HystrixMetricsSnapshotServlet class is a customization of the Hystrix  {@link com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsStreamServlet}.
 *
 * Its purpose is to publish hystrix metrics as snapshot of last N seconds instead of pushing to stream url.
 *
 *  @author Ishwar Kumar
 * @version 1.0, 13 August 2014
 */

public class HystrixMetricsSnapshotServlet extends HttpServlet {

    /** */
	private static final long serialVersionUID = 1L;

	/**
     * Handle incoming GETs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleRequest(request, response);
    }
    
    /**
     * - send the json response in format {"HystrixCommand":[{Map},{AnotherMap}], "HystrixThreadPool":[{Map},{Map}]}
     * -
     * 
     * @param request
     * @param response
     * @throws java.io.IOException
     */
    private void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final JsonFactory jsonFactory = new JsonFactory();
        StringWriter responseJsonString = new StringWriter();
        JsonGenerator responseJson = jsonFactory.createJsonGenerator(responseJsonString);
        responseJson.writeStartObject();

        // HystrixCommand value is an array of multiple objects; an object for each command
        responseJson.writeArrayFieldStart("HystrixCommand");

        for (HystrixCommandMetrics commandMetrics : HystrixCommandMetrics.getInstances()) {
            responseJson.writeStartObject();
            HystrixCommandKey key = commandMetrics.getCommandKey();

            responseJson.writeStringField("name", key.name());
            responseJson.writeStringField("group", commandMetrics.getCommandGroup().name());

            HystrixCommandMetrics.HealthCounts healthCounts = commandMetrics.getHealthCounts();
            responseJson.writeNumberField("errorCount", healthCounts.getErrorCount());
            responseJson.writeNumberField("requestCount", healthCounts.getTotalRequests());

            responseJson.writeNumberField("rollingCountFailure", commandMetrics.getRollingCount(HystrixRollingNumberEvent.FAILURE));
            responseJson.writeNumberField("rollingCountSemaphoreRejected", commandMetrics.getRollingCount(HystrixRollingNumberEvent.SEMAPHORE_REJECTED));
            responseJson.writeNumberField("rollingCountShortCircuited", commandMetrics.getRollingCount(HystrixRollingNumberEvent.SHORT_CIRCUITED));
            responseJson.writeNumberField("rollingCountSuccess", commandMetrics.getRollingCount(HystrixRollingNumberEvent.SUCCESS));
            responseJson.writeNumberField("rollingCountThreadPoolRejected", commandMetrics.getRollingCount(HystrixRollingNumberEvent.THREAD_POOL_REJECTED));
            responseJson.writeNumberField("rollingCountTimeout", commandMetrics.getRollingCount(HystrixRollingNumberEvent.TIMEOUT));

            responseJson.writeNumberField("latencyTotal_mean", commandMetrics.getTotalTimeMean());
            responseJson.writeObjectFieldStart("latencyTotal");
            responseJson.writeNumberField("0", commandMetrics.getTotalTimePercentile(0));
            responseJson.writeNumberField("25", commandMetrics.getTotalTimePercentile(25));
            responseJson.writeNumberField("50", commandMetrics.getTotalTimePercentile(50));
            responseJson.writeNumberField("75", commandMetrics.getTotalTimePercentile(75));
            responseJson.writeNumberField("90", commandMetrics.getTotalTimePercentile(90));
            responseJson.writeNumberField("95", commandMetrics.getTotalTimePercentile(95));
            responseJson.writeNumberField("99", commandMetrics.getTotalTimePercentile(99));
            responseJson.writeNumberField("99.5", commandMetrics.getTotalTimePercentile(99.5));
            responseJson.writeNumberField("100", commandMetrics.getTotalTimePercentile(100));
            responseJson.writeEndObject();

            responseJson.writeEndObject();
        }
        responseJson.writeEndArray();

        responseJson.writeArrayFieldStart("HystrixThreadPool");
        // thread pool metrics: an array of multiple objects; an object for each method

        for (HystrixThreadPoolMetrics threadPoolMetrics : HystrixThreadPoolMetrics.getInstances()) {
            responseJson.writeStartObject();
            HystrixThreadPoolKey key = threadPoolMetrics.getThreadPoolKey();

            responseJson.writeStringField("name", key.name());

            responseJson.writeNumberField("currentActiveCount", threadPoolMetrics.getCurrentActiveCount().intValue());
            responseJson.writeNumberField("currentQueueSize", threadPoolMetrics.getCurrentQueueSize().intValue());
            responseJson.writeEndObject();
        }
        responseJson.writeEndArray();

        responseJson.writeEndObject();
        responseJson.close();

        /* initialize response */
        response.setHeader("Content-Type", "application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
        response.setHeader("Pragma", "no-cache");

        // we will just write a snapshot to response.
        response.getWriter().println(responseJsonString.getBuffer().toString() + "\n");

        response.flushBuffer();
    }
}