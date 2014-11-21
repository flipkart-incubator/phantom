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
package com.flipkart.phantom.task.spi;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.kristofa.brave.FixedSampleRateTraceFilter;
import com.github.kristofa.brave.TraceFilter;

/**
 * The abstract handler class, which has functions necessary only for basic proxy functions
 * @author kartikbu
 * @version 1.0
 * @created 7/8/13 10:05 PM
 */
abstract public class AbstractHandler {

    /** Constants to denote command invocation types*/
    public static final int SYNC_CALL = 0;
    public static final int ASYNC_CALL = 1;
    
	/**
	 * Constants that influence continued handler init activity based on the outcome of execution of init on a handler 
	 */
	public static final int CONTINUE_INIT = 0;
	public static final int VETO_INIT = 999999;    

    /** The status showing the TaskHandler is inited and ready to use */
    public static final int ACTIVE = 1;
    /** The status showing the TaskHandler is not inted/has been shutdown and should not be used */
    public static final int INACTIVE = 0;
    
    /** The default value for tracing frequency. This value indicates that tracing if OFF*/
    public static final TraceFilter NO_TRACING = new FixedSampleRateTraceFilter(-1);

    /** The default command invocation type for this AbstractHandler*/
    private int callInvocationType = AbstractHandler.SYNC_CALL;

    /** This provides the invocation type for each command in a Task Handler. */
    private Map<String,Integer> callInvocationTypePerCommand = new HashMap<String, Integer>();
    
    private int initOutcomeStatus = AbstractHandler.VETO_INIT;

    /** The status of this handler (active/inactive) */
    private AtomicInteger status = new AtomicInteger(INACTIVE);
    
    /** The request tracing frequency for this handler*/
    private TraceFilter traceFilter = NO_TRACING;

    /**
     * Method which returns the name of the handler
     * @return String name
     */
    abstract public String getName();

    /**
     * Method which returns the type of handler
     * Used in dashboard
     * @return String type
     */
    abstract public String getType();

    /**
     * Method which returns a short description of the handler
     * Used in dashboard
     * @return String description
     */
    abstract public String getDetails();

    /**
     * Init lifecycle method
     * @param context context in which init is happening
     */
    abstract public void init(TaskContext context) throws Exception;

    /**
     * Shutdown lifecycle method
     * @param context context in which shutdown is happening
     */
    abstract public void shutdown(TaskContext context) throws Exception;

    /**
     * Set the handler status to Active
     */
    public final void activate() {
        this.status.set(AbstractHandler.ACTIVE);
    }

    /**
     * Set the handler status to Inactive
     */
    public final void deactivate() {
        this.status.set(AbstractHandler.INACTIVE);
    }

    /**
     * Check if the handler is active
     * @return boolean
     */
    public final boolean isActive() {
        return this.status.get() == AbstractHandler.ACTIVE;
    }

    /**
     * The default call invocation type for this handler
     * @return the call invocation type identifier
     */
    public int getCallInvocationType() {
        return this.callInvocationType;
    }

    /**
     * Sets the call invocation type for this handler
     * @param callInvocationType the call invocation type identifier
     */
    public void setCallInvocationType(int callInvocationType) {
        this.callInvocationType = callInvocationType;
    }

    /**
     * The callInvocationTypePerCommand for this handler
     * @return the callInvocationTypePerCommand Map
     */
    public Map<String, Integer> getCallInvocationTypePerCommand(){
        return callInvocationTypePerCommand;
    }

    /**
     * Sets the callInvocationTypePerCommand for this handler
     * @param callInvocationTypePerCommand Map of command Vs Call Invocation Type
     */
    public void setCallInvocationTypePerCommand(Map<String, Integer> callInvocationTypePerCommand) {
        this.callInvocationTypePerCommand = callInvocationTypePerCommand;
    }

    /** Getter/Setter methods*/
	public int getInitOutcomeStatus() {
		return initOutcomeStatus;
	}
	public void setInitOutcomeStatus(int initOutcomeStatus) {
		this.initOutcomeStatus = initOutcomeStatus;
	}
	public TraceFilter getTraceFilter() {
		return traceFilter;
	}
	public void setTraceFilter(TraceFilter traceFilter) {
		this.traceFilter = traceFilter;
	}
    
}
