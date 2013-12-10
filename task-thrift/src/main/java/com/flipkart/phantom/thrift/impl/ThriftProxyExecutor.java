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

import com.flipkart.phantom.task.spi.Executor;
import com.flipkart.phantom.task.spi.RequestWrapper;
import com.flipkart.phantom.task.spi.TaskContext;
import com.netflix.hystrix.*;
import org.apache.thrift.transport.TTransport;

/**
 * <code>ThriftProxyExecutor</code> is an extension of {@link com.netflix.hystrix.HystrixCommand}. It is essentially a
 * wrapper around {@link ThriftProxy}, providing a means for the ThriftProxy to to be called using
 * a Hystrix Command.
 *
 * @author Regunath B
 * @version 1.0, 28 March, 2013
 */
public class ThriftProxyExecutor extends HystrixCommand<TTransport> implements Executor{

    /** The default Hystrix group to which the command belongs, unless otherwise mentioned*/
    public static final String DEFAULT_HYSTRIX_GROUP = "defaultThriftGroup";

    /** The default Hystrix Thread pool to which the command belongs, unless otherwise mentioned */
    public static final String DEFAULT_HYSTRIX_THREAD_POOL = "defaultThriftThreadPool";

    /** The default Hystrix Thread pool to which this command belongs, unless otherwise mentioned */
    public static final int DEFAULT_HYSTRIX_THREAD_POOL_SIZE = 20;

    /** The {@link ThriftProxy} or {@link ThriftProxy} instance which this Command wraps around */
    protected ThriftProxy thriftProxy;

    /** The TaskContext instance*/
    protected TaskContext taskContext;

    /** The client's TTransport*/
    protected TTransport clientTransport;

    /**
     * Constructor for this class.
     * @param hystrixThriftProxy the HystrixThriftProxy that must be wrapped by Hystrix
     * @param taskContext the TaskContext instance that manages the proxies
     * @param commandName the Hystrix command name.
     */
    protected ThriftProxyExecutor(HystrixThriftProxy hystrixThriftProxy, TaskContext taskContext, String commandName, RequestWrapper requestWrapper) {
        super(constructHystrixSetter(hystrixThriftProxy,commandName));
        this.thriftProxy = hystrixThriftProxy;
        this.taskContext = taskContext;

        ThriftRequestWrapper thriftRequestWrapper = (ThriftRequestWrapper)requestWrapper;
        this.clientTransport = thriftRequestWrapper.getClientSocket();
    }

    /**
     * Interface method implementation. @see HystrixCommand#run()
     */
    @SuppressWarnings("rawtypes")
    @Override
    protected TTransport run() {
          return thriftProxy.doRequest(this.clientTransport);
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

    /**Getter/Setter methods */
    public TTransport getClientTransport() {
        return this.clientTransport;
    }
    public void setClientTransport(TTransport clientTransport) {
        this.clientTransport = clientTransport;
    }
    /** End Getter/Setter methods */

}