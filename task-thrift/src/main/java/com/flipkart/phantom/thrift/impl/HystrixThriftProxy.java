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

import com.flipkart.phantom.task.spi.TaskContext;
import com.flipkart.phantom.task.utils.StringUtils;
import com.netflix.hystrix.HystrixCommandProperties;
import org.apache.thrift.transport.TTransport;

import java.util.HashMap;
import java.util.Map;

/**
 * An extension of {@link ThriftProxy}. Adds additional methods required by Hystrix. Uses the Thrift call name as the
 * HystrixCommand name for display on the dashboard.
 * 
 * @author Regunath B
 * @version 1.0, 28 March, 2013
 */
public abstract class HystrixThriftProxy extends ThriftProxy {

	/** The default executor timeout in millis*/
	private static final int DEFAULT_EXECUTOR_TIMEOUT = 2000;
	
	/** The overall proxy level execution timeout */
	private int proxyExecutorTimeout = HystrixThriftProxy.DEFAULT_EXECUTOR_TIMEOUT;
	
	/** The proxy level thread pool size*/
	private Integer proxyThreadPoolSize;
				
    /**
     * Map of command names and their respective executor timeouts in milliseconds
     */
    protected Map<String,Integer> executorTimeouts = new HashMap<String, Integer>();
	
	/**
	 * Executes this fallback request when {@link "#proxyThriftRequest(org.apache.thrift.transport.TTransport)"} fails
	 * @param clientTransport the Thrift {@link org.apache.thrift.transport.TTransport} of the invoking client
	 * @throws RuntimeException in case of errors
	 */
	public abstract void fallbackThriftRequest(TTransport clientTransport,TaskContext taskContext);
	
	/**
	 * Optional. Gets the GroupName this ThriftProxy should be assigned to. 
	 * @return the Group name. Returns null for default
	 */
	public String getGroupName() {
		return super.getName();
	}

	/**
	 * Optional. Gets the ThreadPoolName this ThriftProxy should be assigned to. 
	 * @return the Thread pool name. Returns null for default
	 */
	public String getThreadPoolName() {
		return super.getName();
	}
	
	/**
	 * Optional. Gets the Hystrix Properties set by this ThriftProxy
	 * @return HystrixCommandProperties.Setter, null for default
	 */
	public HystrixCommandProperties.Setter getHystrixProperties() {
		return null;
	}
	
    /**
     * Abstract method implementation
     * @see com.flipkart.phantom.task.spi.AbstractHandler#getDetails()
     */
    public String getDetails() {
        String details = "Service Class: " + this.getThriftServiceClass() + "\n";
        details += "Endpoint: " + this.getThriftServer() + ":" + this.getThriftPort() + "\n";
        details += "Timeout: " + this.getThriftTimeoutMillis() + "ms\n";
        details += "Executor Timeout: " + this.getProxyExecutorTimeout() + "ms\n";
        details += "Methods: " + StringUtils.join(processMap.keySet().toArray(new String[]{}),", ") + "\n";
        return details;
    }	
	
	/**
	 * Returns a command specific executor timeout. Default implementation returns {@value HystrixThriftProxy#DEFAULT_EXECUTOR_TIMEOUT}
	 * @param commandName the command name being executed
	 * @return the executor timeout value in milli seconds
	 */
	public int getExecutorTimeout(String commandName) {		
		Integer timeout = this.getExecutorTimeouts().get(commandName); //check if command level timeout has been specified
		if (timeout != null) {
			return timeout;
		}
		if (this.getProxyExecutorTimeout() != HystrixThriftProxy.DEFAULT_EXECUTOR_TIMEOUT) { // check if proxy level timeout is set
			return this.getProxyExecutorTimeout(); 
		}
		return HystrixThriftProxy.DEFAULT_EXECUTOR_TIMEOUT;
	}
	
	/** Getter/Setter methods */	
	public Map<String, Integer> getExecutorTimeouts() {
		return this.executorTimeouts;
	}
	public void setExecutorTimeouts(Map<String, Integer> executorTimeouts) {
		this.executorTimeouts = executorTimeouts;
	}
	public int getProxyExecutorTimeout() {
		return this.proxyExecutorTimeout;
	}
	public void setProxyExecutorTimeout(int proxyExecutorTimeout) {
		this.proxyExecutorTimeout = proxyExecutorTimeout;
	}
	public Integer getProxyThreadPoolSize() {
		return this.proxyThreadPoolSize;
	}
	public void setProxyThreadPoolSize(Integer proxyThreadPoolSize) {
		this.proxyThreadPoolSize = proxyThreadPoolSize;
	}	
	
}