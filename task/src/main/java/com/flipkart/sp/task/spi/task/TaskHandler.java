/*
 * Copyright 2012-2015, Flipkart Internet Pvt Ltd. All rights reserved.
 * 
 * This software is the confidential and proprietary information of Flipkart Internet Pvt Ltd. ("Confidential Information").  
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of the license 
 * agreement you entered into with Flipkart.    
 * 
 */
package com.flipkart.sp.task.spi.task;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.DisposableBean;
import org.trpr.platform.core.PlatformException;
import org.trpr.platform.core.impl.logging.LogFactory;
import org.trpr.platform.core.spi.logging.Logger;

import com.flipkart.sp.task.impl.task.TaskResult;

/**
 * An individual Task Handler. Based on com.flipkart.w3.agent.TaskHandler
 * Provides methods to additionally set/customize Hytrix Command properties {https://github.com/Netflix/Hystrix}
 *
 * @author devashishshankar
 * @version 1.0, 19 March, 2013
 */
public abstract class TaskHandler implements DisposableBean {

    /** Log instance for this class */
    private static final Logger LOGGER = LogFactory.getLogger(TaskHandler.class);
	
	/** Identifier for threadPool size in initPoolParams */
	public static String PARAM_COMMAND_NAME = "commandName";
	
    /** 
     * The initialization commands can be passed as a list of map.
     * Each map should have a key "commandName" (The name of the command to be executed)
     * The rest of the map should contain command parameters.
     * These commands will be executed as the part of init()
     */
    protected List<Map<String,String>> initializationCommands = new LinkedList<Map<String,String>>();
 
	/** The status showing the TaskHandler is inited and ready to use */
	public static int ACTIVE = 1;

	/** The status showing the TaskHandler is not inted/has been shutdown and should not be used */
	public static int INACTIVE = 0;

	/** The status of this ThriftProxy (active/inactive) */
	private AtomicInteger status = new AtomicInteger(INACTIVE);
	
	/**
	 * Initialize this handler. The default implementation executes "initializationCommands" to initialize the TaskHandler (if found)
	 * @param taskContext the TaskContext that manages this TaskHandler
	 */
	public void init(TaskContext taskContext) throws Exception {
        if(this.initializationCommands==null || this.initializationCommands.size()==0) {
			LOGGER.warn("No initialization commands specified for the TaskHandler: "+this.getName());
			return;
		}
		for(Map<String,String> initParam : this.initializationCommands) {
			String commandName = initParam.get("commandName");
			if(commandName==null) {
				LOGGER.error("Fatal error. commandName not specified in initializationCommands for TaskHandler: "+this.getName());
				throw new UnsupportedOperationException("Fatal error. commandName not specified in initializationCommands of TaskHandler: "+this.getName());
			}
            //Pass a copy of initParam
			TaskResult result = this.execute(taskContext,commandName, new HashMap<String, String>(initParam), null);
			if(result!=null && result.isSuccess()==false) {
				throw new PlatformException("Initialization command: "+commandName+" failed for TaskHandler: "+this.getName()+" The params were: "+initParam);
			}
		}
	}

	/**
	 * Execute this task, using the specified parameters
	 * @param command the command used
	 * @param params task parameters
	 * @param data extra data if any
	 * @return response the TaskResult from task execution
	 */
	public abstract TaskResult execute(TaskContext taskContext, String command, Map<String,String> params, byte[] data) throws RuntimeException;

	/**
	 * Get the name of this handler.
	 * @return the name of this handler
	 */
	public abstract String getName(); 

	/**
	 * Shutdown hooks provided by the TaskContext
	 * @param taskContext the TaskContext that manages this TaskHandler
	 */
	public abstract void shutdown(TaskContext taskContext) throws Exception;
	
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
	/** Get the status of this Task Handler */
	public int getStatus() {
		return status.get();
	}
	/** Set the status of this Task Handler */
	public void setStatus(int status) {
		this.status.set(status);
	}
	public List<Map<String, String>> getInitializationCommands() {
		return initializationCommands;
	}
	public void setInitializationCommands(List<Map<String, String>> initializationCommands) {
		this.initializationCommands = initializationCommands;
	}
	/** End Getter/Setter methods */
}