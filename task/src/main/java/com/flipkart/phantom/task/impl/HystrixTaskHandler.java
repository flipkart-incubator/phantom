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

package com.flipkart.phantom.task.impl;

import java.util.HashMap;
import java.util.Map;

import com.flipkart.phantom.task.spi.Decoder;
import com.flipkart.phantom.task.spi.TaskContext;
import com.flipkart.phantom.task.spi.TaskRequestWrapper;
import com.flipkart.phantom.task.spi.TaskResult;
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;

/**
 * An extension of {@link TaskHandler}. Provides methods to additionally set/customize Hytrix Command properties {https://github.com/Netflix/Hystrix}.
 * Uses {@link TaskHandler#getName()} as the HystrixCommand name for display on the dashboard.
 *
 * @author devashishshankar
 * @version 1.0, 19 March, 2013
 */
public abstract class HystrixTaskHandler extends TaskHandler {

    /** The default executor timeout in millis*/
    public static final int DEFAULT_EXECUTOR_TIMEOUT = 1000;

    /**
     * These can be used to limit the maximum concurrent requests for a Proxy or a command
     * The key will be the Proxy name optionally overridden by a command name
     * The value will be the concurrent pool size.
     * If isolation strategy is @ExecutionIsolationStrategy#Thread then this param is used as a thread-concurrency pool
     * as otherwise if the isolation strategy is @ExecutionIsolationStrategy#Semaphore , then this param is used for semaphore-concurrency pool
     * (If no. of concurrent requests for this concurrent pool exceed this value, they will be rejected by Hystrix)
     * Note: the incoming request should have a "pool" param for it to be routed to the correct Thread Pool. If it doesn't,
     * the default command level thread pool will be used. (It's core size is 10 by default)
     */
    private Map<String,Integer> concurrentPoolSizeParams = new HashMap<String,Integer>();

    /**
     * These can be used to limit the maximum concurrent requests at a command level.
     * The key will be the HystrixCommand name. It should be a valid commandName. (Else an exception is thrown)
     * The value will be the thread pool size. 
     * Note: commandPoolSizeParams override concurrentPoolSizeParams. commandPoolSizeParams will only be applied if there is no
     * "pool" param in incoming requests.
     * (If no. of concurrent requests for this thread pool exceed this value, they will be rejected by Hystrix)
     * If this property is not set for a command, default Hystrix Value for thread pool size(10) will be used.
     */
    private Map<String,Integer> commandPoolSizeParams = new HashMap<String,Integer>();

    /**
     * Map of command names and their respective executor timeouts in milliseconds
     */
    protected Map<String,Integer> executorTimeouts = new HashMap<String, Integer>();

    /**
     * This method will be executed if execute() fails.
     * @param command the command used
     * @param params thrift parameters
     * @param data extra data if any
     * @return response
     */
    public abstract <S> TaskResult<byte[]> getFallBack(TaskContext taskContext, String command, Map<String,String> params, S data);

    /**
     * Returns null. Sub-Classes should override it, if they need fallback functionality.
     * @param taskContext
     * @param command
     * @param taskRequestWrapper
     * @param decoder
     * @param <T>
     * @return
     * @throws RuntimeException
     */
    public <T, S> TaskResult<T> getFallBack(TaskContext taskContext, String command, TaskRequestWrapper<S> taskRequestWrapper,Decoder<T> decoder) throws RuntimeException {
        return null;
    }

    /**
     * Call to optionally release any resources used to create the specified response. Useful when Hystrix command
     * timeouts result in the call being aborted. This callback is intended to be used for freeing up underlying connections/resources.  
     * @param taskResult
     */
    public <T> void releaseResources(TaskResult<T> taskResult) {
    	// do nothing
    }

    /**
     * Return the ExecutionIsolationStrategy. Thread is the default.
     */
    public ExecutionIsolationStrategy getIsolationStrategy() {
        return ExecutionIsolationStrategy.THREAD;
    }

    /** Getter/Setter methods */
    /**
     * Returns a command specific executor timeout. Default implementation returns {@value HystrixTaskHandler#DEFAULT_EXECUTOR_TIMEOUT}
     * @param commandName the command name being executed
     * @return the executor timeout value in milli seconds
     */
    public int getExecutorTimeout(String commandName) {
        Integer timeout = this.getExecutorTimeouts().get(commandName);
        return timeout != null ? timeout : HystrixTaskHandler.DEFAULT_EXECUTOR_TIMEOUT;
    }
    public Map<String,Integer> getCommandPoolSizeParams() {
        return commandPoolSizeParams;
    }
    public void setCommandPoolSizeParams(Map<String,Integer> commandPoolSizeParams) {
        this.commandPoolSizeParams = commandPoolSizeParams;
    }
    public Map<String,Integer> getConcurrentPoolSizeParams() {
        return concurrentPoolSizeParams;
    }
    public void setConcurrentPoolSizeParams(Map<String,Integer> concurrentPoolSizeParams) {
        this.concurrentPoolSizeParams = concurrentPoolSizeParams;
    }
    public Map<String, Integer> getExecutorTimeouts() {
        return this.executorTimeouts;
    }
    public void setExecutorTimeouts(Map<String, Integer> executorTimeouts) {
        this.executorTimeouts = executorTimeouts;
    }
    /** End Getter/Setter methods */
}