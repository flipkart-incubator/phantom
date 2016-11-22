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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.trpr.platform.core.impl.logging.LogFactory;
import org.trpr.platform.core.spi.logging.Logger;

import com.flipkart.phantom.task.impl.HystrixTaskHandler;
import com.flipkart.phantom.task.impl.TaskHandler;
import com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry;

/**
 * <code>TaskHandlerRegistry</code>  maintains a registry of TaskHandlers. Provides lookup 
 * methods to get a TaskHandler using a command string.
 * 
 * @author devashishshankar
 * @version 1.0, 19th March, 2013
 */
public class TaskHandlerRegistry extends AbstractHandlerRegistry<TaskHandler> {

	/** Logger for this class*/
	private static final Logger LOGGER = LogFactory.getLogger(TaskHandlerRegistry.class);

    /** Map storing the mapping of a commandString to TaskHandler */
	private Map<String,TaskHandler> commandToTaskHandler = new ConcurrentHashMap<String, TaskHandler>();

	/** Map storing the mapping of pool Name to its max threadpool size */
	private Map<String,Integer> maxPoolSize = new ConcurrentHashMap<String, Integer>();

    /**
     * Map storing the mapping of pool Name to its core threadpool size
     */
	private Map<String,Integer> corePoolSize = new ConcurrentHashMap<>();

    /**
     * Returns the {@link TaskHandler} instance for the given Command String
     * @param commandString The command string
     * @return TaskHandler, if found, null otherwise
     */
    public TaskHandler getTaskHandlerByCommand(String commandString) {
        return this.commandToTaskHandler.get(commandString);
    }

    /**
     * Get the Thread pool size for a pool/command name.
     * @param poolOrCommandName the pool or command name for which thread pool size is required
     * @return thread pool size, null is pool/command not found
     */
    public Integer getMaxPoolSize(String poolOrCommandName) {
        return this.maxPoolSize.get(poolOrCommandName);
    }

    public Integer getCorePoolSize(String poolOrCommandName) {
        return this.corePoolSize.get(poolOrCommandName);
    }
    
	/**
	 * Abstract method implementation. Returns the type of {@link TaskHandler}
	 * @see com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry#getHandlerType()
	 */
	protected Class<TaskHandler> getHandlerType() {
		return TaskHandler.class;
	}

    /**
     * Overridden super class method. 
     * @see com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry#postInitHandler(com.flipkart.phantom.task.spi.AbstractHandler)
     * Registers mapping for all commands supported by the TaskHandler
	 */
	protected void postInitHandler(TaskHandler taskHandler) {
        // register commands
		for (String commandName:taskHandler.getCommands()) {
            LOGGER.info("Registering task handler " + taskHandler.getName() + " with command " + commandName);
            if (!this.commandToTaskHandler.containsValue(taskHandler)) { //Check to make sure initialize threadpool map is only called once
                this.initializeConcurrencyPoolMap(taskHandler);
            }
            if (this.commandToTaskHandler.get(commandName) != null) {
                throw new IllegalArgumentException("Command " + commandName + " is already registered with handler " + this.commandToTaskHandler.get(commandName).getName());
            }
            this.commandToTaskHandler.put(commandName, taskHandler);
		}
	}
	
	/**
	 * Overridden super class method.
	 * @see com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry#postUnregisterHandler(com.flipkart.phantom.task.spi.AbstractHandler)
	 * Removes the command names supported by the TaskHandler that was unregistered
	 */
    protected void postUnregisterHandler(TaskHandler handler) {
		for (String commandName: handler.getCommands()) {
			this.commandToTaskHandler.remove(commandName);
		}
	}
	
    /**
     * Helper method to initialize maxPoolSize and corePoolSize from {@link com.flipkart.phantom.task.impl.TaskHandler#getInitializationCommands()} ()}
     * and {@link com.flipkart.phantom.task.impl.HystrixTaskHandler#getConcurrentPoolSizeParams()} ()}
     */
    private void initializeConcurrencyPoolMap(TaskHandler taskHandler) {
        if (taskHandler instanceof HystrixTaskHandler) {
            HystrixTaskHandler hystrixTaskHandler = (HystrixTaskHandler) taskHandler;
            // Thread pool size
            Map<String, Integer> threadParams = hystrixTaskHandler.getConcurrentPoolSizeParams();
            for (String threadParam : threadParams.keySet()) {
                this.maxPoolSize.put(threadParam, threadParams.get(threadParam));
            }
            // Commands thread pool size
            Map<String, Integer> commandParams = hystrixTaskHandler.getCommandPoolSizeParams();
            for (String commandParam : commandParams.keySet()) {
                this.maxPoolSize.put(commandParam, commandParams.get(commandParam));
            }

            // Core thread pool size
            Map<String, Integer> corePoolSizeParams = hystrixTaskHandler.getCoreConcurrentPoolSizeParams();
            for (String corePoolSizeParam : corePoolSizeParams.keySet()) {
                this.corePoolSize.put(corePoolSizeParam, corePoolSizeParams.get(corePoolSizeParam));
            }

            // Core commands thread pool size
            Map<String, Integer> coreCommandPoolSizeParams = hystrixTaskHandler.getCoreCommandPoolSizeParams();
            for (String coreCommandPoolSizeParam : coreCommandPoolSizeParams.keySet()) {
                this.corePoolSize.put(coreCommandPoolSizeParam, coreCommandPoolSizeParams.get(coreCommandPoolSizeParam));
            }
        }
    }

}
