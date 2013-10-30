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
package com.flipkart.phantom.event.consumer;

import com.flipkart.phantom.event.ServiceProxyEvent;
import com.netflix.hystrix.HystrixEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trpr.platform.core.impl.event.AbstractEndpointEventConsumerImpl;
import org.trpr.platform.model.event.PlatformEvent;

import java.util.List;

/**
 * ServiceProxyEvent Consumer to log every hystrix command which did not return only SUCCESS as an event.
 * This can happen for a number of reasons such as:
 * - Main execution failed, fallback succeeded. Failure can happen because of:
 * - Command Timeout
 * - Thread Rejection
 * - Exception
 * - Open Circuit
 * - Main execution failed and fallback also failed. Failure reasons are similar.
 * This does not log if main execution succeeds.
 *
 * To enable this request logger, declare it in application context with
 * events to log as subscriptions.
 *
 * @author kartikbu
 * @author amanpreet.singh
 * @version 1.0
 * @created 10/10/13 5:48 PM
 */
public class RequestLogger extends AbstractEndpointEventConsumerImpl {

    /** Logger for this class */
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLogger.class);

    /** Implementation of {@link org.trpr.platform.core.impl.event.AbstractEndpointEventConsumerImpl#handlePlatformEvent(org.trpr.platform.model.event.PlatformEvent)} */
    @Override
    protected void handlePlatformEvent(final PlatformEvent platformEvent) {
        if (platformEvent instanceof ServiceProxyEvent) {
            log((ServiceProxyEvent) platformEvent);
        } else
            LOGGER.warn("Event Not Logged:Non compatible event received. Expecting ServiceProxyEvent Type but received " + platformEvent.getClass());

    }

    /**
     * Writes a line of log describing the command. Logs the following details:
     * - Command Name
     * - Events (comma separated)
     * - TimeStamp when event occurred
     * - EventType describing source of event based on origination handler @see {@link com.flipkart.phantom.event.ServiceProxyEvent}
     * - EventSource Actual Handler responsible for executing the request
     * <p/>
     * Also prints Exception which caused this request to fail.
     *
     * @param event The event published by {@link com.flipkart.phantom.event.ServiceProxyEventProducer}
     */
    private void log(final ServiceProxyEvent event) {
        List<HystrixEventType> events = event.getHystrixEventList();
        if (events.size() > 1 || !events.contains(HystrixEventType.SUCCESS)) {
            LOGGER.error(
                    "Command=" + event.getCommandName() + " " +
                            (events.size() > 0 ? "Events=" + eventsToString(events) + " " : "") +
                            "TimeStamp=" + event.getCreatedDate().getTime() + " " +
                            "EventType=" + event.getEventType() + " " +
                            "EventSource=" + event.getEventSource()
                    , event.getException()
            );
        }
    }

    /**
     * Helper method to comma-separate the list of events
     *
     * @param events The list of events happened during hystrix command execution
     */
    private static String eventsToString(List<HystrixEventType> events) {
        String joined = "";
        boolean first = true;
        for (HystrixEventType event : events) {
            if (first) {
                first = false;
            } else {
                joined += ",";
            }
            joined += event.name();
        }
        return joined;
    }
}
