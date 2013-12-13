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

package com.flipkart.phantom.event;

import com.flipkart.phantom.task.spi.Executor;
import com.netflix.hystrix.HystrixCommand;
import org.trpr.platform.core.spi.event.EndpointEventProducer;

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

    private EndpointEventProducer eventProducer;

    /**
     * @param executor    executor object which serviced the request of event being published.
     * @param commandName Command which executor executed. This corresponds to command name, uri, proxy
     *                    in case of Task Handler,HTTP Handler & Thrift Handler Respectively.
     * @param eventSource Refers to the class of the executor which executed the request.
     * @param eventType   String value which identifies originating Handler of the Event. If this parameter
     *                    starts with "ASYNC" check of {@link com.netflix.hystrix.HystrixCommand#isExecutionComplete()}
     *                      is skipped before publishing the event.
     * @param requestID   Id of request for which this event is generated.
     */
    public void publishEvent(Executor executor, String commandName, Class eventSource, String eventType, String requestID) {
        ServiceProxyEvent.Builder eventBuilder = new ServiceProxyEvent.Builder(commandName, eventType);
        eventBuilder.withEventSource(eventSource.getName()).withRequestId(requestID);
        /** Executor would be null in case there is a problem finding proper executor for the request. */
        if (executor != null) {
            HystrixCommand command = (HystrixCommand) executor;

            /**
             *  For ASYNC executors {@link com.netflix.hystrix.HystrixCommand#isExecutionComplete()}
             *  would be false at the time of scheduling. Thus we publish all scheduled events in case of
             *  ASYNC executors.
             */
            /** TODO://Improve way Async Publishing is Handled */
            if (!eventType.startsWith("ASYNC")) {
                /**
                 * Some Handlers produce events multiple times for a single request. We log event once per request
                 * hence we wait until executor marked request as complete.
                 * @see com.netflix.hystrix.HystrixCommand#isExecutionComplete()
                 */
                if (!command.isExecutionComplete())
                    return;
            }

            eventBuilder.withEventList(command.getExecutionEvents())
                    .withExecutionTime(command.getExecutionTimeInMilliseconds())
                    .withException((Exception) command.getFailedExecutionException());
        }

        final String endpointURI = EVENT_PUBLISHING_URI + eventType;
        eventProducer.publishEvent(eventBuilder.build(), endpointURI);
    }

    /**
     * @param executor    executor object which serviced the request of event being published.
     * @param commandName Command which executor executed. This corresponds to command name, uri, proxy
     *                    in case of Task Handler,HTTP Handler & Thrift Handler Respectively.
     * @param eventSource Refers to the class of the executor which executed the request.
     * @param eventType   String value which identifies originating Handler of the Event. If this parameter
     *                    starts with "ASYNC" check of {@link com.netflix.hystrix.HystrixCommand#isExecutionComplete()}
     *                      is skipped before publishing the event.
     */
    public void publishEvent(Executor executor, String commandName, Class eventSource, String eventType) {
        publishEvent(executor,commandName,eventSource,eventType,null);
    }

    /** Getter/Setter methods */
    public void setEventProducer(EndpointEventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }
    /** End Getter/Setter methods */
}
