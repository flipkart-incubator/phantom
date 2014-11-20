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
package com.flipkart.phantom.thrift.impl;

import java.util.Arrays;
import java.util.List;

import org.apache.thrift.transport.TTransport;

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
import com.flipkart.phantom.thrift.impl.interceptor.ThriftClientResponseInterceptor;
import com.flipkart.phantom.thrift.impl.registry.ThriftProxyRegistry;
import com.github.kristofa.brave.BraveContext;
import com.github.kristofa.brave.ClientTracer;
import com.github.kristofa.brave.TraceFilter;
import com.google.common.base.Optional;

/**
 * <code>ThriftProxyExecutorRepository</code> is a repository of {@link ThriftProxyExecutor} instances. Provides methods to control instantiation of
 * ThriftProxyExecutor instances.
 *
 * @author Regunath B
 * @version 1.0, 28 March, 2013
 */
public class ThriftProxyExecutorRepository implements ExecutorRepository<ThriftRequestWrapper,TTransport, ThriftProxy> {

    /** The TaskContext instance */
    private TaskContext taskContext;

    /** Thrift Proxy Registry containing the list of Thrift Proxies */
    private ThriftProxyRegistry registry;

    /** The client request and response interceptors*/
    private List<RequestInterceptor<ThriftRequestWrapper>> requestInterceptors;
    private List<ResponseInterceptor<TTransport>> responseInterceptors;
    
    /** The EventDispatchingSpanCollector instance used in tracing requests*/
    private EventDispatchingSpanCollector eventDispatchingSpanCollector;
    
    /**
     *  Returns a {@link ThriftProxyExecutor} for the specified ThriftProxy and command name
     *
     * @param commandName  commandName the name of the HystrixComman
     * @param proxyName proxyName the HttpProxy name
     * @param requestWrapper requestWrapper Payload
     * @return  a ThriftProxyExecutor instance
     */
     @Override
     public Executor<ThriftRequestWrapper,TTransport> getExecutor(String commandName, String proxyName, RequestWrapper requestWrapper) {
    	 HystrixThriftProxy proxy = (HystrixThriftProxy) registry.getHandler(proxyName);
    	 if (proxy.isActive()) { // check if the ThriftProxy is indeed active
    		 ThriftProxyExecutor executor = new ThriftProxyExecutor(proxy, this.taskContext, commandName, requestWrapper);
    		 return this.wrapExecutorWithInterceptors(executor, proxy);
    	 }
    	 throw new RuntimeException("The ThriftProxy is not active.");
     }

     /**
      * Helper method to wrap the Executor with request and response interceptors 
      */
     private Executor<ThriftRequestWrapper,TTransport> wrapExecutorWithInterceptors(Executor<ThriftRequestWrapper,TTransport> executor, HystrixThriftProxy proxy) {
         if (executor != null) {
         	// we'll add the request and response interceptors that were configured on this repository
         	for (RequestInterceptor<ThriftRequestWrapper> requestInterceptor : this.requestInterceptors) {
         		executor.addRequestInterceptor(requestInterceptor);
         	}
         	for (ResponseInterceptor<TTransport> responseInterceptor : this.responseInterceptors) {
         		executor.addResponseInterceptor(responseInterceptor);
         	}
         }
         // now add the tracing interceptors - has to be an instance specific to each new Executor i.e. Request
         ClientRequestInterceptor<ThriftRequestWrapper> tracingRequestInterceptor = new ClientRequestInterceptor<ThriftRequestWrapper>();
         // check if the request already has tracing context initiated and use it, else create a new one
         Optional<RequestContext> requestContextOptional = executor.getRequestWrapper().getRequestContext();
         BraveContext requestTracingContext = null;
         if (requestContextOptional.isPresent() && requestContextOptional.get().getRequestTracingContext() !=null) {
         	requestTracingContext =  requestContextOptional.get().getRequestTracingContext();
         } else {
         	RequestContext newRequestContext = new RequestContext();
         	newRequestContext.setRequestTracingContext(new BraveContext());
         	requestContextOptional = Optional.of(newRequestContext);
         	executor.getRequestWrapper().setRequestContext(requestContextOptional);
         }
     	List<TraceFilter> traceFilters = Arrays.<TraceFilter>asList(this.registry.getTraceFilterForHandler(proxy.getName()));
     	ClientTracer clientTracer = requestTracingContext.getClientTracer(this.eventDispatchingSpanCollector, traceFilters);
     	tracingRequestInterceptor.setClientTracer(clientTracer);
         executor.addRequestInterceptor(tracingRequestInterceptor);
         executor.addResponseInterceptor(new ThriftClientResponseInterceptor<TTransport>());
         return executor;
     }
     
    /** Start Getter/Setter methods */
    public TaskContext getTaskContext() {
        return this.taskContext;
    }
    public void setTaskContext(TaskContext taskContext) {
        this.taskContext = taskContext;
    }
    public AbstractHandlerRegistry<ThriftProxy> getRegistry() {
        return this.registry;
    }
    public void setRegistry(AbstractHandlerRegistry<ThriftProxy> registry) {
        this.registry =  (ThriftProxyRegistry) registry;
    }
	public void setRequestInterceptors(List<RequestInterceptor<ThriftRequestWrapper>> requestInterceptors) {
		this.requestInterceptors = requestInterceptors;
	}
	public void setResponseInterceptors(List<ResponseInterceptor<TTransport>> responseInterceptors) {
		this.responseInterceptors = responseInterceptors;
	}
	public void setEventDispatchingSpanCollector(EventDispatchingSpanCollector eventDispatchingSpanCollector) {
		this.eventDispatchingSpanCollector = eventDispatchingSpanCollector;
	}    
    /** End End Getter/Setter methods */
}