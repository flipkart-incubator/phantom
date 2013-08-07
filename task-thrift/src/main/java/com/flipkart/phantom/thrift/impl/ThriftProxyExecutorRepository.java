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
package com.flipkart.phantom.thrift.impl;

import com.flipkart.phantom.thrift.impl.registry.ThriftProxyRegistry;
import com.flipkart.phantom.task.spi.TaskContext;
import com.flipkart.phantom.thrift.spi.ThriftProxy;


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
	private ThriftProxyRegistry registry;

	/**
	 * Returns a {@link ThriftProxyExecutor} for the specified ThriftProxy and command name
	 * @param commandName the name of the HystrixCommand
	 * @return a ThriftProxyExecutor instance
	 */
	public ThriftProxyExecutor getThriftProxyExecutor (String proxyName, String commandName) {
        ThriftProxy thriftProxy = registry.getProxy(proxyName);
		if (thriftProxy.isActive()) { // check if the ThriftProxy is indeed active
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
	public ThriftProxyRegistry getRegistry() {
		return this.registry;
	}
	public void setRegistry(ThriftProxyRegistry registry) {
		this.registry = registry;
	}
	/** End Getter/Setter methods */
}
