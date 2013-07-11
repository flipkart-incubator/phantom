/*
 * Copyright 2012-2015, Flipkart Internet Pvt Ltd. All rights reserved.
 * 
 * This software is the confidential and proprietary information of Flipkart Internet Pvt Ltd. ("Confidential Information").  
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of the license 
 * agreement you entered into with Flipkart.    
 * 
 */
package com.flipkart.sp.task.impl.thrift.proxies;

import org.apache.thrift.transport.TTransport;

import com.flipkart.sp.task.spi.task.TaskContext;
import com.flipkart.sp.task.spi.thrift.HystrixThriftProxy;

/**
 * <code>DefaultThriftProxy</code> is an extension of HystrixThriftProxy. It's a default implementation.
 * 
 * @author devashishshankar
 * @version 1.0, 04 April 2013
 */
public class DefaultThriftProxy extends HystrixThriftProxy {
	
	/** FallBack, to be executed when command fails */
	@Override
	public void fallbackThriftRequest(TTransport clientTransport, TaskContext taskContext) {
	
	}
}
