
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

import com.netflix.hystrix.HystrixEventType;
import org.trpr.platform.model.event.PlatformEvent;

import java.util.Calendar;
import java.util.List;

/**
 * This is an extension of {@link org.trpr.platform.model.event.PlatformEvent}
 * which adds few additional fields to the Event.
 * <br/>
 * <b>
 * IMPORTANT- In case of Request TimeOut {@link ServiceProxyEvent#getEventStatus()} is SUCCESS. However {@link ServiceProxyEvent#getHystrixEventList()}
 * would contain a TimeOut Event. In case consumer is interested in TimeOuts, it must check {@link ServiceProxyEvent#getHystrixEventList()} to detect
 * TimeOut Events.
 * </b>
 *
 * @author amanpreet.singh
 * @version 1.0.0
 * @since 24/10/13 5:44 PM.
 */

public class ServiceProxyEvent extends PlatformEvent {
    /** Sequential list of events which executor executed to serve the request. */
    private final List<HystrixEventType> hystrixEventList;

    /** In case of failure this field holds the exception which caused the failure otherwise it is {@code null} */
    private final Exception exception;

    /**
     * Command which executor executed. This corresponds to command name, uri, proxy
     * in case of Task Handler,HTTP Handler & Thrift Handler Respectively.
     */
    private final String commandName;

    //Enum just to denote string constants for event status.
    enum EventStatus {
        SUCCESS, FAILURE
    }

    /**
     * @param commandName      Command which executor executed from which this event was generated.
     * @param eventSource      Name of the executor class which executed this event. In case executor was not found it refers to the class
     *                         responsible for finding appropriate executor.
     * @param eventType        String value which identifies originating Handler of the Event.
     * @param hystrixEventList Sequential list of events which executor executed to serve the request.
     * @param exception        In case of failure this field holds the exception which caused the failure otherwise it is {@code null}
     */
    public ServiceProxyEvent(String commandName, String eventSource, String eventType, List<HystrixEventType> hystrixEventList, Exception exception) {
        /** Inherited Fields */
        this.eventSource = eventSource;
        this.eventType = eventType;
        /** EventStatus is SUCCESS in case of request TimeOuts see class description for more detail */
        this.eventStatus = exception == null ? EventStatus.SUCCESS.name() : EventStatus.FAILURE.name();
        this.eventMessage = exception == null ? EventStatus.SUCCESS.name() : exception.getMessage();
        setCreatedDate(Calendar.getInstance());

        /** Introduced Fields */
        this.hystrixEventList = hystrixEventList;
        this.commandName = commandName;
        this.exception = exception;

    }

    /** Getter/Setter methods */
    public Exception getException() {
        return exception;
    }

    public List<HystrixEventType> getHystrixEventList() {
        return hystrixEventList;
    }

    public String getCommandName() {
        return commandName;
    }
    /** End Getter/Setter methods */
}
