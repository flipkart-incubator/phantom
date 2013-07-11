/*
 * Copyright 2012-2015, Flipkart Internet Pvt Ltd. All rights reserved.
 * 
 * This software is the confidential and proprietary information of Flipkart Internet Pvt Ltd. ("Confidential Information").  
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of the license 
 * agreement you entered into with Flipkart.    
 * 
 */
package com.flipkart.sp.task.spi.task;

import java.util.Map;
import java.util.concurrent.Future;

import org.codehaus.jackson.map.ObjectMapper;

import com.flipkart.sp.task.impl.task.TaskResult;

/**
 * <code>TaskContext</code> provides methods for {@link TaskHandler} to communicate with it's 
 * Component Container. For backward compatibility, this class provides utility methods present in com.flipkart.w3.agent.W3Agent
 *  
 * @author devashishshankar
 * @version 1.0, 19th March, 2013
 */
public interface TaskContext {
	
	/**
	 * Gets the config from the ConfigTaskHandler.
	 * @param group group name of the object to be fetched
	 * @param key the primary key
	 * @return the config as string, empty string if not found/error
	 */
	public String getConfig(String group, String key, int count);

	/**
	 * Executes a command
	 */
	public TaskResult executeCommand(String commandName, byte[] data, Map<String,String> params) throws UnsupportedOperationException;

	/**
	 * Executes a command asynchronously
	 */
	public Future<TaskResult> executeAsyncCommand(String commandName, byte[] data, Map<String, String> params) throws UnsupportedOperationException;

	/**
	 * Profiles agent command. (For logging/metrics calculation)
	 * @param handler The task Handler which executed the command
	 * @param command The command name
	 * @param diff Time taken to execute the command
	 * @param tags Optional info
	 */
	void profileAgentCommand(TaskHandler handler, String command, Long diff, String tags);

	/** Gets the ObjectMapper instance */
	ObjectMapper getObjectMapper();
}
