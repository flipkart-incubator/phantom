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

import java.util.Map;
import java.util.concurrent.Future;

import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flipkart.phantom.event.ServiceProxyEvent;
import com.flipkart.phantom.event.ServiceProxyEventProducer;
import com.flipkart.phantom.task.impl.interceptor.ClientRequestInterceptor;
import com.flipkart.phantom.task.impl.interceptor.CommandClientResponseInterceptor;
import com.flipkart.phantom.task.impl.registry.TaskHandlerRegistry;
import com.flipkart.phantom.task.impl.repository.AbstractExecutorRepository;
import com.flipkart.phantom.task.spi.Decoder;
import com.flipkart.phantom.task.spi.Executor;
import com.flipkart.phantom.task.spi.RequestWrapper;
import com.flipkart.phantom.task.spi.TaskRequestWrapper;
import com.flipkart.phantom.task.spi.TaskResult;
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;

/**
 * <code>TaskHandlerExecutorRepository</code> is a repository that searches for a {@link TaskHandler}
 * in the {@link TaskHandlerRegistry} based on a command name and then wraps the {@link TaskHandler} into 
 * a {@link TaskHandlerExecutor} and returns it.
 *
 * @author devashishshankar
 * @version 1.0, 20th March, 2013
 */
@SuppressWarnings("rawtypes")
public class TaskHandlerExecutorRepository extends AbstractExecutorRepository<TaskRequestWrapper,TaskResult, TaskHandler> {

    /** Logger for this class*/
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskHandlerExecutorRepository.class);

    /** Regex for finding all non alphanumeric characters */
    public static final String ONLY_ALPHANUMERIC_REGEX = "[^\\dA-Za-z_]";

    /** Regex for finding all whitespace characters */
    public static final String WHITESPACE_REGEX = "\\s+";

    /** The default thread pool core size*/
    private static final int DEFAULT_THREAD_POOL_SIZE = 10;

    /** The publisher used to broadcast events to Service Proxy Subscribers */
    private ServiceProxyEventProducer eventProducer;
    
    /**
     * Gets the TaskHandlerExecutor or RequestCacheableTaskHandlerExecutor for a commandName
     * @param commandName the command name/String for which the Executor is needed
     * @param proxyName the threadPool using which command has to be processed
     * @return The executor corresponding to the commandName.
     * @throws UnsupportedOperationException if doesn't find a TaskHandler in the registry corresponding to the command name
     */
    public Executor<TaskRequestWrapper,TaskResult> getExecutor(String commandName,String proxyName, TaskRequestWrapper requestWrapper) {
    	Executor<TaskRequestWrapper,TaskResult> executor = null;
        //Regex matching of threadPoolName and commandName
        //(Hystrix dashboard requires names to be alphanumeric)    	
    	String refinedCommandName = this.getRefinedName(commandName);
    	String refinedProxyName = this.getRefinedName(proxyName);
    	TaskHandler taskHandler = this.getTaskHandler(commandName, refinedCommandName, proxyName, refinedProxyName, requestWrapper);
    	int maxConcurrency = this.getMaxConcurrency(taskHandler, proxyName);
    	int executionTimeout = this.getExecutionTimeout(taskHandler, commandName);
        if(taskHandler instanceof HystrixTaskHandler) {
	        if(((HystrixTaskHandler)taskHandler).getIsolationStrategy()==ExecutionIsolationStrategy.SEMAPHORE) {
	        	executor = getTaskHandlerExecutorWithSemaphoreIsolation(requestWrapper, refinedCommandName, 
	        			taskHandler, maxConcurrency);
	        } else {
	        	executor = getTaskHandlerExecutor(requestWrapper, refinedCommandName,
	                refinedProxyName, maxConcurrency,executionTimeout, taskHandler);
	        }
        } else { 
        	executor = getTaskHandlerExecutor(requestWrapper, refinedCommandName, refinedProxyName,
        			maxConcurrency, executionTimeout, taskHandler);
        }
        return this.wrapExecutorWithInterceptors(executor, taskHandler);
    }

    /**
     * Gets the TaskHandlerExecutor or RequestCacheableTaskHandlerExecutor for a commandName
     * @param commandName the command name/String for which the Executor is needed
     * @param proxyName the threadPool using which command has to be processed
     * @param requestWrapper requestWrapper
     * @param decoder decoder passed by the client
     * @return The executor corresponding to the commandName.
     * @throws UnsupportedOperationException if doesn't find a TaskHandler in the registry corresponding to the command name
     */
    public Executor<TaskRequestWrapper,TaskResult> getExecutor(String commandName,String proxyName, TaskRequestWrapper requestWrapper, Decoder decoder) {
    	Executor<TaskRequestWrapper,TaskResult> executor = null;
        //Regex matching of threadPoolName and commandName
        //(Hystrix dashboard requires names to be alphanumeric)    	
    	String refinedCommandName = this.getRefinedName(commandName);
    	String refinedProxyName = this.getRefinedName(proxyName);
    	TaskHandler taskHandler = this.getTaskHandler(commandName, refinedCommandName, proxyName, refinedProxyName, requestWrapper);
    	int maxConcurrency = this.getMaxConcurrency(taskHandler, proxyName);
    	int executionTimeout = this.getExecutionTimeout(taskHandler, commandName);
        if(taskHandler instanceof HystrixTaskHandler) {
	        if(((HystrixTaskHandler)taskHandler).getIsolationStrategy()==ExecutionIsolationStrategy.SEMAPHORE) {
	        	executor = getTaskHandlerExecutorWithSemaphoreIsolationAndDecoder(requestWrapper, refinedCommandName, 
	        			taskHandler, maxConcurrency, decoder);
	        } else {
	        	executor = getTaskHandlerExecutorWithDecoder(requestWrapper, refinedCommandName,
	                refinedProxyName, maxConcurrency,executionTimeout, taskHandler, decoder);
	        }
        } else { 
        	executor = getTaskHandlerExecutorWithDecoder(requestWrapper, refinedCommandName, refinedProxyName,
        			maxConcurrency, executionTimeout, taskHandler, decoder);
        }
        return this.wrapExecutorWithInterceptors(executor, taskHandler);
    }

    /**
     * Gets the TaskHandlerExecutor for a commandName
     * @param commandName the command name/String for which the Executor is needed. Defaults threadPoolName to commandName
     * @param requestWrapper requestWrapper having data,hashmap of parameters
     * @return The executor corresponding to the commandName.
     * @throws UnsupportedOperationException if doesn't find a TaskHandler in the registry corresponding to the command name
     */
    public Executor<TaskRequestWrapper,TaskResult> getExecutor(String commandName, TaskRequestWrapper requestWrapper) {
        return this.getExecutor(commandName, commandName, requestWrapper);
    }

    private Future<TaskResult> executeAsyncCommand(final long receiveTime, final TaskHandlerExecutor command,
                                                   final String commandName, final TaskRequestWrapper requestWrapper) {
        if(command==null) {
            throw new UnsupportedOperationException("Invoked unsupported command : " + commandName);
        } else {
            final Future<TaskResult> future = command.queue();
            MoreExecutors.sameThreadExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        future.get();
                    } catch (Exception e) {
                        throw new RuntimeException("Error in processing command " + command.getServiceName() + ": " + e.getMessage(), e);
                    } finally {
                        if (eventProducer != null) {
                            // Publishes event both in case of success and failure.
                            final Map<String, String> params = requestWrapper.getParams();
                            ServiceProxyEvent.Builder eventBuilder = command.getEventBuilder().withCommandData(command).withEventSource(command.getClass().getName());
                            eventBuilder.withRequestId(params.get("requestID")).withRequestReceiveTime(receiveTime);
                            if (params.containsKey("requestSentTime"))
                                eventBuilder.withRequestSentTime(Long.valueOf(params.get("requestSentTime")));
                            eventProducer.publishEvent(eventBuilder.build());
                        } else
                            LOGGER.debug("eventProducer not set, not publishing event");
                    }
                }
            });
            return future;
        }
    }

    /**
     * Executes a command asynchronously. (Returns a future promise)
     * @param commandName name of the command
     * @param requestWrapper requestWrapper having data,hashmap of parameters
     * @param proxyName name of the threadpool in which the command has to be executed
     * @return thrift result
     * @throws UnsupportedOperationException if no handler found for command
     */
    public Future<TaskResult> executeAsyncCommand(final String commandName, String proxyName, final TaskRequestWrapper requestWrapper) throws UnsupportedOperationException {
        final long receiveTime = System.currentTimeMillis();
        final TaskHandlerExecutor command = (TaskHandlerExecutor) getExecutor(commandName, proxyName, requestWrapper);
        return executeAsyncCommand(receiveTime, command, commandName, requestWrapper);
    }

    /**
     * Executes a command
     * @param commandName name of the command
     * @param requestWrapper requestWrapper having data,hashmap of parameters
     * @param proxyName name of the threadpool in which the command has to be executed
     * @return thrift result
     * @throws UnsupportedOperationException if no handler found for command
     */
    public TaskResult executeCommand(String commandName, String proxyName, TaskRequestWrapper requestWrapper) throws UnsupportedOperationException {
        long receiveTime = System.currentTimeMillis();
        TaskHandlerExecutor command = (TaskHandlerExecutor) getExecutor(commandName, proxyName, requestWrapper);
        if(command==null) {
            throw new UnsupportedOperationException("Invoked unsupported command : " + commandName);
        } else {
            try {
                return command.execute();
            } catch (Exception e) {
                throw new RuntimeException("Error in processing command "+commandName+": " + e.getMessage(), e);
            } finally {
                if (eventProducer != null) {
                    // Publishes event both in case of success and failure.
                    final Map<String, String> params = requestWrapper.getParams();
                    ServiceProxyEvent.Builder eventBuilder = command.getEventBuilder().withCommandData(command).withEventSource(command.getClass().getName());
                    eventBuilder.withRequestId(params.get("requestID")).withRequestReceiveTime(receiveTime);
                    if(params.containsKey("requestSentTime"))
                        eventBuilder.withRequestSentTime(Long.valueOf(params.get("requestSentTime")));
                    eventProducer.publishEvent(eventBuilder.build());
                } else
                    LOGGER.debug("eventProducer not set, not publishing event");
            }
        }
    }

    /**
     * Executes a command
     * @param commandName name of the command (also the thread pool name)
     * @param requestWrapper requestWrapper having data,hashmap of parameters
     * @return thrift result
     * @throws UnsupportedOperationException if no handler found for command
     */
    public TaskResult executeCommand(String commandName, TaskRequestWrapper requestWrapper) throws UnsupportedOperationException {
        return this.executeCommand(commandName,commandName, requestWrapper);
    }

    /**
     * Executes a command
     * @param commandName name of the command (also the thread pool name)
     * @param requestWrapper requestWrapper having data,hashmap of parameters
     * @return thrift result
     * @throws UnsupportedOperationException if no handler found for command
     */
    @SuppressWarnings("unchecked")
    public <T> TaskResult<T> executeCommand(String commandName, TaskRequestWrapper requestWrapper,Decoder<T> decoder) throws UnsupportedOperationException {
        TaskHandlerExecutor command = (TaskHandlerExecutor) getExecutor(commandName, commandName, requestWrapper, decoder);
        if(command==null) {
            throw new UnsupportedOperationException("Invoked unsupported command : " + commandName);
        } else {
            try {
                return command.execute();
            } catch (Exception e) {
                throw new RuntimeException("Error in processing command "+commandName+": " + e.getMessage(), e);
            }
        }
    }

    /**
     * Executes a command asynchronously. (Returns a future promise)
     * @param commandName name of the command
     * @param requestWrapper requestWrapper having data,hashmap of parameters
     * @return thrift result
     * @throws UnsupportedOperationException if no handler found for command
     */
    public Future<TaskResult> executeAsyncCommand(String commandName, TaskRequestWrapper requestWrapper, Decoder decoder) throws UnsupportedOperationException {
        final long receiveTime = System.currentTimeMillis();
        TaskHandlerExecutor command = (TaskHandlerExecutor) getExecutor(commandName, commandName, requestWrapper, decoder);
        return executeAsyncCommand(receiveTime, command, commandName, requestWrapper);
    }

    /**
     * Executes a command asynchronously
     * @param commandName name of the command (also the thread pool name)
     * @param requestWrapper requestWrapper having data,hashmap of parameters
     * @return thrift result
     * @throws UnsupportedOperationException if no handler found for command
     */
    public Future<TaskResult> executeAsyncCommand(String commandName, TaskRequestWrapper requestWrapper) throws UnsupportedOperationException {
        return this.executeAsyncCommand(commandName,commandName, requestWrapper);
    }

    /**
     * Helper methods to create and return the appropriate TaskHandlerExecutor instance
     */
    private Executor<TaskRequestWrapper,TaskResult> getTaskHandlerExecutorWithSemaphoreIsolation(TaskRequestWrapper requestWrapper, String refinedCommandName,
                                                                              TaskHandler taskHandler, int maxConcurrentSize) {
        if (taskHandler instanceof RequestCacheableHystrixTaskHandler){
            return new RequestCacheableTaskHandlerExecutor((RequestCacheableHystrixTaskHandler)taskHandler,this.getTaskContext(),refinedCommandName,
                    requestWrapper , maxConcurrentSize);
        } else {
            return new TaskHandlerExecutor(taskHandler,this.getTaskContext(),refinedCommandName,requestWrapper , maxConcurrentSize);
        }
    }
    private Executor<TaskRequestWrapper,TaskResult> getTaskHandlerExecutor(TaskRequestWrapper requestWrapper, String refinedCommandName,
                                                        String refinedProxyName, int maxConcurrentSize, int executorTimeOut, TaskHandler taskHandler) {
        if (taskHandler instanceof RequestCacheableHystrixTaskHandler) {
            return new RequestCacheableTaskHandlerExecutor((RequestCacheableHystrixTaskHandler)taskHandler,this.getTaskContext(),
                    refinedCommandName, executorTimeOut, refinedProxyName,maxConcurrentSize,requestWrapper);
        } else {
            return new TaskHandlerExecutor(taskHandler,this.getTaskContext(),refinedCommandName, executorTimeOut,
                    refinedProxyName,maxConcurrentSize,requestWrapper);
        }
    }

    /**
     * Helper methods to create and return the appropriate TaskHandlerExecutor instance
     */
    private  Executor<TaskRequestWrapper,TaskResult> getTaskHandlerExecutorWithSemaphoreIsolationAndDecoder(TaskRequestWrapper requestWrapper, String refinedCommandName,
                                                                                         TaskHandler taskHandler, int maxConcurrentSize, Decoder decoder) {
        if (taskHandler instanceof RequestCacheableHystrixTaskHandler){
            return  new RequestCacheableTaskHandlerExecutor((RequestCacheableHystrixTaskHandler)taskHandler,this.getTaskContext(),refinedCommandName,
                    requestWrapper , maxConcurrentSize,decoder);
        } else {
            return new TaskHandlerExecutor(taskHandler,this.getTaskContext(),refinedCommandName,requestWrapper , maxConcurrentSize, decoder);
        }
    }
    private Executor<TaskRequestWrapper,TaskResult> getTaskHandlerExecutorWithDecoder(TaskRequestWrapper requestWrapper, String refinedCommandName,
                                                                   String refinedProxyName, int maxConcurrentSize, int executorTimeOut,
                                                                   TaskHandler taskHandler, Decoder decoder) {
        if (taskHandler instanceof RequestCacheableHystrixTaskHandler) {
            return new RequestCacheableTaskHandlerExecutor((RequestCacheableHystrixTaskHandler)taskHandler,this.getTaskContext(),
                    refinedCommandName, executorTimeOut, refinedProxyName,maxConcurrentSize,requestWrapper, decoder);
        } else {
            return new TaskHandlerExecutor(taskHandler,this.getTaskContext(),refinedCommandName, executorTimeOut,
                    refinedProxyName,maxConcurrentSize,requestWrapper, decoder);
        }
    }
    
    /**
     * Helper method to wrap the Executor with request and response interceptors 
     */
    private Executor<TaskRequestWrapper,TaskResult> wrapExecutorWithInterceptors(Executor<TaskRequestWrapper,TaskResult> executor, TaskHandler taskHandler) {
        ClientRequestInterceptor<TaskRequestWrapper> tracingRequestInterceptor = new ClientRequestInterceptor<TaskRequestWrapper>();
        CommandClientResponseInterceptor<TaskResult> tracingResponseInterceptor = new CommandClientResponseInterceptor<TaskResult>();
        return this.wrapExecutorWithInterceptors(executor, taskHandler, tracingRequestInterceptor, tracingResponseInterceptor);
    }
    
    /**
     * Helper method to get the execution timeout
     */
    private int getExecutionTimeout(TaskHandler taskHandler, String commandName) {
    	int executionTimeout = HystrixTaskHandler.DEFAULT_EXECUTOR_TIMEOUT;
    	if (taskHandler instanceof HystrixTaskHandler) {
    		executionTimeout = ((HystrixTaskHandler)taskHandler).getExecutorTimeout(commandName);
    	}
    	return executionTimeout;
    }
    
    /**
     * Helper method to get the max concurrency size for the specified handler
     */
    private int getMaxConcurrency(TaskHandler taskHandler, String proxyName) {
        int maxConcurrentSize = DEFAULT_THREAD_POOL_SIZE;
        if(taskHandler instanceof HystrixTaskHandler) {
            HystrixTaskHandler hystrixTaskHandler = (HystrixTaskHandler) taskHandler;
            LOGGER.debug("Isolation strategy: "+hystrixTaskHandler.getIsolationStrategy()+" for "+hystrixTaskHandler);
            if(((TaskHandlerRegistry)getRegistry()).getPoolSize(proxyName)!=null) {
                LOGGER.debug("Found a predefined pool size for "+proxyName+". Not using default value of "+DEFAULT_THREAD_POOL_SIZE);
                maxConcurrentSize = ((TaskHandlerRegistry)getRegistry()).getPoolSize(proxyName);
            }
        }
        return maxConcurrentSize;
    }
    
    /**
     * Helper method to refine the specified name
     */
    private String getRefinedName(String name) {
    	return name.replaceAll(ONLY_ALPHANUMERIC_REGEX, "").replaceAll(WHITESPACE_REGEX, "");
    }
    
    /**
     * Helper method to validate the handler identifications parameters and return a suitable active TaskHandler 
     */
    private TaskHandler getTaskHandler(String commandName, String refinedCommandName, String proxyName, String refinedProxyName, RequestWrapper requestWrapper) {
        if(!commandName.equals(refinedCommandName)) {
            LOGGER.debug("Command names are not allowed to have Special characters/ whitespaces. Replacing: "+commandName+" with "+refinedCommandName);
        }
        if(!proxyName.equals(refinedProxyName)) {
            LOGGER.debug("Thread pool names are not allowed to have Special characters/ whitespaces. Replacing: "+proxyName+" with "+refinedProxyName);
        }
        if(proxyName.isEmpty()) {
            proxyName=commandName;
            LOGGER.debug("null/empty threadPoolName passed. defaulting to commandName: "+commandName);
        }
        TaskHandler taskHandler = ((TaskHandlerRegistry)getRegistry()).getTaskHandlerByCommand(commandName);
        if(taskHandler!=null) {
            if (!taskHandler.isActive()) {
                LOGGER.error("TaskHandler: "+taskHandler.getName()+" is not yet active. Command: "+commandName+" will not be processed");
                return null;
            }
        } else {
        	throw new UnsupportedOperationException("Invoked unsupported command : " + commandName);        	
        }
        return taskHandler;
    }

    /** Getter/Setter methods*/
    public void setEventProducer(ServiceProxyEventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }
    
}
