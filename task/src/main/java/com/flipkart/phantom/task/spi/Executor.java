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

package com.flipkart.phantom.task.spi;

import com.flipkart.phantom.event.ServiceProxyEvent;
import com.flipkart.phantom.task.spi.interceptor.RequestInterceptor;
import com.flipkart.phantom.task.spi.interceptor.ResponseInterceptor;
import com.google.common.base.Optional;

/**
 * <code>Executor</code> is an interface which executes any task with request of type RequestWrapper and returns the result of Type S.
 * This interface provides a way of decoupling task submission from the mechanics of how each task will be run.
 * Also supports request and response processing interceptors.
 *
 * @author : arya.ketan
 * @author : Regunath B
 * @version : 2.0
 * @date : 11/11/14
 */
public interface Executor<T extends RequestWrapper,S> {
	
	/**
	 * Executes a given task and returns the result
	 * @return task execution result
	 */
    public S execute();

    /**
     * Gets the ServiceProxyEvent builder implementation
     * @return the ServiceProxyEvent builder implementation
     */
    public ServiceProxyEvent.Builder getEventBuilder();
    
    /**
     * Adds the specified RequestInterceptor to the request processing path
     * @param requestInterceptor the RequestInterceptor to process the request
     */
    public void addRequestInterceptor(RequestInterceptor<T> requestInterceptor);
    
    /**
     * Adds the specified ResponseInterceptor to the response processing path
     * @param responseInterceptor the ResponseInterceptor to process the response
     */
    public void addResponseInterceptor(ResponseInterceptor<S> responseInterceptor);
    
    /**
     * Returns the service name for this executor
     * @param requestWrapper the request wrapper
     * @return an optional service name
     */
    public Optional<String> getServiceName(T requestWrapper);
    
}
