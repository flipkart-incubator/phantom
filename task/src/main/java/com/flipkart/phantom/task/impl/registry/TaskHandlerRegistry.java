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

import com.flipkart.phantom.task.impl.HystrixTaskHandler;
import com.flipkart.phantom.task.impl.TaskHandler;
import com.flipkart.phantom.task.spi.AbstractHandler;
import com.flipkart.phantom.task.spi.TaskContext;
import com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry;
import com.flipkart.phantom.task.spi.registry.HandlerConfigInfo;
import org.trpr.platform.core.PlatformException;
import org.trpr.platform.core.impl.logging.LogFactory;
import org.trpr.platform.core.spi.logging.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <code>TaskHandlerRegistry</code>  maintains a registry of TaskHandlers. Provides lookup 
 * methods to get a TaskHandler using a command string.
 * 
 * @author devashishshankar
 * @version 1.0, 19th March, 2013
 */
public class TaskHandlerRegistry implements AbstractHandlerRegistry {

	/** Logger for this class*/
	private static final Logger LOGGER = LogFactory.getLogger(TaskHandlerRegistry.class);

    /** List of TaskHandlers */
    private Map<String,TaskHandler> taskHandlers = new HashMap<String,TaskHandler>();

    /** Map storing the mapping of a commandString to TaskHandler */
	private Map<String,TaskHandler> commandToTaskHandler = new ConcurrentHashMap<String, TaskHandler>();

	/** Map storing the mapping of pool Name to its core threadpool size */
	private Map<String,Integer> concurrencyPoolSize = new ConcurrentHashMap<String, Integer>();

    /**
     * Abstract method implementation
     * @see com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry#init(java.util.List, com.flipkart.phantom.task.spi.TaskContext)
     * Should call init lifecycle methods of task handlers
     * @param taskContext the TaskContext object
     * @throws Exception
     */
    @Override
    public AbstractHandlerRegistry.InitedHandlerInfo[] init(List<HandlerConfigInfo> handlerConfigInfoList, TaskContext taskContext) throws Exception {
    	List<AbstractHandlerRegistry.InitedHandlerInfo> initedHanlderInfos = new LinkedList<AbstractHandlerRegistry.InitedHandlerInfo>();    	
        for (HandlerConfigInfo handlerConfigInfo : handlerConfigInfoList) {
            String[] taskHandlerBeanIds = handlerConfigInfo.getProxyHandlerContext().getBeanNamesForType(TaskHandler.class);
            for (String taskHandlerBeanId : taskHandlerBeanIds) {
                TaskHandler taskHandler = (TaskHandler) handlerConfigInfo.getProxyHandlerContext().getBean(taskHandlerBeanId);
                try {
                    LOGGER.info("Initializing TaskHandler: " + taskHandler.getName());
                    taskHandler.init(taskContext);
                    taskHandler.activate();
                    initedHanlderInfos.add(new AbstractHandlerRegistry.InitedHandlerInfo(taskHandler,handlerConfigInfo));                    
                } catch (Exception e) {
                    LOGGER.error("Error initializing TaskHandler {}. Error is: " + e.getMessage(), taskHandler.getName(), e);
                    throw new PlatformException("Error initializing TaskHandler: " + taskHandler.getName(), e);
                }
                // Register the taskHandler for all the commands it handles
                this.registerTaskHandler(taskHandler);
            }
        }
        return initedHanlderInfos.toArray(new AbstractHandlerRegistry.InitedHandlerInfo[0]);        
    }

    /**
     * Abstract method implementation
     * @see com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry#reinitHandler(String, com.flipkart.phantom.task.spi.TaskContext)
     */
    @Override
    public void reinitHandler(String name, TaskContext taskContext) throws Exception {
        TaskHandler handler = this.taskHandlers.get(name);
        if (handler != null) {
            try {
                handler.deactivate();
                handler.shutdown(taskContext);
                handler.init(taskContext);
                handler.activate();
            } catch (Exception e) {
                LOGGER.error("Error initializing TaskHandler {}. Error is: " + e.getMessage(), name, e);
                throw new PlatformException("Error reinitialising TaskHandler: " + name, e);
            }
        }
    }

    /**
     * Abstract method implementation
     * @see com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry#shutdown(com.flipkart.phantom.task.spi.TaskContext)
     * Should call shutdown lifecycle methods of task handlers
     * @param taskContext the TaskContext object
     * @throws Exception
     */
    @Override
    public void shutdown(TaskContext taskContext) throws Exception {
        for (String name : taskHandlers.keySet()) {
            LOGGER.info("Shutting down task handler: " + name);
            try {
                taskHandlers.get(name).shutdown(taskContext);
                taskHandlers.get(name).deactivate();
            } catch (Exception e) {
                LOGGER.warn("Failed to shutdown task handler: " + name, e);
            }
        }
    }

    /**
     * Abstract method implementation
     * @see com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry#getHandlers()
     * @return
     */
    @Override
    public List<AbstractHandler> getHandlers() {
        return new ArrayList<AbstractHandler>(taskHandlers.values());
    }

    /**
     * Returns the {@link TaskHandler} instance for the given handler name
     * @param name The name of the Handler
     * @return TaskHandler, if found, null otherwise
     */
    public AbstractHandler getHandler(String name) {
        return taskHandlers.get(name);
    }

	/**
	 * Register a new TaskHandler. The commandString is defaulted to the name defined
	 * in {@link TaskHandler}
	 * @param taskHandler The {@link TaskHandler} instance to be added
	 */
	public void registerTaskHandler(TaskHandler taskHandler) {

        // put in all handlers map
        taskHandlers.put(taskHandler.getName(),taskHandler);

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
     * Helper method to initialize concurrencyPoolSize from {@link com.flipkart.phantom.task.impl.TaskHandler#getInitializationCommands()} ()}
     * and {@link com.flipkart.phantom.task.impl.HystrixTaskHandler#getConcurrentPoolSizeParams()} ()}
     */
    private void initializeConcurrencyPoolMap(TaskHandler taskHandler) {
        if (taskHandler instanceof HystrixTaskHandler) {
            HystrixTaskHandler hystrixTaskHandler = (HystrixTaskHandler) taskHandler;
            // Thread pool size
            Map<String, Integer> threadParams = hystrixTaskHandler.getConcurrentPoolSizeParams();
            for (String threadParam : threadParams.keySet()) {
                this.concurrencyPoolSize.put(threadParam, threadParams.get(threadParam));
            }
            // Commands thread pool size
            Map<String, Integer> commandParams = hystrixTaskHandler.getCommandPoolSizeParams();
            for (String commandParam : commandParams.keySet()) {
                this.concurrencyPoolSize.put(commandParam, commandParams.get(commandParam));
            }
        }
    }

    /**
     * Abstract method implementation
     * @see com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry#unregisterTaskHandler(com.flipkart.phantom.task.spi.AbstractHandler)
     */
    @Override
	public void unregisterTaskHandler(AbstractHandler taskHandler) {    	
		for (String commandName: ((TaskHandler)taskHandler).getCommands()) {
			this.commandToTaskHandler.remove(commandName);
		}
	}
	
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
    public Integer getPoolSize(String poolOrCommandName) {
        return this.concurrencyPoolSize.get(poolOrCommandName);
    }

}
