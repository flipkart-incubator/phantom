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
import com.netflix.hystrix.HystrixEventType;
import org.trpr.platform.model.event.PlatformEvent;

import java.util.Calendar;
import java.util.Collections;
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
	
    /** */
	private static final long serialVersionUID = 1L;

	/** Sequential list of events which executor executed to serve the request. */
    private final List<HystrixEventType> hystrixEventList;

    /** In case of failure this field holds the exception which caused the failure otherwise it is {@code null} */
    private final Exception exception;

    /**
     * Command which executor executed. This corresponds to command name, uri, proxy
     * in case of Task Handler,HTTP Handler & Thrift Handler Respectively.
     */
    private final String commandName;

    /** Time it took to execute the command. In case command is not found value is -1. */
    private final int executionTime;

    /** Time at which request is sent */
    private final long requestSentTime;

    /** Time at which request is received */
    private final long requestReceiveTime;

    /** Time at which request starts executing */
    private final long requestExecutionStartTime;

    /** Request Id corresponding to which this event is generated. */
    private final String requestId;

    //Enum just to denote string constants for event status.
    enum EventStatus {
        SUCCESS, FAILURE
    }

    private ServiceProxyEvent(Builder builder) {
        /** Inherited Fields */
        this.eventSource = builder.eventSource;
        this.eventType = builder.eventType;

        /** Introduced Fields */
        this.requestId = builder.requestId;
        this.hystrixEventList = builder.hystrixEventList;
        this.commandName = builder.commandName;
        this.exception = builder.exception;
        this.executionTime = builder.executionTime;
        this.requestSentTime = builder.requestSentTime;
        this.requestReceiveTime = builder.requestReceiveTime;
        this.requestExecutionStartTime = builder.requestExecutionStartTime;

        /** EventStatus is SUCCESS in case of request TimeOuts see class description for more detail */
        this.eventStatus = exception == null ? EventStatus.SUCCESS.name() : EventStatus.FAILURE.name();
        this.eventMessage = exception == null ? EventStatus.SUCCESS.name() : exception.getMessage();
        setCreatedDate(Calendar.getInstance());
    }

    /** Getter methods */
    public Exception getException() {
        return exception;
    }

    public List<HystrixEventType> getHystrixEventList() {
        return hystrixEventList;
    }

    public String getCommandName() {
        return commandName;
    }

    public int getExecutionTime() {
        return executionTime;
    }

    public String getRequestId() {
        return requestId;
    }

    public long getRequestReceiveTime() {
        return requestReceiveTime;
    }

    public long getRequestSentTime() {
        return requestSentTime;
    }

    public long getRequestExecutionStartTime() {
        return requestExecutionStartTime;
    }

    /** End Getter methods */

    public static class Builder {
        /** Mandatory Fields */
        private final String commandName;
        private final String eventType;

        /** Optional Fields */
        private String requestId = null;
        private int executionTime = -1;
        private long requestSentTime = -1;
        private long requestReceiveTime = -1;
        private long requestExecutionStartTime = -1;
        private Exception exception = null;
        private String eventSource = "unspecified";
        private List<HystrixEventType> hystrixEventList = Collections.emptyList();

        /**
         * @param commandName Command which executor executed from which this event was generated.
         * @param eventType   Refer to {@link org.trpr.platform.model.event.PlatformEvent#eventType}
         */
        public Builder(String commandName, String eventType) {
            this.commandName = commandName;
            this.eventType = eventType;
        }

        /**
         * @param eventSource Name of the executor class which executed this event. In case executor was not found it refers to the class
         *                    responsible for finding appropriate executor.
         */
        public Builder withEventSource(String eventSource) {
            this.eventSource = eventSource;
            return this;
        }

        /**
         * @param hystrixEventList Sequential list of events which executor executed to serve the request.
         */
        public Builder withEventList(List<HystrixEventType> hystrixEventList) {
            this.hystrixEventList = hystrixEventList;
            return this;
        }

        /**
         * @param exception In case of failure this field holds the exception which caused the failure.
         */
        public Builder withException(Exception exception) {
            this.exception = exception;
            return this;
        }

        /**
         * @param executionTime Time it took to execute the command. In case command is not found value is -1.
         */
        public Builder withExecutionTime(int executionTime) {
            this.executionTime = executionTime;
            return this;
        }

        /**
         * @param requestId Request Id corresponding to which this event is generated.
         */
        public Builder withRequestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        /**
         * @param sentTime Time at which request is sent.
         */
        public Builder withRequestSentTime(long sentTime) {
            this.requestSentTime = sentTime;
            return this;
        }

        /**
         * @param receiveTime Time at which request is received.
         */
        public Builder withRequestReceiveTime(long receiveTime) {
            this.requestReceiveTime = receiveTime;
            return this;
        }

        /**
         * @param executionStartTime Time at which actual processing starts for the request.
         */
        public Builder withRequestExecutionStartTime(long executionStartTime) {
            this.requestExecutionStartTime = executionStartTime;
            return this;
        }

        /**
         * Copies various parameters like execution events,time and any exception after command execution.<br/><br/>
         * <b>Should be called only after execution of command completes</b>
         * @param executor Executor used to execute this request.
         * @return
         */
        @SuppressWarnings({ "rawtypes", "unchecked" })
		public Builder withCommandData(Executor executor) {
            HystrixCommand command = (HystrixCommand) executor;
            withEventList(command.getExecutionEvents())
                    .withExecutionTime(command.getExecutionTimeInMilliseconds())
                    .withException((Exception) command.getFailedExecutionException());
            return this;
        }

        public ServiceProxyEvent build() {
            return new ServiceProxyEvent(this);
        }
    }
}
