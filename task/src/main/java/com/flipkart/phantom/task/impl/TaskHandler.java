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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.flipkart.phantom.task.spi.AbstractHandler;
import com.flipkart.phantom.task.spi.TaskContext;
import com.flipkart.phantom.task.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.trpr.platform.core.PlatformException;

/**
 * <code>TaskHandler</code> executes a unit of work i.e thrift. Provides lifecycle methods to initialize the thrift processing infrastructure. Life cycle methods
 * are invoked by the container in a thread-safe manner before this TaskHandler actually starts processing requests.
 * This implementation works on the Command pattern where-in all data required to execute the thrift is provided in the method call to execute the thrift.
 * Task execution is stateless i.e. there is no shared state between any number of consecutive thrift executions. The thrift execution pattern is request-response
 * driven. 
 *
 * @author devashishshankar
 * @author regunath.balasubramanian
 * 
 * @version 1.0, 19 March, 2013
 * @version 2.0, 11 July, 2013
 */
public abstract class TaskHandler extends AbstractHandler implements DisposableBean {

    /** Log instance for this class */
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskHandler.class);
	
	/** Identifier for threadPool size in initPoolParams */
	public static String PARAM_COMMAND_NAME = "commandName";
	
    /** 
     * The initialization commands can be passed as a list of map.
     * Each map should have a key "commandName" (The name of the command to be executed)
     * The rest of the map should contain command parameters.
     * These commands will be executed as the part of init()
     */
    protected List<Map<String,String>> initializationCommands = new LinkedList<Map<String,String>>();

    /**
     * Abstract method implementation
     * @see AbstractHandler#getType()
     */
    @Override
    public String getType() {
        return "TaskHandler";
    }

    /**
     * Abstract method implementation
     * @see AbstractHandler#getDetails()
     */
    @Override
    public String getDetails() {
        String[] commands = getCommands();
        if (commands != null && commands.length > 0) {
            return "Commands: " + StringUtils.join(commands,", ");
        }
        return "No commands registered";
    }

	/**
	 * Initialize this handler. The default implementation executes "initializationCommands" to initialize the TaskHandler (if found)
	 * @param taskContext the TaskContext that manages this TaskHandler
	 */
	public void init(TaskContext taskContext) throws Exception {
        if (this.initializationCommands == null || this.initializationCommands.size() == 0) {
			LOGGER.warn("No initialization commands specified for the TaskHandler: " + this.getName());
		} else {
            for (Map<String,String> initParam : this.initializationCommands) {
                String commandName = initParam.get(PARAM_COMMAND_NAME);
                if (commandName == null) {
                    LOGGER.error("Fatal error. commandName not specified in initializationCommands for TaskHandler: " + this.getName());
                    throw new UnsupportedOperationException("Fatal error. commandName not specified in initializationCommands of TaskHandler: "+this.getName());
                }
                TaskResult result = this.execute(taskContext, commandName, new HashMap<String, String>(initParam), null);
                if (result != null && result.isSuccess() == false) {
                    throw new PlatformException("Initialization command: "+commandName+" failed for TaskHandler: "+this.getName()+" The params were: "+initParam);
                }
            }
        }
	}

	/**
	 * Execute this thrift, using the specified parameters
	 * @param command the command used
	 * @param params thrift parameters
	 * @param data extra data if any
	 * @return response the TaskResult from thrift execution
	 */
	public abstract TaskResult execute(TaskContext taskContext, String command, Map<String,String> params, byte[] data) throws RuntimeException;

	/**
	 * Returns the command names which this handler will handle.
	 * @return commands
	 */
	public abstract String[] getCommands();
	
	/**
	 * Shuts Down TaskHandler when bean is destroyed
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	public void destroy() throws Exception {
		this.shutdown(null);
	}
	
	/** Getter/Setter methods */
	public List<Map<String, String>> getInitializationCommands() {
		return initializationCommands;
	}
	public void setInitializationCommands(List<Map<String, String>> initializationCommands) {
		this.initializationCommands = initializationCommands;
	}
	/** End Getter/Setter methods */
}