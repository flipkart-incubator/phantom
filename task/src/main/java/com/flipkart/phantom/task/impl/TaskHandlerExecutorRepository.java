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

import com.flipkart.phantom.event.ServiceProxyEvent;
import com.flipkart.phantom.event.ServiceProxyEventProducer;
import com.flipkart.phantom.task.impl.registry.TaskHandlerRegistry;
import com.flipkart.phantom.task.spi.Decoder;
import com.flipkart.phantom.task.spi.Executor;
import com.flipkart.phantom.task.spi.RequestWrapper;
import com.flipkart.phantom.task.spi.TaskContext;
import com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry;
import com.flipkart.phantom.task.spi.repository.ExecutorRepository;
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Future;

/**
 * <code>TaskHandlerExecutorRepository</code> is a repository that searches for a {@link TaskHandler}
 * in the {@link TaskHandlerRegistry} based on a command name and then wraps the {@link TaskHandler} into 
 * a {@link TaskHandlerExecutor} and returns it.
 *
 * @author devashishshankar
 * @version 1.0, 20th March, 2013
 */
@SuppressWarnings("rawtypes")
public class TaskHandlerExecutorRepository implements ExecutorRepository<TaskRequestWrapper,TaskResult, TaskHandler> {

    /** Logger for this class*/
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskHandlerExecutorRepository.class);

    /** Regex for finding all non alphanumeric characters */
    public static final String ONLY_ALPHANUMERIC_REGEX = "[^\\dA-Za-z_]";

    /** Regex for finding all whitespace characters */
    public static final String WHITESPACE_REGEX = "\\s+";

    /** The default thread pool core size*/
    private static final int DEFAULT_THREAD_POOL_SIZE = 10;

    /** The registry holding the names of the TaskHandler */
    private TaskHandlerRegistry registry;

    /** The taskContext being passed to the Handlers, providing a way for communication to the Container */
    private TaskContext taskContext;

    /** The publisher used to broadcast events to Service Proxy Subscribers */
    private ServiceProxyEventProducer eventProducer;

    /**
     * Gets the TaskHandlerExecutor or RequestCacheableTaskHandlerExecutor for a commandName
     * @param commandName the command name/String for which the Executor is needed
     * @param proxyName the threadPool using which command has to be processed
     * @return The executor corresponding to the commandName.
     * @throws UnsupportedOperationException if doesn't find a TaskHandler in the registry corresponding to the command name
     */
    public Executor<TaskRequestWrapper,TaskResult> getExecutor(String commandName,String proxyName, RequestWrapper requestWrapper) {
        //Regex matching of threadPoolName and commandName
        //(Hystrix dashboard requires names to be alphanumeric)
        String refinedCommandName = commandName.replaceAll(ONLY_ALPHANUMERIC_REGEX, "").replaceAll(WHITESPACE_REGEX, "");
        String refinedProxyName = proxyName.replaceAll(ONLY_ALPHANUMERIC_REGEX, "").replaceAll(WHITESPACE_REGEX, "");

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
        TaskHandler taskHandler = getTaskHandlerRegistry().getTaskHandlerByCommand(commandName);
        if(taskHandler!=null) {
            if (!taskHandler.isActive()) {
                LOGGER.error("TaskHandler: "+taskHandler.getName()+" is not yet active. Command: "+commandName+" will not be processed");
                return null;
            }
            if(taskHandler instanceof HystrixTaskHandler) {
                HystrixTaskHandler hystrixTaskHandler = (HystrixTaskHandler) taskHandler;
                LOGGER.debug("Isolation strategy: "+hystrixTaskHandler.getIsolationStrategy()+" for "+hystrixTaskHandler);
                int maxConcurrentSize = DEFAULT_THREAD_POOL_SIZE;
                if(getTaskHandlerRegistry().getPoolSize(proxyName)!=null) {
                    LOGGER.debug("Found a predefined pool size for "+proxyName+". Not using default value of "+DEFAULT_THREAD_POOL_SIZE);
                    maxConcurrentSize = getTaskHandlerRegistry().getPoolSize(proxyName);
                }
                if(hystrixTaskHandler.getIsolationStrategy()==ExecutionIsolationStrategy.SEMAPHORE) {
                    return getTaskHandlerExecutorWithSemaphoreIsolation((TaskRequestWrapper) requestWrapper, refinedCommandName, taskHandler, maxConcurrentSize);
                }
                return getTaskHandlerExecutor((TaskRequestWrapper) requestWrapper, refinedCommandName,
                        refinedProxyName, maxConcurrentSize,hystrixTaskHandler.getExecutorTimeout(commandName), taskHandler);

            } else { //Run everything with defaults
                return getTaskHandlerExecutor((TaskRequestWrapper) requestWrapper, refinedCommandName, refinedProxyName,
                        HystrixTaskHandler.DEFAULT_EXECUTOR_TIMEOUT, DEFAULT_THREAD_POOL_SIZE,taskHandler);
            }
        } else {
            throw new UnsupportedOperationException("Invoked unsupported command : " + commandName);
        }
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
    public Executor<TaskRequestWrapper,TaskResult> getExecutor(String commandName,String proxyName, RequestWrapper requestWrapper, Decoder decoder) {
        //Regex matching of threadPoolName and commandName
        //(Hystrix dashboard requires names to be alphanumeric)
        String refinedCommandName = commandName.replaceAll(ONLY_ALPHANUMERIC_REGEX, "").replaceAll(WHITESPACE_REGEX, "");
        String refinedProxyName = proxyName.replaceAll(ONLY_ALPHANUMERIC_REGEX, "").replaceAll(WHITESPACE_REGEX, "");
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
        TaskHandler taskHandler = getTaskHandlerRegistry().getTaskHandlerByCommand(commandName);
        if(taskHandler!=null) {
            if (!taskHandler.isActive()) {
                LOGGER.error("TaskHandler: "+taskHandler.getName()+" is not yet active. Command: "+commandName+" will not be processed");
                return null;
            }
            if(taskHandler instanceof HystrixTaskHandler) {
                HystrixTaskHandler hystrixTaskHandler = (HystrixTaskHandler) taskHandler;
                LOGGER.debug("Isolation strategy: "+hystrixTaskHandler.getIsolationStrategy()+" for "+hystrixTaskHandler);
                int maxConcurrentSize = DEFAULT_THREAD_POOL_SIZE;
                if(getTaskHandlerRegistry().getPoolSize(proxyName)!=null) {
                    LOGGER.debug("Found a predefined pool size for "+proxyName+". Not using default value of "+DEFAULT_THREAD_POOL_SIZE);
                    maxConcurrentSize = getTaskHandlerRegistry().getPoolSize(proxyName);
                }
                if(hystrixTaskHandler.getIsolationStrategy()==ExecutionIsolationStrategy.SEMAPHORE) {
                    return getTaskHandlerExecutorWithSemaphoreIsolationAndDecoder((TaskRequestWrapper) requestWrapper, refinedCommandName, taskHandler, maxConcurrentSize, decoder);
                }
                return getTaskHandlerExecutorWithDecoder((TaskRequestWrapper) requestWrapper, refinedCommandName,
                        refinedProxyName, maxConcurrentSize, hystrixTaskHandler.getExecutorTimeout(commandName), taskHandler, decoder);
            } else { //Run everything with defaults
                return getTaskHandlerExecutorWithDecoder((TaskRequestWrapper) requestWrapper, refinedCommandName, refinedProxyName,
                        HystrixTaskHandler.DEFAULT_EXECUTOR_TIMEOUT, DEFAULT_THREAD_POOL_SIZE, taskHandler, decoder);
            }
        } else {
            throw new UnsupportedOperationException("Invoked unsupported command : " + commandName);
        }
    }

    /**
     * Gets the TaskHandlerExecutor for a commandName
     * @param commandName the command name/String for which the Executor is needed. Defaults threadPoolName to commandName
     * @param requestWrapper requestWrapper having data,hashmap of parameters
     * @return The executor corresponding to the commandName.
     * @throws UnsupportedOperationException if doesn't find a TaskHandler in the registry corresponding to the command name
     */
    public Executor<TaskRequestWrapper,TaskResult> getExecutor(String commandName, RequestWrapper requestWrapper) {
        return this.getExecutor(commandName, commandName, requestWrapper);
    }

    /**
     * Executes a command asynchronously. (Returns a future promise)
     * @param commandName name of the command
     * @param requestWrapper requestWrapper having data,hashmap of parameters
     * @param proxyName name of the threadpool in which the command has to be executed
     * @return thrift result
     * @throws UnsupportedOperationException if no handler found for command
     */
    public Future<TaskResult> executeAsyncCommand(String commandName, String proxyName, RequestWrapper requestWrapper) throws UnsupportedOperationException {
        TaskHandlerExecutor command = (TaskHandlerExecutor) getExecutor(commandName, proxyName, requestWrapper);
        if(command==null) {
            throw new UnsupportedOperationException("Invoked unsupported command : " + commandName);
        } else {
            return command.queue();
        }
    }

    /**
     * Executes a command
     * @param commandName name of the command
     * @param requestWrapper requestWrapper having data,hashmap of parameters
     * @param proxyName name of the threadpool in which the command has to be executed
     * @return thrift result
     * @throws UnsupportedOperationException if no handler found for command
     */
    public TaskResult executeCommand(String commandName, String proxyName, RequestWrapper requestWrapper) throws UnsupportedOperationException {
        long receiveTime = System.currentTimeMillis();
        TaskHandlerExecutor command = (TaskHandlerExecutor) getExecutor(commandName, proxyName, requestWrapper);
        if(command==null) {
            throw new UnsupportedOperationException("Invoked unsupported command : " + commandName);
        } else {
            try {
                return  command.execute();
            } catch (Exception e) {
                throw new RuntimeException("Error in processing command "+commandName+": " + e.getMessage(), e);
            }
            finally {
                if (eventProducer != null) {
                    // Publishes event both in case of success and failure.
                    final Map<String, String> params = ((TaskRequestWrapper)requestWrapper).getParams();

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
    public TaskResult executeCommand(String commandName, RequestWrapper requestWrapper) throws UnsupportedOperationException {
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
    public <T> TaskResult<T> executeCommand(String commandName, RequestWrapper requestWrapper,Decoder<T> decoder) throws UnsupportedOperationException {
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
    public Future<TaskResult> executeAsyncCommand(String commandName, RequestWrapper requestWrapper, Decoder decoder) throws UnsupportedOperationException {
        TaskHandlerExecutor command = (TaskHandlerExecutor) getExecutor(commandName, commandName, requestWrapper, decoder);
        if(command==null) {
            throw new UnsupportedOperationException("Invoked unsupported command : " + commandName);
        } else {
            return command.queue();
        }
    }

    /**
     * Executes a command asynchronously
     * @param commandName name of the command (also the thread pool name)
     * @param requestWrapper requestWrapper having data,hashmap of parameters
     * @return thrift result
     * @throws UnsupportedOperationException if no handler found for command
     */
    public Future<TaskResult> executeAsyncCommand(String commandName, RequestWrapper requestWrapper) throws UnsupportedOperationException {
        return this.executeAsyncCommand(commandName,commandName, requestWrapper);
    }

    /** Getter/Setter methods */

    @Override
    public AbstractHandlerRegistry<TaskHandler> getRegistry() {
        return this.registry;
    }

    @Override
    public void setRegistry(AbstractHandlerRegistry<TaskHandler> taskHandlerRegistry) {
        this.registry = (TaskHandlerRegistry)taskHandlerRegistry;
    }

    @Override
    public TaskContext getTaskContext() {
        return this.taskContext;
    }

    @Override
    public void setTaskContext(TaskContext taskContext) {
        this.taskContext = taskContext;
    }

    /**
     * Returns the Task Handler Registry instance.
     * @return
     */
    public TaskHandlerRegistry getTaskHandlerRegistry() {
        AbstractHandlerRegistry<TaskHandler> registry = getRegistry();
        if(registry instanceof TaskHandlerRegistry){
            return (TaskHandlerRegistry)registry;
        }
        else {
            return new TaskHandlerRegistry();
        }
    }
    /** End Getter/Setter methods */

    /**
     * Helper methods to create and return the appropriate TaskHandlerExecutor instance
     */
    private Executor<TaskRequestWrapper,TaskResult> getTaskHandlerExecutorWithSemaphoreIsolation(TaskRequestWrapper requestWrapper, String refinedCommandName,
                                                                              TaskHandler taskHandler, int maxConcurrentSize) {
        if (taskHandler instanceof RequestCacheableHystrixTaskHandler){
            return new RequestCacheableTaskHandlerExecutor((RequestCacheableHystrixTaskHandler)taskHandler,this.getTaskContext(),refinedCommandName,
                    requestWrapper , maxConcurrentSize);
        }
        else {
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
        }
        else {
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

    public void setEventProducer(ServiceProxyEventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }
}
