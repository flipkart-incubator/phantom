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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpResponse;

import com.flipkart.phantom.http.impl.interceptor.HttpClientRequestInterceptor;
import com.flipkart.phantom.http.impl.interceptor.HttpClientResponseInterceptor;
import com.flipkart.phantom.http.impl.registry.HttpProxyRegistry;
import com.flipkart.phantom.task.impl.collector.EventDispatchingSpanCollector;
import com.flipkart.phantom.task.impl.interceptor.ClientRequestInterceptor;
import com.flipkart.phantom.task.spi.Executor;
import com.flipkart.phantom.task.spi.RequestContext;
import com.flipkart.phantom.task.spi.RequestWrapper;
import com.flipkart.phantom.task.spi.TaskContext;
import com.flipkart.phantom.task.spi.interceptor.RequestInterceptor;
import com.flipkart.phantom.task.spi.interceptor.ResponseInterceptor;
import com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry;
import com.flipkart.phantom.task.spi.repository.ExecutorRepository;
import com.github.kristofa.brave.BraveContext;
import com.github.kristofa.brave.ClientTracer;
import com.github.kristofa.brave.EndPointSubmitter;
import com.github.kristofa.brave.ServerTracer;
import com.github.kristofa.brave.TraceFilter;
import com.google.common.base.Optional;

/**
 * Provides a repository of HttpProxyExecutor classes which execute HTTP requests using Hystrix commands
 *
 * @author kartikbu
 * @created 16/7/13 1:54 AM
 * @version 1.0
 */
public class HttpProxyExecutorRepository implements ExecutorRepository<HttpRequestWrapper,HttpResponse, HttpProxy>{

    /** repository */
    private HttpProxyRegistry registry;

    /** The TaskContext instance */
    private TaskContext taskContext;

    /** The client request and response interceptors*/
    private List<RequestInterceptor<HttpRequestWrapper>> requestInterceptors = new LinkedList<RequestInterceptor<HttpRequestWrapper>>();
    private List<ResponseInterceptor<HttpResponse>> responseInterceptors = new LinkedList<ResponseInterceptor<HttpResponse>>();
    
    /** The EventDispatchingSpanCollector instance used in tracing requests*/
    private EventDispatchingSpanCollector eventDispatchingSpanCollector;
    
    /**
     * Returns {@link Executor} for the specified requestWrapper
     * @param commandName command Name as specified by Executor. Not used in this case.
     * @param proxyName   proxyName the HttpProxy name
     * @param requestWrapper requestWrapper Object containing requestWrapper Data
     * @return  an {@link HttpProxyExecutor} instance
     */
    public Executor<HttpRequestWrapper,HttpResponse> getExecutor (String commandName, String proxyName, RequestWrapper requestWrapper)  {
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
        if (executor != null) {
        	// we'll add the request and response interceptors that were configured on this repository
        	for (RequestInterceptor<HttpRequestWrapper> requestInterceptor : this.requestInterceptors) {
        		executor.addRequestInterceptor(requestInterceptor);
        	}
        	for (ResponseInterceptor<HttpResponse> responseInterceptor : this.responseInterceptors) {
        		executor.addResponseInterceptor(responseInterceptor);
        	}
        }
        // now add the tracing interceptors - has to be an instance specific to each new Executor i.e. Request
        ClientRequestInterceptor<HttpRequestWrapper> tracingRequestInterceptor = new HttpClientRequestInterceptor<HttpRequestWrapper>();
        // check if the request already has tracing context initiated and use it, else create a new one
        Optional<RequestContext> requestContextOptional = executor.getRequestWrapper().getRequestContext();
        BraveContext requestTracingContext = null;
    	List<TraceFilter> traceFilters = Arrays.<TraceFilter>asList(this.registry.getTraceFilterForHandler(proxy.getName()));
        if (requestContextOptional.isPresent() && requestContextOptional.get().getRequestTracingContext() !=null) {
        	requestTracingContext =  requestContextOptional.get().getRequestTracingContext();
        } else {
        	RequestContext newRequestContext = new RequestContext();
        	requestTracingContext = new BraveContext();
        	// we dont know what server trace this request was part of, so set it to unknown and set the endpoint to that of the proxy
        	final ServerTracer serverTracer = requestTracingContext.getServerTracer(this.eventDispatchingSpanCollector, traceFilters);
        	serverTracer.setStateUnknown(proxy.getName());
            final EndPointSubmitter endPointSubmitter = requestTracingContext.getEndPointSubmitter();
            endPointSubmitter.submit(proxy.getPool().getHost(),proxy.getPool().getPort(),proxy.getName());        	
        	newRequestContext.setRequestTracingContext(requestTracingContext);
        	requestContextOptional = Optional.of(newRequestContext);
        	executor.getRequestWrapper().setRequestContext(requestContextOptional);
        }
    	ClientTracer clientTracer = requestTracingContext.getClientTracer(this.eventDispatchingSpanCollector, traceFilters);
    	tracingRequestInterceptor.setClientTracer(clientTracer);
        executor.addRequestInterceptor(tracingRequestInterceptor);
        HttpClientResponseInterceptor<HttpResponse> responseInterceptor = new HttpClientResponseInterceptor<HttpResponse>();
        responseInterceptor.setClientTracer(clientTracer);
        executor.addResponseInterceptor(responseInterceptor);
        return executor;
    }
    
    /** Getter/Setter methods */
    public AbstractHandlerRegistry<HttpProxy> getRegistry() {
        return registry;
    }
    public void setRegistry(AbstractHandlerRegistry<HttpProxy> registry) {
        this.registry = (HttpProxyRegistry)registry;
    }
    public TaskContext getTaskContext() {
        return this.taskContext;
    }
    public void setTaskContext(TaskContext taskContext) {
        this.taskContext = taskContext;
    }
	public void setRequestInterceptors(List<RequestInterceptor<HttpRequestWrapper>> requestInterceptors) {
		this.requestInterceptors = requestInterceptors;
	}
	public void setResponseInterceptors(List<ResponseInterceptor<HttpResponse>> responseInterceptors) {
		this.responseInterceptors = responseInterceptors;
	}
	public void setEventDispatchingSpanCollector(EventDispatchingSpanCollector eventDispatchingSpanCollector) {
		this.eventDispatchingSpanCollector = eventDispatchingSpanCollector;
	}    
    /** End Getter/Setter methods */
}
