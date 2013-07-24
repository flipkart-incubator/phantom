/*
 * Copyright 2012-2015, Flipkart Internet Pvt Ltd. All rights reserved.
 * 
 * This software is the confidential and proprietary information of Flipkart Internet Pvt Ltd. ("Confidential Information").  
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of the license 
 * agreement you entered into with Flipkart.    
 * 
 */
package com.flipkart.phantom.runtime.impl.notifier;

import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixEventType;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * <code>HystrixEventReceiver</code> is a class that receives Hystrix events and logs them.
 * @author devashishshankar
 *
 */
public class HystrixEventReceiver extends HystrixEventNotifier{

	/** Logger for this class*/
	private static final Logger LOGGER = LoggerFactory.getLogger(HystrixEventReceiver.class);
	
	/**
	 * Interface method implementation. Receives events.
	 * @see com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier#markEvent(com.netflix.hystrix.HystrixEventType, com.netflix.hystrix.HystrixCommandKey)
	 */
	@Override
	public void markEvent(HystrixEventType eventType, HystrixCommandKey key) {
		if(eventType==HystrixEventType.THREAD_POOL_REJECTED ) {
			LOGGER.warn("Command: "+key.name()+" failed because of Thread Pool Rejection");			
		} else if(eventType == HystrixEventType.TIMEOUT) {
			LOGGER.warn("Command: "+key.name()+" failed because of Timeout");	
		} else if(eventType == HystrixEventType.SHORT_CIRCUITED) {
			LOGGER.warn("Command: "+key.name()+" was short circuited");	
		}
	}
}
