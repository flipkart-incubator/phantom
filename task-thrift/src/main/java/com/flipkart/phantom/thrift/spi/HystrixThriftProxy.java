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
package com.flipkart.phantom.thrift.spi;

import com.flipkart.phantom.task.spi.TaskContext;
import com.netflix.hystrix.HystrixCommandProperties;
import org.apache.thrift.transport.TTransport;

/**
 * An extension of {@link ThriftProxy}. Adds additional methods required by Hystrix. Uses the Thrift call name as the 
 * HystrixCommand name for display on the dashboard.
 * 
 * @author Regunath B
 * @version 1.0, 28 March, 2013
 */
public abstract class HystrixThriftProxy extends ThriftProxy {
	
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
}
