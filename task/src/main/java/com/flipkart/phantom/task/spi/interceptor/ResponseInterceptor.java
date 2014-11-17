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

/**
 * <code>ResponseInterceptor</code> intercepts task execution responses of type S. Provides a hook into the task response processing pipeline that may be used
 * to post-process task responses and perform activities like tracing. 
 * 
 * @author Regunath B
 * @version 1.0, 11th Nov, 2014
 */
public interface ResponseInterceptor<S> {

	/**
	 * Intercept and process the task response of type S
	 * @param response the task response
	 */
	public void process(S response);
	
}