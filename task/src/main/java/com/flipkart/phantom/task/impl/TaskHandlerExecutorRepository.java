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

import com.flipkart.phantom.task.impl.registry.TaskHandlerRegistry;
import com.flipkart.phantom.task.spi.TaskContext;
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>TaskHandlerExecutorRepository</code> is a repository that searches for a {@link TaskHandler}
 * in the {@link TaskHandlerRegistry} based on a command name and then wraps the {@link TaskHandler} into 
 * a {@link TaskHandlerExecutor} and returns it.
 *  
 * @author devashishshankar
 * @version 1.0, 20th March, 2013
 */
public class TaskHandlerExecutorRepository {

	/** Logger for this class*/
	private static final Logger LOGGER = LoggerFactory.getLogger(TaskHandlerExecutorRepository.class);

	/** Regex for finding all non alphanumeric characters */
	public static final String ONLY_ALPHANUMERIC_REGEX = "[^\\dA-Za-z ]";

	/** Regex for finding all whitespace characters */
	public static final String WHITESPACE_REGEX = "\\s+";

	/** The default thread pool core size*/
	private static final int DEFAULT_THREAD_POOL_SIZE = 10;

	/** The registry holding the names of the TaskHandler */
	private TaskHandlerRegistry taskHandlerRegistry;

	/** The taskContext being passed to the Handlers, providing a way for communication to the Container */
	private TaskContext taskContext;

	/**
	 * Gets the TaskHandlerExecutor for a commandName
	 * @param commandName the command name/String for which the Executor is needed
	 * @param threadPoolName the threadPool using which command has to be processed
	 * @return The executor corresponding to the commandName.
	 * @throws UnsupportedOperationException if doesn't find a TaskHandler in the registry corresponding to the command name
	 */
	public TaskHandlerExecutor get(String commandName,String threadPoolName) {
		//Regex matching of threadPoolName and commandName
		//(Hystrix dashboard requires names to be alphanumeric)
		String refinedCommandName = commandName.replaceAll(ONLY_ALPHANUMERIC_REGEX, "").replaceAll(WHITESPACE_REGEX, "");
		String refinedThreadPoolName = threadPoolName.replaceAll(ONLY_ALPHANUMERIC_REGEX, "").replaceAll(WHITESPACE_REGEX, "");

		if(!commandName.equals(refinedCommandName)) {
			LOGGER.debug("Command names are not allowed to have Special characters/ whitespaces. Replacing: "+commandName+" with "+refinedCommandName);
		}
		if(!threadPoolName.equals(refinedThreadPoolName)) {
			LOGGER.debug("Thread pool names are not allowed to have Special characters/ whitespaces. Replacing: "+threadPoolName+" with "+refinedThreadPoolName);
		}
		if(threadPoolName==null || threadPoolName == "") {
			threadPoolName=commandName;
			LOGGER.debug("null/empty threadPoolName passed. defaulting to commandName: "+commandName);
		}
		TaskHandler taskHandler = this.taskHandlerRegistry.getTaskHandlerByCommand(commandName);
		if(taskHandler!=null) {
            if (!taskHandler.isActive()) {
				LOGGER.error("TaskHandler: "+taskHandler.getName()+" is not yet active. Command: "+commandName+" will not be processed");
				return null;
			}
			if(taskHandler instanceof HystrixTaskHandler) {
				HystrixTaskHandler hystrixTaskHandler = (HystrixTaskHandler) taskHandler;
				LOGGER.debug("Isolation strategy: "+hystrixTaskHandler.getIsolationStrategy()+" for "+hystrixTaskHandler);
				if(hystrixTaskHandler.getIsolationStrategy()==ExecutionIsolationStrategy.SEMAPHORE) {
					return new TaskHandlerExecutor(taskHandler,this.getTaskContext(),refinedCommandName);
				}
				if(this.taskHandlerRegistry.getPoolSize(threadPoolName)==null) {
					LOGGER.debug("Did not find a predefined pool size for "+threadPoolName+". Falling back to default value of "+DEFAULT_THREAD_POOL_SIZE);
					return new TaskHandlerExecutor(taskHandler,this.getTaskContext(),refinedCommandName, hystrixTaskHandler.getExecutorTimeout(commandName),refinedThreadPoolName,DEFAULT_THREAD_POOL_SIZE);
				}
				return new TaskHandlerExecutor(taskHandler,this.getTaskContext(),refinedCommandName, hystrixTaskHandler.getExecutorTimeout(commandName),refinedThreadPoolName,this.taskHandlerRegistry.getPoolSize(threadPoolName));
			} else { //Run everything with defaults
				return new TaskHandlerExecutor(taskHandler,this.getTaskContext(),refinedCommandName, HystrixTaskHandler.DEFAULT_EXECUTOR_TIMEOUT,refinedThreadPoolName,DEFAULT_THREAD_POOL_SIZE);
			}
		} else {
			LOGGER.error("No TaskHandler found for command: '"+commandName+"'. Disconnecting");
			throw new UnsupportedOperationException("Invoked unsupported command : " + commandName);
		}	
	}

	/**
	 * Gets the TaskHandlerExecutor for a commandName
	 * @param commandName the command name/String for which the Executor is needed. Defaults threadPoolName to commandName
	 * @return The executor corresponding to the commandName.
	 * @throws UnsupportedOperationException if doesn't find a TaskHandler in the registry corresponding to the command name
	 */
	public TaskHandlerExecutor get(String commandName) {
		return this.get(commandName,commandName);
	}

	/**
	 * Executes a command asynchronously. (Returns a future promise)
	 * @param commandName name of the command
	 * @param data data
	 * @param params hash map of parameters
	 * @param threadPoolName name of the threadpool in which the command has to be executed
	 * @return thrift result
	 * @throws UnsupportedOperationException if no handler found for command
	 */
	public Future<TaskResult> executeAsyncCommand(String commandName, String threadPoolName, byte[] data, Map<String,String> params) throws UnsupportedOperationException {
		TaskHandlerExecutor command = get(commandName, threadPoolName);
		if(command==null) {
			throw new UnsupportedOperationException("Invoked unsupported command : " + commandName);
		} else {
			command.setData(data);
			command.setParams(params);
			return command.queue();
		}
	}

	/**
	 * Executes a command
	 * @param commandName name of the command
	 * @param data data
	 * @param params hash map of parameters
	 * @param threadPoolName name of the threadpool in which the command has to be executed
	 * @return thrift result
	 * @throws UnsupportedOperationException if no handler found for command
	 */
	public TaskResult executeCommand(String commandName, String threadPoolName, byte[] data, Map<String,String> params) throws UnsupportedOperationException {
		TaskHandlerExecutor command = get(commandName, threadPoolName);
		if(command==null) {
			throw new UnsupportedOperationException("Invoked unsupported command : " + commandName);
		} else {
			command.setData(data);
			command.setParams(params);
			return command.execute();
		}
	}

	/**
	 * Executes a command
	 * @param commandName name of the command (also the thread pool name)
	 * @param data data
	 * @param params hash map of parameters
	 * @return thrift result
	 * @throws UnsupportedOperationException if no handler found for command
	 */
	public TaskResult executeCommand(String commandName, byte[] data, Map<String,String> params) throws UnsupportedOperationException {
		return this.executeCommand(commandName,commandName, data, params);
	}

	/**
	 * Executes a command asynchronously
	 * @param commandName name of the command (also the thread pool name)
	 * @param data data
	 * @param params hash map of parameters
	 * @return thrift result
	 * @throws UnsupportedOperationException if no handler found for command
	 */
	public Future<TaskResult> executeAsyncCommand(String commandName, byte[] data, Map<String,String> params) throws UnsupportedOperationException {
		return this.executeAsyncCommand(commandName,commandName, data, params);
	}

	/** Getter/Setter methods */
	public TaskHandlerRegistry getTaskHandlerRegistry() {
		return this.taskHandlerRegistry;
	}
	public void setTaskHandlerRegistry(TaskHandlerRegistry taskHandlerRegistry) {
		this.taskHandlerRegistry = taskHandlerRegistry;
	}	
	public TaskContext getTaskContext() {
		return this.taskContext;
	}
	public void setTaskContext(TaskContext taskContext) {
		this.taskContext = taskContext;
	}
	/** End Getter/Setter methods */
}
