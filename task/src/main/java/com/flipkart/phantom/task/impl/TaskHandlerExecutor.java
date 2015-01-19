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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flipkart.phantom.event.ServiceProxyEvent;
import com.flipkart.phantom.task.spi.Decoder;
import com.flipkart.phantom.task.spi.Executor;
import com.flipkart.phantom.task.spi.TaskContext;
import com.flipkart.phantom.task.spi.TaskRequestWrapper;
import com.flipkart.phantom.task.spi.TaskResult;
import com.flipkart.phantom.task.spi.interceptor.RequestInterceptor;
import com.flipkart.phantom.task.spi.interceptor.ResponseInterceptor;
import com.github.kristofa.brave.Brave;
import com.google.common.base.Optional;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.HystrixThreadPoolProperties;

/**
 * <code>TaskHandlerExecutor</code> is an extension of {@link HystrixCommand}. It is essentially a
 * wrapper around {@link TaskHandler}, providing a means for the TaskHandler to to be called using
 * a Hystrix Command.
 *
 * @author devashishshankar
 * @version 1.0, 19th March, 2013
 */
@SuppressWarnings("rawtypes")
public class TaskHandlerExecutor extends HystrixCommand<TaskResult> implements Executor<TaskRequestWrapper, TaskResult> {

    /** Log instance for this class */
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskHandlerExecutor.class);

    /** TaskResult message constants */
    public static final String NO_RESULT = "The command returned no result";
    public static final String ASYNC_QUEUED = "The command dispatched for async execution";

    /** The default Hystrix group to which the command belongs, unless otherwise mentioned*/
    public static final String DEFAULT_HYSTRIX_GROUP = "DEFAULT_GROUP";

    /** The default Hystrix Thread pool to which this command belongs, unless otherwise mentioned */
    public static final String DEFAULT_HYSTRIX_THREAD_POOL = "DEFAULT_THREAD_POOL";

    /** The default Hystrix Thread pool to which this command belongs, unless otherwise mentioned */
    public static final int DEFAULT_HYSTRIX_THREAD_POOL_SIZE = 10;

    /** Event Type for publishing all events which are generated here */
    private final static String COMMAND_HANDLER = "COMMAND_HANDLER";

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

    /* Task Request Wrapper */
    protected TaskRequestWrapper taskRequestWrapper;

    /* Decoder to decode requests */
    protected Decoder decoder;

    /** Event which records various paramenters of this request execution & published later */
    protected ServiceProxyEvent.Builder eventBuilder;
    
    /** List of request and response interceptors */
    private List<RequestInterceptor<TaskRequestWrapper>> requestInterceptors = new LinkedList<RequestInterceptor<TaskRequestWrapper>>();
    private List<ResponseInterceptor<TaskResult>> responseInterceptors = new LinkedList<ResponseInterceptor<TaskResult>>();

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
                .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(taskHandler.getVersionedThreadPoolName(threadPoolName)))
                .andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter().withCoreSize(threadPoolSize))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter().withExecutionIsolationThreadTimeoutInMilliseconds(timeout)));
        this.taskHandler = taskHandler;
        this.taskContext = taskContext;
        this.command = commandName;
        this.data = taskRequestWrapper.getData();
        this.params = taskRequestWrapper.getParams();
        this.taskRequestWrapper = taskRequestWrapper;
        this.eventBuilder = new ServiceProxyEvent.Builder(commandName, COMMAND_HANDLER);
    }

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
     * @param decoder Decoder sent by the Client
     */
    protected TaskHandlerExecutor(TaskHandler taskHandler, TaskContext taskContext, String commandName, int timeout,
                                  String threadPoolName, int threadPoolSize, TaskRequestWrapper taskRequestWrapper , Decoder decoder ) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(taskHandler.getName()))
                .andCommandKey(HystrixCommandKey.Factory.asKey(commandName))
                .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(taskHandler.getVersionedThreadPoolName(threadPoolName)))
                .andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter().withCoreSize(threadPoolSize))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter().withExecutionIsolationThreadTimeoutInMilliseconds(timeout)));
        this.taskHandler = taskHandler;
        this.taskContext = taskContext;
        this.command = commandName;
        this.data = taskRequestWrapper.getData();
        this.params = taskRequestWrapper.getParams();
        this.taskRequestWrapper = taskRequestWrapper;
        this.eventBuilder = new ServiceProxyEvent.Builder(commandName, COMMAND_HANDLER);
        this.decoder = decoder;
    }

    /**
     * Constructor for {@link TaskHandler} using Semaphore isolation. The Hystrix command name is commandName. The group name is the Handler Name
     * (HystrixTaskHandler#getName)
     *
     * @param taskHandler The taskHandler to be wrapped
     * @param taskContext The context (Unique context required by Handlers to communicate with the container.)
     * @param commandName name of the command
     * @param taskRequestWrapper requestWrapper containing the data and the parameters
     * @param concurrentRequestSize no of Max Concurrent requests which can be served
     */
    protected TaskHandlerExecutor(TaskHandler taskHandler, TaskContext taskContext, String commandName,
                                  TaskRequestWrapper taskRequestWrapper , int concurrentRequestSize ) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(taskHandler.getName()))
                .andCommandKey(HystrixCommandKey.Factory.asKey(commandName))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withExecutionIsolationStrategy(ExecutionIsolationStrategy.SEMAPHORE).
                                withExecutionIsolationSemaphoreMaxConcurrentRequests(concurrentRequestSize)))
        ;
        this.taskHandler = taskHandler;
        this.taskContext = taskContext;
        this.command = commandName;
        this.data = taskRequestWrapper.getData();
        this.params = taskRequestWrapper.getParams();
        this.taskRequestWrapper = taskRequestWrapper;
        this.eventBuilder = new ServiceProxyEvent.Builder(commandName, COMMAND_HANDLER);
    }

    /**
     * Constructor for {@link TaskHandler} using Semaphore isolation. The Hystrix command name is commandName. The group name is the Handler Name
     * (HystrixTaskHandler#getName)
     *
     * @param taskHandler The taskHandler to be wrapped
     * @param taskContext The context (Unique context required by Handlers to communicate with the container.)
     * @param commandName name of the command
     * @param taskRequestWrapper requestWrapper containing the data and the parameters
     * @param decoder Decoder sent by the Client
     * @param concurrentRequestSize no of Max Concurrent requests which can be served
     */
    protected TaskHandlerExecutor(TaskHandler taskHandler, TaskContext taskContext, String commandName,
                                  TaskRequestWrapper taskRequestWrapper , int concurrentRequestSize, Decoder decoder ) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(taskHandler.getName()))
                .andCommandKey(HystrixCommandKey.Factory.asKey(commandName))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withExecutionIsolationStrategy(ExecutionIsolationStrategy.SEMAPHORE).
                                withExecutionIsolationSemaphoreMaxConcurrentRequests(concurrentRequestSize)))
        ;
        this.taskHandler = taskHandler;
        this.taskContext = taskContext;
        this.command = commandName;
        this.data = taskRequestWrapper.getData();
        this.params = taskRequestWrapper.getParams();
        this.taskRequestWrapper = taskRequestWrapper;
        this.decoder = decoder;
        this.eventBuilder = new ServiceProxyEvent.Builder(commandName, COMMAND_HANDLER);
    }

    /**
     * Constructor for TaskHandlerExecutor run through Default Hystrix Thread Pool ({@link TaskHandlerExecutor#DEFAULT_HYSTRIX_THREAD_POOL})
     * @param taskHandler The taskHandler to be wrapped
     * @param taskContext The context (Unique context required by Handlers to communicate with the container.)
     * @param commandName name of the command
     * @param executorTimeout the timeout for the Hystrix thread
     * @param taskRequestWrapper requestWrapper containing the data and the parameters
     */
    public TaskHandlerExecutor(TaskHandler taskHandler, TaskContext taskContext, String commandName, int executorTimeout,
                               TaskRequestWrapper taskRequestWrapper) {
        this(taskHandler,taskContext,commandName,executorTimeout,DEFAULT_HYSTRIX_THREAD_POOL,DEFAULT_HYSTRIX_THREAD_POOL_SIZE,taskRequestWrapper);
    }

    /**
     * Constructor for TaskHandlerExecutor run through Default Hystrix Thread Pool ({@link TaskHandlerExecutor#DEFAULT_HYSTRIX_THREAD_POOL})
     * @param taskHandler The taskHandler to be wrapped
     * @param taskContext The context (Unique context required by Handlers to communicate with the container.)
     * @param commandName name of the command
     * @param executorTimeout the timeout for the Hystrix thread
     * @param decoder Decoder sent by the Client
     * @param taskRequestWrapper requestWrapper containing the data and the parameters
     */
    public TaskHandlerExecutor(TaskHandler taskHandler, TaskContext taskContext, String commandName, int executorTimeout,
                               TaskRequestWrapper taskRequestWrapper, Decoder decoder) {
        this(taskHandler,taskContext,commandName,executorTimeout,DEFAULT_HYSTRIX_THREAD_POOL,
                DEFAULT_HYSTRIX_THREAD_POOL_SIZE,taskRequestWrapper, decoder);
    }

    /**
     * Interface method implementation. @see HystrixCommand#run()
     * If Decoder has not been set by the Client, it goes into the default Implementation, otherwise
     * It calls the execute Method which handles decoder.
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
	@Override
    protected  TaskResult run() throws Exception {
        this.eventBuilder.withRequestExecutionStartTime(System.currentTimeMillis());
        if (this.taskRequestWrapper.getRequestContext().isPresent() && this.taskRequestWrapper.getRequestContext().get().getCurrentServerSpan() != null) {
        	Brave.getServerSpanThreadBinder().setCurrentSpan(this.taskRequestWrapper.getRequestContext().get().getCurrentServerSpan());
        }
        for (RequestInterceptor<TaskRequestWrapper> requestInterceptor : this.requestInterceptors) {
        	requestInterceptor.process(this.taskRequestWrapper);
        }        
        Optional<RuntimeException> transportException = Optional.absent();
        TaskResult result = null;
        try {
	        if(decoder == null) {
	            result = this.taskHandler.execute(taskContext, command, params, data);
	        } else {
	            result = this.taskHandler.execute(taskContext, command, taskRequestWrapper,decoder);
	        }
	        if (result == null) {
	        	result = new TaskResult<byte[]>(true, TaskHandlerExecutor.NO_RESULT);
	        }
        } catch (RuntimeException e) {
        	transportException = Optional.of(e);
        	throw e; // rethrow this for it to handled by other layers in the call stack
        } finally {
	        for (ResponseInterceptor<TaskResult> responseInterceptor : this.responseInterceptors) {
	        	responseInterceptor.process(result, transportException);
	        }
        }
        if (!result.isSuccess()) {
            throw new RuntimeException("Command returned FALSE: " + result.getMessage());
        }
        return result;
    }

    /**
     * Interface method implementation. @see HystrixCommand#getFallback()
     */
    @SuppressWarnings("unchecked")
	@Override
    protected TaskResult getFallback() {
        if(this.taskHandler instanceof HystrixTaskHandler) {
            HystrixTaskHandler hystrixTaskHandler = (HystrixTaskHandler) this.taskHandler;
            if(decoder == null) {
                return hystrixTaskHandler.getFallBack(taskContext, command, params, data);
            }
            else {
                return hystrixTaskHandler.getFallBack(taskContext, command, taskRequestWrapper, decoder);
            }
        }
        return null;
    }

    /**
     * Interface method implementation. Adds the RequestInterceptor to the list of request interceptors that will be invoked
     * @see com.flipkart.phantom.task.spi.Executor#addRequestInterceptor(com.flipkart.phantom.task.spi.interceptor.RequestInterceptor)
     */
    public void addRequestInterceptor(RequestInterceptor<TaskRequestWrapper> requestInterceptor) {    	
    	this.requestInterceptors.add(requestInterceptor);
    }

    /**
     * Interface method implementation. Adds the ResponseInterceptor to the list of response interceptors that will be invoked
     * @see com.flipkart.phantom.task.spi.Executor#addResponseInterceptor(com.flipkart.phantom.task.spi.interceptor.ResponseInterceptor)
     */
    public void addResponseInterceptor(ResponseInterceptor<TaskResult> responseInterceptor){
    	this.responseInterceptors.add(responseInterceptor);
    }
    
    /**
     * Interface method implementation. Returns the name of the TaskHandler used by this Executor
     * @see com.flipkart.phantom.task.spi.Executor#getServiceName()
     */
    public Optional<String> getServiceName() {
    	return Optional.of(this.taskHandler.getName());
    }

    /**
     * Interface method implementation. Returns the TaskRequestWrapper instance that this Executor was created with
     * @see com.flipkart.phantom.task.spi.Executor#getRequestWrapper()
     */
    public TaskRequestWrapper getRequestWrapper() {
    	return this.taskRequestWrapper;
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
    
    /**
     * Getter method for the event builder
     */
    public ServiceProxyEvent.Builder getEventBuilder() {
        return eventBuilder;
    }

}
