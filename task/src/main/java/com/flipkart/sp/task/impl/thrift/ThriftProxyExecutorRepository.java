/*
 * Copyright 2012-2015, Flipkart Internet Pvt Ltd. All rights reserved.
 * 
 * This software is the confidential and proprietary information of Flipkart Internet Pvt Ltd. ("Confidential Information").  
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of the license 
 * agreement you entered into with Flipkart.    
 * 
 */
package com.flipkart.sp.task.impl.thrift;

import com.flipkart.sp.task.impl.thrift.registry.ThriftProxyRegistry;
import com.flipkart.sp.task.spi.task.TaskContext;
import com.flipkart.sp.task.spi.thrift.ThriftProxy;


/**
 * <code>ThriftProxyExecutorRepository</code> is a repository of {@link ThriftProxyExecutor} instances. Provides methods to control instantiation of
 * ThriftProxyExecutor instances.
 * 
 * @author Regunath B
 * @version 1.0, 28 March, 2013
 */
public class ThriftProxyExecutorRepository {

	/** The TaskContext instance */
	private TaskContext taskContext;

	/** Thrift Proxy Registry containing the list of Thrift Proxies */
	private ThriftProxyRegistry thriftProxyRegistry;

	/**
	 * Returns a {@link ThriftProxyExecutor} for the specified ThriftProxy and command name
	 * @param commandName the name of the HystrixCommand
	 * @return a ThriftProxyExecutor instance
	 */
	public ThriftProxyExecutor getThriftProxyExecutor (ThriftProxy thriftProxy, String commandName) {
		if(thriftProxy.isActive()) { // check if the ThriftProxy is indeed active
			return new ThriftProxyExecutor(thriftProxy, this.taskContext, commandName, thriftProxy.getExecutorTimeout());
		}
		throw new RuntimeException("The Thrift Proxy is not active. It cannot be executed");
	}

	/** Getter/Setter methods */
	public TaskContext getTaskContext() {
		return this.taskContext;
	}
	public void setTaskContext(TaskContext taskContext) {
		this.taskContext = taskContext;
	}
	public ThriftProxyRegistry getThriftProxyRegistry() {
		return this.thriftProxyRegistry;
	}
	public void setThriftProxyRegistry(ThriftProxyRegistry thriftProxyRegistry) {
		this.thriftProxyRegistry = thriftProxyRegistry;
	}
	/** End Getter/Setter methods */
}
