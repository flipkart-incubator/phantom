package com.flipkart.phantom.event;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixEventType;
import org.trpr.platform.core.spi.event.EndpointEventProducer;

import java.util.Collections;
import java.util.List;

/**
 * The <Code>ServiceProxyEventProducer</Code> class which encodes publishing logic of {@link ServiceProxyEvent}
 * This method used to prepare {@link ServiceProxyEvent} and push it to proper
 * end point based on the source of event.
 *
 * @author amanpreet.singh
 * @version 1.0.0
 * @since 24/10/13 5:44 PM.
 */
public class ServiceProxyEventProducer {

    /** End Point URI Prefix to publish service proxy events */
    private final static String EVENT_PUBLISHING_URI = "evt://com.flipkart.phantom.events.";

    EndpointEventProducer eventProducer;

    /**
     * @param executor      executor object which serviced the request of event being published.
     * @param commandName   Command which executor executed. This corresponds to command name, uri, proxy
     *                      in case of Task Handler,HTTP Handler & Thrift Handler Respectively.
     * @param eventSource   Refers to the class of the executor which executed the request.
     * @param eventType     Defines source of event using {@link ServiceProxyEventType}
     */
    public void publishEvent(HystrixCommand executor, String commandName, Class eventSource, ServiceProxyEventType eventType) {
        List<HystrixEventType> executionEvents = Collections.EMPTY_LIST;
        Exception exception = null;

        /** Executor would be null in case there is a problem finding proper executor for the request. */
        if (executor != null) {
            /**
             * Some Handlers produce events multiple times for a single request. We log event once per request
             * hence we wait until executor marked request as complete.
             * @see com.netflix.hystrix.HystrixCommand#isExecutionComplete()
             */
            if (!executor.isExecutionComplete())
                return;

            executionEvents = executor.getExecutionEvents();
            exception = (Exception) executor.getFailedExecutionException();

        }

        ServiceProxyEvent event = new ServiceProxyEvent(commandName, eventSource.getName(), eventType, executionEvents, exception);
        final String endpointURI = EVENT_PUBLISHING_URI + eventType.name();
        eventProducer.publishEvent(event, endpointURI);
    }

    /** Getter/Setter methods */
    public void setEventProducer(EndpointEventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }
    /** End Getter/Setter methods */
}
