/*
 * Copyright 2012-2015, Flipkart Internet Pvt Ltd. All rights reserved.
 * 
 * This software is the confidential and proprietary information of Flipkart Internet Pvt Ltd. ("Confidential Information").  
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of the license 
 * agreement you entered into with Flipkart.    
 * 
 */
package com.flipkart.sp.task.impl.task;

import java.util.Map;

import org.trpr.platform.core.impl.logging.LogFactory;
import org.trpr.platform.core.spi.logging.Logger;

import com.flipkart.sp.task.spi.task.HystrixTaskHandler;
import com.flipkart.sp.task.spi.task.TaskContext;
import com.flipkart.sp.task.spi.task.TaskHandler;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.HystrixThreadPoolProperties;

/**
 * <code>TaskHandlerExecutor</code> is an extension of {@link HystrixCommand}. It is essentially a 
 * wrapper around {@link TaskHandler}, providing a means for the TaskHandler to to be called using 
 * a Hystrix Command.
 * 
 * @author devashishshankar
 * @version 1.0, 19th March, 2013
 */
public class TaskHandlerExecutor extends HystrixCommand<TaskResult> {

	/** Logger for this class*/
	private static final Logger LOGGER = LogFactory.getLogger(TaskHandlerExecutor.class);

	/** The default Hystrix group to which the command belongs, unless otherwise mentioned*/
	public static final String DEFAULT_HYSTRIX_GROUP = "DEFAULT_GROUP";

	/** The default Hystrix Thread pool to which this command belongs, unless otherwise mentioned */
	public static final String DEFAULT_HYSTRIX_THREAD_POOL = "DEFAULT_THREAD_POOL";

	/** The default Hystrix Thread pool to which this command belongs, unless otherwise mentioned */
	public static final int DEFAULT_HYSTRIX_THREAD_POOL_SIZE = 10;

	/** The {@link TaskHandler} or {@link HystrixTaskHandler} instance which this Command wraps around */
	protected TaskHandler taskHandler;

	/** The params required to execute a TaskHandler */
	protected TaskContext taskContext;
	protected String command;
	protected Map<String,String> params;
	protected byte[] data;

	/**
	 * Basic constructor for {@link TaskHandler}. The Hystrix command name is commandName. The group name is the Handler Name
	 * (HystrixTaskHandler#getName)
	 * 
	 * @param taskHandler The taskHandler to be wrapped
	 * @param taskContext The context (Unique context required by Handlers to communicate with the container.)
	 * @param commandName name of the command
	 * @param timeout the timeout for the Hystrix thread
	 * @param threadPoolName Name of the thread pool
	 * @param threadPoolSize core size of the thread pool
	 */
	protected TaskHandlerExecutor(TaskHandler taskHandler, TaskContext taskContext, String commandName, int timeout, String threadPoolName, int threadPoolSize) {
		super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(taskHandler.getName()))
				.andCommandKey(HystrixCommandKey.Factory.asKey(commandName))
				.andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(threadPoolName))
				.andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter().withCoreSize(threadPoolSize))
				.andCommandPropertiesDefaults(HystrixCommandProperties.Setter().withExecutionIsolationThreadTimeoutInMilliseconds(timeout)));
		this.taskHandler = taskHandler;
		this.taskContext = taskContext;
		this.command = commandName;
	}

	/**
	 * Constructor for {@link TaskHandler} using Semaphore isolation. The Hystrix command name is commandName. The group name is the Handler Name
	 * (HystrixTaskHandler#getName)
	 * 
	 * @param taskHandler The taskHandler to be wrapped
	 * @param taskContext The context (Unique context required by Handlers to communicate with the container.)
	 * @param commandName name of the command
	 */
	protected TaskHandlerExecutor(TaskHandler taskHandler, TaskContext taskContext, String commandName) {
		super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(taskHandler.getName()))
				.andCommandKey(HystrixCommandKey.Factory.asKey(commandName))
				.andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
						.withExecutionIsolationStrategy(ExecutionIsolationStrategy.SEMAPHORE)))
						;
		this.taskHandler = taskHandler;
		this.taskContext = taskContext;
		this.command = commandName;
	}


	/**
	 * Constructor for TaskHandlerExecutor run through Default Hystrix Thread Pool ({@link TaskHandlerExecutor#DEFAULT_HYSTRIX_THREAD_POOL})
	 * @param taskHandler The taskHandler to be wrapped
	 * @param taskContext The context (Unique context required by Handlers to communicate with the container.)
	 * @param commandName name of the command
	 * @param timeout the timeout for the Hystrix thread
	 */
	public TaskHandlerExecutor(TaskHandler taskHandler, TaskContext taskContext, String commandName, int executorTimeout) {
		this(taskHandler,taskContext,commandName,executorTimeout,DEFAULT_HYSTRIX_THREAD_POOL,DEFAULT_HYSTRIX_THREAD_POOL_SIZE);
	}


	/**
	 * Interface method implementation. @see HystrixCommand#run()
	 * @throws Exception 
	 */
	@Override
	protected TaskResult run() throws Exception {
		try {
			TaskResult result = this.taskHandler.execute(taskContext, command, params, data);
			if(result==null) {
				return new TaskResult(true,"The command returned no result");
			}
			if(result.isSuccess()==false) {
				throw new RuntimeException("Command: "+this.command+" failed: "+(result==null?"":result.getMessage()));
			}
			return result;
		} catch(Exception e) {
			LOGGER.error("Command: "+this.command+" failed. Params: "+this.params+". Data:"+this.data,e);
			throw e;
		}
	}

	/**
	 * Interface method implementation. @see HystrixCommand#getFallback()
	 */
	@Override
	protected TaskResult getFallback() {
		if(this.taskHandler instanceof HystrixTaskHandler) {
			HystrixTaskHandler hystrixTaskHandler = (HystrixTaskHandler) this.taskHandler;
			return hystrixTaskHandler.getFallBack(taskContext, command, params, data);
		}
		return null;
	}

	/**Getter/Setter methods */	
	public Map<String, String> getParams() {
		return params;
	}
	public void setParams(Map<String, String> params) {
		this.params = params;
	}
	public byte[] getData() {
		return data;
	}
	public void setData(byte[] data) {
		this.data = data;
	}
	/** End Getter/Setter methods */	
}
