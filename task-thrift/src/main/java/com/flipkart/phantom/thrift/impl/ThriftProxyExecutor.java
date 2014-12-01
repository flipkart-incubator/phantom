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

import java.util.LinkedList;
import java.util.List;

import org.apache.thrift.transport.TTransport;

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
 * <code>ThriftProxyExecutor</code> is an extension of {@link com.netflix.hystrix.HystrixCommand}. It is essentially a
 * wrapper around {@link ThriftProxy}, providing a means for the ThriftProxy to to be called using
 * a Hystrix Command.
 *
 * @author Regunath B
 * @version 1.0, 28 March, 2013
 */
public class ThriftProxyExecutor extends HystrixCommand<TTransport> implements Executor<ThriftRequestWrapper, TTransport> {

    /** The default Hystrix group to which the command belongs, unless otherwise mentioned*/
    public static final String DEFAULT_HYSTRIX_GROUP = "defaultThriftGroup";

    /** The default Hystrix Thread pool to which the command belongs, unless otherwise mentioned */
    public static final String DEFAULT_HYSTRIX_THREAD_POOL = "defaultThriftThreadPool";

    /** The default Hystrix Thread pool to which this command belongs, unless otherwise mentioned */
    public static final int DEFAULT_HYSTRIX_THREAD_POOL_SIZE = 20;

    /** Event Type for publishing all events which are generated here */
    private final static String THRIFT_HANDLER = "THRIFT_HANDLER";

    /** The {@link ThriftProxy} or {@link ThriftProxy} instance which this Command wraps around */
    protected ThriftProxy thriftProxy;

    /** The TaskContext instance*/
    protected TaskContext taskContext;

    /** The client's TTransport*/
    protected TTransport clientTransport;
    
    /** The Thrift request wrapper*/
    protected ThriftRequestWrapper thriftRequestWrapper;

    /** Event which records various paramenters of this request execution & published later */
    protected ServiceProxyEvent.Builder eventBuilder;

    /** List of request and response interceptors */
    private List<RequestInterceptor<ThriftRequestWrapper>> requestInterceptors = new LinkedList<RequestInterceptor<ThriftRequestWrapper>>();
    private List<ResponseInterceptor<TTransport>> responseInterceptors = new LinkedList<ResponseInterceptor<TTransport>>();
    
    /**
     * Constructor for this class.
     * @param hystrixThriftProxy the HystrixThriftProxy that must be wrapped by Hystrix
     * @param taskContext the TaskContext instance that manages the proxies
     * @param commandName the Hystrix command name.
     */
    protected ThriftProxyExecutor(HystrixThriftProxy hystrixThriftProxy, TaskContext taskContext, String commandName, ThriftRequestWrapper thriftRequestWrapper) {
        super(constructHystrixSetter(hystrixThriftProxy,commandName));
        this.thriftProxy = hystrixThriftProxy;
        this.taskContext = taskContext;
        this.thriftRequestWrapper = thriftRequestWrapper;
        this.clientTransport = thriftRequestWrapper.getClientSocket();
        this.eventBuilder = new ServiceProxyEvent.Builder(commandName, THRIFT_HANDLER);
    }

    /**
     * Interface method implementation. @see HystrixCommand#run()
     */
    @Override
    protected TTransport run() {
        this.eventBuilder.withRequestExecutionStartTime(System.currentTimeMillis());
        if (this.thriftRequestWrapper.getRequestContext().isPresent() && this.thriftRequestWrapper.getRequestContext().get().getCurrentServerSpan() != null) {
        	Brave.getServerSpanThreadBinder().setCurrentSpan(this.thriftRequestWrapper.getRequestContext().get().getCurrentServerSpan());
        }
        for (RequestInterceptor<ThriftRequestWrapper> requestInterceptor : this.requestInterceptors) {
        	requestInterceptor.process(this.thriftRequestWrapper);
        }
        TTransport response = null;
        Optional<RuntimeException> transportException = Optional.absent();
        try {
        	response = thriftProxy.doRequest(this.clientTransport);
        }  catch (RuntimeException e) {
        	transportException = Optional.of(e);
        	throw e; // rethrow this for it to handled by other layers in the call stack
        } finally {
	        for (ResponseInterceptor<TTransport> responseInterceptor : this.responseInterceptors) {
	        	responseInterceptor.process(response, transportException);
	        }   
        }
        return response;
    }

    /**
     * Interface method implementation. @see HystrixCommand#getFallback()
     */
    @Override
    protected TTransport getFallback() {
        if(this.thriftProxy instanceof HystrixThriftProxy) {
            HystrixThriftProxy hystrixThriftProxy = (HystrixThriftProxy) this.thriftProxy;
            hystrixThriftProxy.fallbackThriftRequest(this.clientTransport,this.taskContext);
            return this.clientTransport;
        }
        return null;
    }

    /**
     * Helper method that constructs the {@link com.netflix.hystrix.HystrixCommand.Setter} according to the properties set in the {@link ThriftProxy}.
     * Falls back to default properties, if any of them has not been set
     *
     * @param hystrixThriftProxy The {@link ThriftProxy} for whom properties have to be set
     * @return Setter
     */
    private static Setter constructHystrixSetter(HystrixThriftProxy hystrixThriftProxy, String commandName) {
        Setter setter = null;
        if((hystrixThriftProxy.getGroupName()!=null) || !hystrixThriftProxy.getGroupName().equals("")) {
            setter = Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(hystrixThriftProxy.getGroupName()));
        } else {
            setter = Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(hystrixThriftProxy.getName()));
        }
        setter = setter.andCommandKey(HystrixCommandKey.Factory.asKey(commandName));
        if((hystrixThriftProxy.getThreadPoolName()!=null) || !hystrixThriftProxy.getThreadPoolName().equals("")) {
            setter = setter.andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(hystrixThriftProxy.getThreadPoolName()));
        } else {
            setter = setter.andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(DEFAULT_HYSTRIX_THREAD_POOL));
        }
        Integer threadPoolSize = hystrixThriftProxy.getProxyThreadPoolSize() == null ? ThriftProxyExecutor.DEFAULT_HYSTRIX_THREAD_POOL_SIZE :  hystrixThriftProxy.getProxyThreadPoolSize();
        setter = setter.andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter().withCoreSize(threadPoolSize));
        setter = setter.andCommandPropertiesDefaults(HystrixCommandProperties.Setter().withExecutionIsolationThreadTimeoutInMilliseconds(hystrixThriftProxy.getExecutorTimeout(commandName)));
        if((hystrixThriftProxy.getHystrixProperties()!=null)) {
            setter = setter.andCommandPropertiesDefaults(hystrixThriftProxy.getHystrixProperties());
        }
        return setter;
    }

    /**
     * Interface method implementation. Adds the RequestInterceptor to the list of request interceptors that will be invoked
     * @see com.flipkart.phantom.task.spi.Executor#addRequestInterceptor(com.flipkart.phantom.task.spi.interceptor.RequestInterceptor)
     */
    public void addRequestInterceptor(RequestInterceptor<ThriftRequestWrapper> requestInterceptor) {    	
    	this.requestInterceptors.add(requestInterceptor);
    }

    /**
     * Interface method implementation. Adds the ResponseInterceptor to the list of response interceptors that will be invoked
     * @see com.flipkart.phantom.task.spi.Executor#addResponseInterceptor(com.flipkart.phantom.task.spi.interceptor.ResponseInterceptor)
     */
    public void addResponseInterceptor(ResponseInterceptor<TTransport> responseInterceptor){
    	this.responseInterceptors.add(responseInterceptor);
    }
    
    /**
     * Interface method implementation. Returns the name of the ThriftProxy used by this Executor
     * @see com.flipkart.phantom.task.spi.Executor#getServiceName()
     */
    public Optional<String> getServiceName() {
    	return Optional.of(this.thriftProxy.getName());
    }  
    
    /**
     * Interface method implementation. Returns the ThriftRequestWrapper instance that this Executor was created with
     * @see com.flipkart.phantom.task.spi.Executor#getRequestWrapper()
     */
    public ThriftRequestWrapper getRequestWrapper() {
    	return this.thriftRequestWrapper;
    }
    
    /**Getter/Setter methods */
    public TTransport getClientTransport() {
        return this.clientTransport;
    }
    public void setClientTransport(TTransport clientTransport) {
        this.clientTransport = clientTransport;
    }
    public ServiceProxyEvent.Builder getEventBuilder() {
        return eventBuilder;
    }
    /** End Getter/Setter methods */

}