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

package com.flipkart.phantom.thrift.impl.proxy;

import com.flipkart.phantom.task.spi.TaskContext;
import com.flipkart.phantom.thrift.impl.HystrixThriftProxy;
import org.apache.thrift.transport.TTransport;

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
		throw new UnsupportedOperationException("No fallback implementation found!");
	}
}
