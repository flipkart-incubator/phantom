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
package com.flipkart.phantom.task.spi.interceptor;

import com.flipkart.phantom.task.spi.RequestWrapper;

/**
 * <code>RequestInterceptor</code> intercepts task request of type RequestWrapper. Provides a hook into the request processing pipeline that may be used
 * to pre-process task requests and perform activities like tracing. 
 * 
 * @author Regunath B
 * @version 1.0, 11th Nov, 2014
 */
public interface RequestInterceptor<T extends RequestWrapper> {

	/**
	 * Intercept and process the task request of type T
	 * @param request the task request
	 */
	public void process(T request);
	
}
