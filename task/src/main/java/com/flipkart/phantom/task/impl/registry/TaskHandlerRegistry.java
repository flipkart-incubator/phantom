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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.flipkart.phantom.task.spi.HystrixTaskHandler;
import com.flipkart.phantom.task.spi.TaskContext;
import com.flipkart.phantom.task.spi.TaskHandler;
import com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry;
import com.flipkart.phantom.task.spi.registry.ProxyHandlerConfigInfo;
import com.flipkart.phantom.task.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trpr.platform.core.PlatformException;

/**
 * <code>TaskHandlerRegistry</code>  maintains a registry of TaskHandlers. Provides lookup 
 * methods to get a TaskHandler using a command string.
 * 
 * @author devashishshankar
 * @version 1.0, 19th March, 2013
 */
public class TaskHandlerRegistry extends AbstractHandlerRegistry {

	/** Logger for this class*/
	private static final Logger LOGGER = LoggerFactory.getLogger(TaskHandlerRegistry.class);

    /** List of TaskHandlers */
    private List<TaskHandler> taskHandlers = new ArrayList<TaskHandler>();

    /** Map storing the mapping of a commandString to TaskHandler */
	private Map<String,TaskHandler> commandToTaskHandler = new ConcurrentHashMap<String, TaskHandler>();

	/** Map storing the mapping of pool Name to its core threadpool size */
	private Map<String,Integer> poolToThreadPoolSize = new ConcurrentHashMap<String, Integer>();

	/**
	 * Register a new TaskHandler. The commandString is defaulted to the name defined
	 * in {@link TaskHandler}
	 * @param taskHandler The {@link TaskHandler} instance to be added
	 */
	public void registerTaskHandler(TaskHandler taskHandler) {
        taskHandlers.add(taskHandler);
		this.initializeThreadPoolMap(taskHandler);
		for (String commandName:taskHandler.getCommands()) {
            LOGGER.info("Registering task handler " + taskHandler.getName() + " with command " + commandName);
            if (!this.commandToTaskHandler.containsValue(taskHandler)) { //Check to make sure initialize threadpool map is only called once
                this.initializeThreadPoolMap(taskHandler);
            }
            if (this.commandToTaskHandler.get(commandName) != null) {
                LOGGER.warn("Overriding TaskHandler for command: " + commandName);
            }
            this.commandToTaskHandler.put(commandName, taskHandler);
		}
	}

	/**
	 * Unregisters (removes) a TaskHandler from registry.
	 * @param taskHandler
	 */
	public void unregisterTaskHandler(TaskHandler taskHandler) {
		for(String commandName: taskHandler.getCommands()) {
			this.commandToTaskHandler.remove(commandName);
		}
	}

    /**
     * Returns the {@link TaskHandler} instance for the given Command String
     * @param commandString The command string
     * @return TaskHandler, if found, null otherwise
     */
    public TaskHandler getTaskHandler(String commandString) {
        return this.commandToTaskHandler.get(commandString);
    }

    /**
     * Returns all the TaskHandlers present in the registry
     * @return Array of TaskHandler
     */
    public TaskHandler[] getAllTaskHandlers() {
        return (TaskHandler[]) taskHandlers.toArray();
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
	 * Helper method to initialize poolToThreadPoolSize from {@link com.flipkart.phantom.task.spi.TaskHandler#getInitializationCommands()} ()}
	 * and {@link com.flipkart.phantom.task.spi.HystrixTaskHandler#getThreadPoolSizeParams()}
	 */
	private void initializeThreadPoolMap(TaskHandler taskHandler) {
		if (taskHandler instanceof HystrixTaskHandler) {
			HystrixTaskHandler hystrixTaskHandler = (HystrixTaskHandler) taskHandler;
			// Thread pool size
			Map<String, Integer> threadParams = hystrixTaskHandler.getThreadPoolSizeParams();
			for (String threadParam : threadParams.keySet()) {
				this.poolToThreadPoolSize.put(threadParam, threadParams.get(threadParam));
			}
			// Commands thread pool size
			Map<String, Integer> commandParams = hystrixTaskHandler.getCommandPoolSizeParams();
			for (String commandParam : commandParams.keySet()) {
				this.poolToThreadPoolSize.put(commandParam, commandParams.get(commandParam));
			}
		}
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
     * Abstract method implementation
     * @see com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry#init(java.util.List, com.flipkart.phantom.task.spi.TaskContext)
     * Should call init lifecycle methods of task handlers
     * @param taskContext the TaskContext object
     * @throws Exception
     */
    @Override
    public void init(List<ProxyHandlerConfigInfo> proxyHandlerConfigInfoList, TaskContext taskContext) throws Exception {
        for (ProxyHandlerConfigInfo proxyHandlerConfigInfo : proxyHandlerConfigInfoList) {
            // Init the TaskHandler
            String[] taskHandlerBeanIds = proxyHandlerConfigInfo.getProxyHandlerContext().getBeanNamesForType(TaskHandler.class);
            for(String taskHandlerBeanId : taskHandlerBeanIds) {
                TaskHandler taskHandler = (TaskHandler) proxyHandlerConfigInfo.getProxyHandlerContext().getBean(taskHandlerBeanId);
                // init the TaskHandler
                try {
                    LOGGER.info("Initing TaskHandler: "+taskHandler.getName());
                    taskHandler.init(taskContext);
                    taskHandler.setStatus(TaskHandler.ACTIVE);
                } catch (Exception e) {
                    LOGGER.error("Error initing TaskHandler {} . Error is : " + e.getMessage(),taskHandler.getName(), e);
                    throw new PlatformException("Error initing TaskHandler : " + taskHandler.getName(), e);
                }
                // Register the taskHandler for all the commands it handles
                this.registerTaskHandler(taskHandler);
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
        for (TaskHandler taskHandler:taskHandlers) {
            LOGGER.info("Shutting down task handler: " + taskHandler.getName());
            try {
                taskHandler.shutdown(taskContext);
            } catch (Exception e) {
                LOGGER.warn("Failed to shutdown task handler: " + taskHandler.getName(), e);
            }
            taskHandler.setStatus(TaskHandler.INACTIVE);
        }
    }

    @Override
    public Map<String,String> getHandlers() {
        Map<String,String> map = new HashMap<String, String>();
        for (TaskHandler handler : taskHandlers) {
            map.put(handler.getName(), StringUtils.join(handler.getCommands(), ","));
        }
        return map;
    }

    @Override
    public void reinitHandler(String handlerName, TaskContext taskContext) throws Exception {
        TaskHandler handler = getTaskHandlerByName(handlerName);
        if (handler != null) {
            handler.setStatus(TaskHandler.INACTIVE);
            handler.shutdown(taskContext);
            handler.init(taskContext);
            handler.setStatus(TaskHandler.ACTIVE);
        }
    }

    /** getters / setters **/
    public List<TaskHandler> getTaskHandlers() {
        return taskHandlers;
    }
    public void setTaskHandlers(List<TaskHandler> taskHandlers) {
        this.taskHandlers = taskHandlers;
    }



}
