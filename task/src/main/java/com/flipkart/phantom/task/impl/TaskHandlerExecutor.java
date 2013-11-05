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

import com.flipkart.phantom.task.spi.Executor;
import com.flipkart.phantom.task.spi.TaskContext;
import com.netflix.hystrix.*;
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * <code>TaskHandlerExecutor</code> is an extension of {@link HystrixCommand}. It is essentially a
 * wrapper around {@link TaskHandler}, providing a means for the TaskHandler to to be called using
 * a Hystrix Command.
 *
 * @author devashishshankar
 * @version 1.0, 19th March, 2013
 */
public class TaskHandlerExecutor extends HystrixCommand<TaskResult> implements Executor{

    /** TaskResult message constants */
    public static final String NO_RESULT = "The command returned no result";
    public static final String ASYNC_QUEUED = "The command dispatched for async execution";

    /** Logger for this class*/
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskHandlerExecutor.class);

    /** The default Hystrix group to which the command belongs, unless otherwise mentioned*/
    public static final String DEFAULT_HYSTRIX_GROUP = "DEFAULT_GROUP";

    /** The default Hystrix Thread pool to which this command belongs, unless otherwise mentioned */
    public static final String DEFAULT_HYSTRIX_THREAD_POOL = "DEFAULT_THREAD_POOL";

    /** The default Hystrix Thread pool to which this command belongs, unless otherwise mentioned */
    public static final int DEFAULT_HYSTRIX_THREAD_POOL_SIZE = 10;

    /** The {@link TaskHandler} or {@link HystrixTaskHandler} instance which this Command wraps around */
    protected TaskHandler taskHandler;

    /** The params required to execute a TaskHandler */
    protected TaskContext taskContext;

    /** The command for which this task is executed */
    protected String command;

    /** The parameters which are utilized by the task for execution */
    protected Map<String,String> params;

    /** Data which is utilized by the task for execution */
    protected byte[] data;

    /**
     * Basic constructor for {@link TaskHandler}. The Hystrix command name is commandName. The group name is the Handler Name
     * (HystrixTaskHandler#getName)
     *
     * @param taskHandler The taskHandler to be wrapped
     * @param taskContext The context (Unique context required by Handlers to communicate with the container.)
     * @param commandName name of the command
     * @param timeout the timeout for the Hystrix thread
     * @param threadPoolName Name of the thread pool
     * @param threadPoolSize core size of the thread pool
     * @param taskRequestWrapper requestWrapper containing the data and the parameters
     */
    protected TaskHandlerExecutor(TaskHandler taskHandler, TaskContext taskContext, String commandName, int timeout,
                                  String threadPoolName, int threadPoolSize, TaskRequestWrapper taskRequestWrapper ) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(taskHandler.getName()))
                .andCommandKey(HystrixCommandKey.Factory.asKey(commandName))
                .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(threadPoolName))
                .andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter().withCoreSize(threadPoolSize))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter().withExecutionIsolationThreadTimeoutInMilliseconds(timeout)));
        this.taskHandler = taskHandler;
        this.taskContext = taskContext;
        this.command = commandName;
        this.data = taskRequestWrapper.getData();
        this.params = taskRequestWrapper.getParams();
    }

    /**
     * Constructor for {@link TaskHandler} using Semaphore isolation. The Hystrix command name is commandName. The group name is the Handler Name
     * (HystrixTaskHandler#getName)
     *
     * @param taskHandler The taskHandler to be wrapped
     * @param taskContext The context (Unique context required by Handlers to communicate with the container.)
     * @param commandName name of the command
     * @param taskRequestWrapper requestWrapper containing the data and the parameters
     */
    protected TaskHandlerExecutor(TaskHandler taskHandler, TaskContext taskContext, String commandName,TaskRequestWrapper taskRequestWrapper ) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(taskHandler.getName()))
                .andCommandKey(HystrixCommandKey.Factory.asKey(commandName))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withExecutionIsolationStrategy(ExecutionIsolationStrategy.SEMAPHORE)))
        ;
        this.taskHandler = taskHandler;
        this.taskContext = taskContext;
        this.command = commandName;
        this.data = taskRequestWrapper.getData();
        this.params = taskRequestWrapper.getParams();
    }

    /**
     * Constructor for TaskHandlerExecutor run through Default Hystrix Thread Pool ({@link TaskHandlerExecutor#DEFAULT_HYSTRIX_THREAD_POOL})
     * @param taskHandler The taskHandler to be wrapped
     * @param taskContext The context (Unique context required by Handlers to communicate with the container.)
     * @param commandName name of the command
     * @param executorTimeout the timeout for the Hystrix thread
     * @param taskRequestWrapper requestWrapper containing the data and the parameters
     */
    public TaskHandlerExecutor(TaskHandler taskHandler, TaskContext taskContext, String commandName, int executorTimeout,TaskRequestWrapper taskRequestWrapper) {
        this(taskHandler,taskContext,commandName,executorTimeout,DEFAULT_HYSTRIX_THREAD_POOL,DEFAULT_HYSTRIX_THREAD_POOL_SIZE,taskRequestWrapper);
    }

    /**
     * Interface method implementation. @see HystrixCommand#run()
     * @throws Exception
     */
    @Override
    protected TaskResult run() throws Exception {
        TaskResult result = this.taskHandler.execute(taskContext, command, params, data);
        if(result==null) {
            return new TaskResult(true,TaskHandlerExecutor.NO_RESULT);
        }
        if(result.isSuccess()==false) {
            throw new RuntimeException("Command returned FALSE: "+(result==null?"":result.getMessage()));
        }
        return result;
    }

    /**
     * Interface method implementation. @see HystrixCommand#getFallback()
     */
    @Override
    protected TaskResult getFallback() {
        if(this.taskHandler instanceof HystrixTaskHandler) {
            HystrixTaskHandler hystrixTaskHandler = (HystrixTaskHandler) this.taskHandler;
            return hystrixTaskHandler.getFallBack(taskContext, command, params, data);
        }
        return null;
    }

    /**
     * First It checks the call invocation type has been overridden for the command,
     * if not, it defaults to task handler call invocation type.
     *
     * @return  callInvocation Type
     */
    public int getCallInvocationType() {
        if(this.taskHandler.getCallInvocationTypePerCommand() != null) {
            Integer callInvocationType = this.taskHandler.getCallInvocationTypePerCommand().get(this.command);
            if(callInvocationType != null) {
                return callInvocationType;
            }
        }
        return this.taskHandler.getCallInvocationType();
    }
    /** End Getter/Setter methods */
}
