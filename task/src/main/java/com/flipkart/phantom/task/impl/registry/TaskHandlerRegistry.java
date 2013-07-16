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

package com.flipkart.phantom.task.impl.registry;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.trpr.platform.core.impl.logging.LogFactory;
import org.trpr.platform.core.spi.logging.Logger;

import com.flipkart.phantom.task.spi.HystrixTaskHandler;
import com.flipkart.phantom.task.spi.TaskHandler;

/**
 * <code>TaskHandlerRegistry</code>  maintains a registry of TaskHandlers. Provides lookup 
 * methods to get a TaskHandler using a command string.
 * 
 * @author devashishshankar
 * @version 1.0, 19th March, 2013
 */
public class TaskHandlerRegistry {

	/** Logger for this class*/
	private static final Logger LOGGER = LogFactory.getLogger(TaskHandlerRegistry.class);

	/** Map storing the mapping of a commandString to TaskHandler */
	private Map<String,TaskHandler> stringToTaskHandler = new ConcurrentHashMap<String, TaskHandler>();

	/** Map storing the mapping of pool Name to its core threadpool size */
	private Map<String,Integer> poolToThreadPoolSize = new ConcurrentHashMap<String, Integer>();

	/**
	 * Register a new TaskHandler. The commandString is defaulted to the name defined
	 * in {@link TaskHandler}
	 * @param taskHandler The {@link TaskHandler} instance to be added
	 */
	public void registerTaskHandler(TaskHandler taskHandler) {
		this.initializeThreadPoolMap(taskHandler);
		for(String commandName: taskHandler.getCommands()) {
			this.registerTaskHandler(commandName,taskHandler);
		}
	}

	/**
	 * Unregisters (removes) a TaskHandler from registry.
	 * @param taskHandler
	 */
	public void unregisterTaskHandler(TaskHandler taskHandler) {
		for(String commandName: taskHandler.getCommands()) {
			this.stringToTaskHandler.remove(commandName);
		}
	}
	/**
	 * Register a new {@link TaskHandler} to the registry. Note: If the CommandString
	 * exists, it overwrites
	 * @param commandString The command String identifying the command
	 * @param taskHandler {@link TaskHandler} instance
	 */
	private void registerTaskHandler(String commandString, TaskHandler taskHandler) {
		if(!this.stringToTaskHandler.containsValue(taskHandler)) { //Check to make sure initialize threadpool map is only called once
			this.initializeThreadPoolMap(taskHandler);
		}
		if(this.stringToTaskHandler.get(commandString)!=null) {
			LOGGER.warn("Overriding TaskHandler for command: "+commandString);
		}
		this.stringToTaskHandler.put(commandString, taskHandler);
	}

    /**
     * Returns the {@link TaskHandler} instance for the given Command String
     * @param commandString The command string
     * @return TaskHandler, if found, null otherwise
     */
    public TaskHandler getTaskHandler(String commandString) {
        return this.stringToTaskHandler.get(commandString);
    }

    /**
     * Returns the {@link TaskHandler} instance for the given handler name
     * @param handlerName The name of the Handler
     * @return TaskHandler, if found, null otherwise
     */
    public TaskHandler getTaskHandlerByName(String handlerName) {
        for(TaskHandler taskHandler: this.getAllTaskHandlers()) {
            if(taskHandler.getName().equals(handlerName))  {
                return taskHandler;
            }
        }
        return null;
    }

	/**
	 * Returns all the TaskHandlers present in the registry
	 * @return Array of TaskHandler
	 */
	public TaskHandler[] getAllTaskHandlers() {
		return (new HashSet<TaskHandler>(this.stringToTaskHandler.values())).toArray(new TaskHandler[0]);
	}

	/**
	 * Get the Thread pool size for a pool/command name.
	 * @param poolOrCommandName the pool or command name for which thread pool size is required
	 * @return thread pool size, null is pool/command not found
	 */
	public Integer getPoolSize(String poolOrCommandName) {
		return this.poolToThreadPoolSize.get(poolOrCommandName);
	}

	/**
	 * Helper method to initialize poolToThreadPoolSize from {@link com.flipkart.phantom.task.spi.TaskHandler#getInitializationCommands()} ()}
	 * and {@link com.flipkart.phantom.task.spi.HystrixTaskHandler#getThreadPoolSizeParams()}
	 */
	private void initializeThreadPoolMap(TaskHandler taskHandler) {
		if(taskHandler instanceof HystrixTaskHandler) {
			HystrixTaskHandler hystrixTaskHandler = (HystrixTaskHandler) taskHandler;
			//thread pool size
			Map<String, Integer> threadParams = hystrixTaskHandler.getThreadPoolSizeParams();
			for(String threadParam : threadParams.keySet()) {
				this.poolToThreadPoolSize.put(threadParam, threadParams.get(threadParam));
			}
			//Commands thread pool size
			Map<String, Integer> commandParams = hystrixTaskHandler.getCommandPoolSizeParams();
			for(String commandParam : commandParams.keySet()) {
				this.poolToThreadPoolSize.put(commandParam, commandParams.get(commandParam));
			}
		}
	}
}
