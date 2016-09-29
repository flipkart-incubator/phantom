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

package com.flipkart.phantom.http.impl;

import org.apache.http.HttpResponse;

import com.flipkart.phantom.http.impl.interceptor.HttpClientResponseInterceptor;
import com.flipkart.phantom.task.impl.interceptor.ClientRequestInterceptor;
import com.flipkart.phantom.task.impl.repository.AbstractExecutorRepository;
import com.flipkart.phantom.task.spi.Executor;

/**
 * Provides a repository of HttpProxyExecutor classes which execute HTTP requests using Hystrix commands
 *
 * @author kartikbu
 * @created 16/7/13 1:54 AM
 * @version 1.0
 */
public class HttpProxyExecutorRepository extends AbstractExecutorRepository<HttpRequestWrapper,HttpResponse, HttpProxy>{

    /**
     * Returns {@link Executor} for the specified requestWrapper
     * @param commandName command Name as specified by Executor. Not used in this case.
     * @param proxyName   proxyName the HttpProxy name
     * @param requestWrapper requestWrapper Object containing requestWrapper Data
     * @return  an {@link HttpProxyExecutor} instance
     */
    public Executor<HttpRequestWrapper,HttpResponse> getExecutor (String commandName, String proxyName, HttpRequestWrapper requestWrapper)  {
        HttpProxy proxy = (HttpProxy) registry.getHandler(proxyName);
        if (proxy.isActive()) {
        	HttpProxyExecutor executor = new HttpProxyExecutor(proxy, this.taskContext, requestWrapper);
            return this.wrapExecutorWithInterceptors(executor,proxy);
        }
        throw new RuntimeException("The HttpProxy is not active.");
    }

    /**
     * Helper method to wrap the Executor with request and response interceptors 
     */
    private Executor<HttpRequestWrapper,HttpResponse> wrapExecutorWithInterceptors(Executor<HttpRequestWrapper,HttpResponse> executor, HttpProxy proxy) {
        ClientRequestInterceptor<HttpRequestWrapper> tracingRequestInterceptor = new ClientRequestInterceptor<HttpRequestWrapper>();
        HttpClientResponseInterceptor<HttpResponse> tracingResponseInterceptor = new HttpClientResponseInterceptor<HttpResponse>();
        return this.wrapExecutorWithInterceptors(executor, proxy, tracingRequestInterceptor, tracingResponseInterceptor);
    }
    
}
