/*
 * Copyright 2012-2015, Flipkart Internet Pvt Ltd. All rights reserved.
 * 
 * This software is the confidential and proprietary information of Flipkart Internet Pvt Ltd. ("Confidential Information").  
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of the license 
 * agreement you entered into with Flipkart.    
 * 
 */
package com.flipkart.sp.task.spi.thrift;

import org.apache.thrift.transport.TTransport;

import com.flipkart.sp.task.spi.task.TaskContext;
import com.netflix.hystrix.HystrixCommandProperties;

/**
 * An extension of {@link ThriftProxy}. Adds additional methods required by Hystrix. Uses the Thrift call name as the 
 * HystrixCommand name for display on the dashboard.
 * 
 * @author Regunath B
 * @version 1.0, 28 March, 2013
 */
public abstract class HystrixThriftProxy extends ThriftProxy {
	
	/**
	 * Executes this fallback request when {@link #proxyThriftRequest(TTransport)} fails
	 * @param clientTransport the Thrift {@link TTransport} of the invoking client
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
