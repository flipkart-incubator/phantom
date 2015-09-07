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

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trpr.platform.core.impl.event.AbstractEndpointEventConsumerImpl;
import org.trpr.platform.core.impl.event.PlatformApplicationEvent;
import org.trpr.platform.core.spi.event.EndpointEventConsumer;
import org.trpr.platform.model.event.PlatformEvent;

import com.flipkart.phantom.event.ServiceProxyEvent;
import com.flipkart.phantom.event.ServiceProxyEventProducer;
import com.netflix.hystrix.HystrixEventType;

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
 * <p/>
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
    private static FastDateFormat dateFormatter = FastDateFormat.getInstance("dd-MMM-yyyy_HH:mm:ss.SSS");
    
    /** Map of endpoint URIs and their registered consumers*/
    private Map<String, EndpointEventConsumer> subscriptionConsumers = new HashMap<String, EndpointEventConsumer>();

    /** Implementation of {@link org.trpr.platform.core.impl.event.AbstractEndpointEventConsumerImpl#handlePlatformEvent(org.trpr.platform.model.event.PlatformEvent)} */
    @SuppressWarnings("unchecked")
	@Override
    protected void handlePlatformEvent(final PlatformEvent platformEvent) {
        if (platformEvent instanceof ServiceProxyEvent) {
        	ServiceProxyEvent serviceProxyEvent = (ServiceProxyEvent)platformEvent;
        	String subscriptionKey = ServiceProxyEventProducer.EVENT_PUBLISHING_URI + serviceProxyEvent.getEventType();
        	if (this.subscriptionConsumers.containsKey(subscriptionKey)) {
        		// we pass on this event to the consumer registered to process the event
        		this.subscriptionConsumers.get(subscriptionKey).onApplicationEvent(new PlatformApplicationEvent(platformEvent));
        	} else {
        		log((ServiceProxyEvent) platformEvent);
        	}
        } else {
            LOGGER.warn("Event Not Logged:Non compatible event received. Expecting ServiceProxyEvent Type but received " + platformEvent.getClass());
        }
    }
    
    /**
     * Adds the specified endpoint URI to the subscriptions list of this consumer. Forwards all received matching callbacks to the specified EndpointEventConsumer
     * @param endpointURI the subscription to add to the list of subscriptions
     * @param eventConsumer the EndpointEventConsumer to invoke for all events with matching endpoint URIs.
     */
    public void addSubscriptionAndConsumer(String endpointURI, EndpointEventConsumer eventConsumer) {
    	this.subscriptionConsumers.put(endpointURI, eventConsumer);
    	List<String> allSubscriptions = new LinkedList<String>(Arrays.asList(this.getSubscriptions()));
    	allSubscriptions.add(endpointURI);
    	super.setSubscriptions(allSubscriptions.toArray(new String[0]));
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
        if (events.size() > 0 && !events.contains(HystrixEventType.SUCCESS)) {
            LOGGER.error(
                    "ClientRequestId=" + event.getRequestId() + " " +
                            "Command=" + event.getCommandName() + " " +
                            (events.size() > 0 ? "Events=" + eventsToString(events) + " " : "") +
                            "SentTime=" + getFormattedTimeStamp(event.getRequestSentTime()) + " " +
                            "ReceivedTime=" + getFormattedTimeStamp(event.getRequestReceiveTime()) + " " +
                            "ExecutionStartTime=" + getFormattedTimeStamp(event.getRequestExecutionStartTime()) + " " +
                            "ExecutionEndTime=" + dateFormatter.format(event.getCreatedDate().getTime()) + " " +
                            "EventType=" + event.getEventType() + " " +
                            "EventSource=" + event.getEventSource() + " " +
                            "TimeTaken=" + event.getExecutionTime()
                    , event.getException()
            );
        }
    }

    /**
     * Helper method to get formatted time stamp 
     */
    private String getFormattedTimeStamp(final long requestReceiveTime) {
        if (requestReceiveTime < 0) {
            return String.valueOf(requestReceiveTime);
        } else {
            return dateFormatter.format(new Date(requestReceiveTime));
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
