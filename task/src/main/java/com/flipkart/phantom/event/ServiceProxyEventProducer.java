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
     * Publishes service proxy event to appropriate end point based on event type.
     * @param event Service Proxy event to be published.
     */
    public void publishEvent(ServiceProxyEvent event) {
        final String endpointURI = EVENT_PUBLISHING_URI + event.getEventType();
        eventProducer.publishEvent(event, endpointURI);
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
    }

    /** Getter/Setter methods */
    public void setEventProducer(EndpointEventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }
    /** End Getter/Setter methods */
}
