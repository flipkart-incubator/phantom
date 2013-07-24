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
