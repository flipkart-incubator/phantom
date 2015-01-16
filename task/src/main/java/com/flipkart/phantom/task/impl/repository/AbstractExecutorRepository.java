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

package com.flipkart.phantom.task.impl.repository;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import com.flipkart.phantom.task.impl.TaskHandler;
import com.flipkart.phantom.task.impl.collector.EventDispatchingSpanCollector;
import com.flipkart.phantom.task.impl.interceptor.AbstractClientResponseInterceptor;
import com.flipkart.phantom.task.impl.interceptor.ClientRequestInterceptor;
import com.flipkart.phantom.task.spi.AbstractHandler;
import com.flipkart.phantom.task.spi.Executor;
import com.flipkart.phantom.task.spi.RequestContext;
import com.flipkart.phantom.task.spi.RequestWrapper;
import com.flipkart.phantom.task.spi.TaskContext;
import com.flipkart.phantom.task.spi.interceptor.RequestInterceptor;
import com.flipkart.phantom.task.spi.interceptor.ResponseInterceptor;
import com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry;
import com.flipkart.phantom.task.spi.repository.ExecutorRepository;
import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.ServerTracer;
import com.github.kristofa.brave.TraceFilter;
import com.google.common.base.Optional;


/**
 * <code>AbstractExecutorRepository</code> is an implementation of {@link ExecutorRepository} that hs data and behavior to all protocol specific sub-types.
 *
 * @author Regunath B
 * @version 1.0, 24th Nov, 2014
 */
public abstract class AbstractExecutorRepository<T extends RequestWrapper,S, R extends AbstractHandler> implements ExecutorRepository<T,S,R>, InitializingBean {

    /** The registry holding the names of the TaskHandler */
    protected AbstractHandlerRegistry<R> registry;

    /** The taskContext being passed to the Handlers, providing a way for communication to the Container */
    protected TaskContext taskContext;
	
    /** The client request and response interceptors*/
    protected List<RequestInterceptor<T>> requestInterceptors = new LinkedList<RequestInterceptor<T>>();
    protected List<ResponseInterceptor<S>> responseInterceptors = new LinkedList<ResponseInterceptor<S>>();
    
    /** The EventDispatchingSpanCollector instance used in tracing requests*/
    protected EventDispatchingSpanCollector eventDispatchingSpanCollector;

    /**
     * Interface method implementation. Checks if all mandatory properties have been set
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(this.registry, "The 'registry' may not be null");
        Assert.notNull(this.taskContext, "The 'taskContext' may not be null");
        Assert.notNull(this.eventDispatchingSpanCollector, "The 'eventDispatchingSpanCollector' may not be null");
    }
    
    /**
     * Helper method to wrap the Executor with request and response interceptors 
     */
    protected Executor<T,S> wrapExecutorWithInterceptors(Executor<T,S> executor, R handler, 
    		ClientRequestInterceptor<T> tracingRequestInterceptor, AbstractClientResponseInterceptor<S> tracingResponseInterceptor) {
        if (executor != null) {
        	// we'll add the request and response interceptors that were configured on this repository
        	for (RequestInterceptor<T> requestInterceptor : this.requestInterceptors) {
        		executor.addRequestInterceptor(requestInterceptor);
        	}
        	for (ResponseInterceptor<S> responseInterceptor : this.responseInterceptors) {
        		executor.addResponseInterceptor(responseInterceptor);
        	}
	        // now add the tracing interceptors - has to be an instance specific to each new Executor 
	        Optional<RequestContext> requestContextOptional = executor.getRequestWrapper().getRequestContext();
	    	List<TraceFilter> traceFilters = Arrays.<TraceFilter>asList(this.registry.getTraceFilterForHandler(handler.getName()));
	    	// The request did not come through a server/service call i.e. not via Netty Channel handlers 
	        if (!requestContextOptional.isPresent()) {
	        	RequestContext newRequestContext = new RequestContext();
	        	requestContextOptional = Optional.of(newRequestContext);
	        	executor.getRequestWrapper().setRequestContext(requestContextOptional);
	        }
        	final String serviceName = executor.getServiceName().isPresent() ? executor.getServiceName().get() : Executor.DEFAULT_SERVICE_NAME;
	        if (requestContextOptional.get().getCurrentServerSpan() == null) {
	        	final ServerTracer serverTracer = Brave.getServerTracer(this.eventDispatchingSpanCollector, traceFilters);
	        	// we dont know what server trace this request was part of, so set it to unknown 
	        	serverTracer.setStateUnknown(handler.getName());
	        	// set the endpoint to default
	        	Brave.getEndPointSubmitter().submit(TaskHandler.DEFAULT_HOST, TaskHandler.DEFAULT_PORT, serviceName);
	        	// Set the current server span on the request context 
	        	requestContextOptional.get().setCurrentServerSpan(Brave.getServerSpanThreadBinder().getCurrentServerSpan());
	        }
	        // Set the client endpoint on the request context
        	requestContextOptional.get().setCurrentClientEndpoint(new RequestContext.ServiceEndpoint(handler.getHost(), handler.getPort(), serviceName));
	    	tracingRequestInterceptor.setEventDispatchingSpanCollector(this.eventDispatchingSpanCollector);
	    	tracingRequestInterceptor.setTraceFilters(traceFilters);
	        executor.addRequestInterceptor(tracingRequestInterceptor);
	        tracingResponseInterceptor.setEventDispatchingSpanCollector(this.eventDispatchingSpanCollector);
	    	tracingResponseInterceptor.setTraceFilters(traceFilters);
	        executor.addResponseInterceptor(tracingResponseInterceptor);
        }
        return executor;
    }
    
    /** Getter/Setter methods */
    public AbstractHandlerRegistry<R> getRegistry() {
        return registry;
    }
    public void setRegistry(AbstractHandlerRegistry<R> registry) {
        this.registry = registry;
    }
    public TaskContext getTaskContext() {
        return this.taskContext;
    }
    public void setTaskContext(TaskContext taskContext) {
        this.taskContext = taskContext;
    }
	public void setRequestInterceptors(List<RequestInterceptor<T>> requestInterceptors) {
		this.requestInterceptors = requestInterceptors;
	}
	public void setResponseInterceptors(List<ResponseInterceptor<S>> responseInterceptors) {
		this.responseInterceptors = responseInterceptors;
	}
	public void setEventDispatchingSpanCollector(EventDispatchingSpanCollector eventDispatchingSpanCollector) {
		this.eventDispatchingSpanCollector = eventDispatchingSpanCollector;
	}    	
    /** End Getter/Setter methods */
    
}
