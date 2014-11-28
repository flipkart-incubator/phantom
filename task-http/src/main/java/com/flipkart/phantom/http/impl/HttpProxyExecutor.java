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

import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpResponse;

import com.flipkart.phantom.event.ServiceProxyEvent;
import com.flipkart.phantom.task.spi.Executor;
import com.flipkart.phantom.task.spi.TaskContext;
import com.flipkart.phantom.task.spi.interceptor.RequestInterceptor;
import com.flipkart.phantom.task.spi.interceptor.ResponseInterceptor;
import com.github.kristofa.brave.Brave;
import com.google.common.base.Optional;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.HystrixThreadPoolProperties;


/**
 * Implements the HystrixCommand class for executing HTTP proxy requests
 *
 * @author kartikbu
 * @version 1.0
 * @created 16/7/13 1:54 AM
 */
public class HttpProxyExecutor extends HystrixCommand<HttpResponse> implements Executor<HttpRequestWrapper, HttpResponse> {
	
    /** Event Type for publishing all events which are generated here */
    private final static String HTTP_HANDLER = "HTTP_HANDLER";
    
    /** Http Request Wrapper */
    private HttpRequestWrapper httpRequestWrapper;

    /** the proxy client */
    private HttpProxy proxy;

    /** Event which records various parameters of this request execution & published later */
    protected ServiceProxyEvent.Builder eventBuilder;

    /** List of request and response interceptors */
    private List<RequestInterceptor<HttpRequestWrapper>> requestInterceptors = new LinkedList<RequestInterceptor<HttpRequestWrapper>>();
    private List<ResponseInterceptor<HttpResponse>> responseInterceptors = new LinkedList<ResponseInterceptor<HttpResponse>>();
    
    /** only constructor uses the proxy client, task context and the http requestWrapper */
    public HttpProxyExecutor(HttpProxy proxy, TaskContext taskContext, HttpRequestWrapper httpRequestWrapper) {
        super(
            Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(proxy.getGroupKey()))
                    .andCommandKey(HystrixCommandKey.Factory.asKey(proxy.getCommandKey()))
                    .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(proxy.getThreadPoolKey()))
                    .andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter().withCoreSize(proxy.getThreadPoolSize()))
                    .andCommandPropertiesDefaults(HystrixCommandProperties.Setter().withExecutionIsolationThreadTimeoutInMilliseconds
                            (proxy.getPool().getOperationTimeout())));
        this.proxy = proxy;
        this.httpRequestWrapper = httpRequestWrapper;
        this.eventBuilder = new ServiceProxyEvent.Builder(this.httpRequestWrapper.getUri(), HTTP_HANDLER);
    }

    /**
     * Interface method implementation
     * @return response HttpResponse for the give request
     * @throws Exception
     */
    @Override
    protected HttpResponse run() throws Exception {
        this.eventBuilder.withRequestExecutionStartTime(System.currentTimeMillis());
        if (this.httpRequestWrapper.getRequestContext().isPresent() && this.httpRequestWrapper.getRequestContext().get().getCurrentServerSpan() != null) {
        	Brave.getServerSpanThreadBinder().setCurrentSpan(this.httpRequestWrapper.getRequestContext().get().getCurrentServerSpan());
        }
        Brave.getEndPointSubmitter().submit(this.proxy.getHost(),this.proxy.getPort(),this.proxy.getName());
        for (RequestInterceptor<HttpRequestWrapper> requestInterceptor : this.requestInterceptors) {
        	requestInterceptor.process(this.httpRequestWrapper);
        }
        Optional<RuntimeException> transportException = Optional.absent();
        HttpResponse response = null;
        try {
        	response = this.proxy.doRequest(this.httpRequestWrapper);
        } catch (RuntimeException e) {
        	transportException = Optional.of(e);
        	throw e; // rethrow this for it to handled by other layers in the call stack
        } finally {
	        for (ResponseInterceptor<HttpResponse> responseInterceptor : this.responseInterceptors) {
	        	responseInterceptor.process(response, transportException);
	        }
        }
        return response;
    }
    
    /**
     * Interface method implementation. Returns the name of the {@link HttpProxy} used by this Executor
     * @see com.flipkart.phantom.task.spi.Executor#getServiceName()
     */
    public Optional<String> getServiceName() {
    	return Optional.of(this.proxy.getName());
    }

    /**
     * Interface method implementation. Returns the HttpRequestWrapper instance that this Executor was created with
     * @see com.flipkart.phantom.task.spi.Executor#getRequestWrapper()
     */
    public HttpRequestWrapper getRequestWrapper() {
    	return this.httpRequestWrapper;
    }
    
    /**
     * Interface method implementation. Adds the RequestInterceptor to the list of request interceptors that will be invoked
     * @see com.flipkart.phantom.task.spi.Executor#addRequestInterceptor(com.flipkart.phantom.task.spi.interceptor.RequestInterceptor)
     */
    public void addRequestInterceptor(RequestInterceptor<HttpRequestWrapper> requestInterceptor) {    	
    	this.requestInterceptors.add(requestInterceptor);
    }

    /**
     * Interface method implementation. Adds the ResponseInterceptor to the list of response interceptors that will be invoked
     * @see com.flipkart.phantom.task.spi.Executor#addResponseInterceptor(com.flipkart.phantom.task.spi.interceptor.ResponseInterceptor)
     */
    public void addResponseInterceptor(ResponseInterceptor<HttpResponse> responseInterceptor){
    	this.responseInterceptors.add(responseInterceptor);
    }
    
    /**
     * Interface method implementation
     * @return response HttpResponse from the fallback
     * @throws Exception
     */
    @Override
    protected HttpResponse getFallback() {
        return proxy.fallbackRequest(this.httpRequestWrapper);
    }

    /**
     * Retruns the event builder instance
     */
    public ServiceProxyEvent.Builder getEventBuilder() {
        return eventBuilder;
    }
}
