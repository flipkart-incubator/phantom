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

import org.apache.thrift.transport.TTransport;

import com.flipkart.phantom.task.impl.interceptor.ClientRequestInterceptor;
import com.flipkart.phantom.task.impl.repository.AbstractExecutorRepository;
import com.flipkart.phantom.task.spi.Executor;
import com.flipkart.phantom.task.spi.RequestWrapper;
import com.flipkart.phantom.thrift.impl.interceptor.ThriftClientResponseInterceptor;

/**
 * <code>ThriftProxyExecutorRepository</code> is a repository of {@link ThriftProxyExecutor} instances. Provides methods to control instantiation of
 * ThriftProxyExecutor instances.
 *
 * @author Regunath B
 * @version 1.0, 28 March, 2013
 */
public class ThriftProxyExecutorRepository extends AbstractExecutorRepository<ThriftRequestWrapper,TTransport, ThriftProxy> {

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
         ClientRequestInterceptor<ThriftRequestWrapper> tracingRequestInterceptor = new ClientRequestInterceptor<ThriftRequestWrapper>();
         ThriftClientResponseInterceptor<TTransport> tracingResponseInterceptor = new ThriftClientResponseInterceptor<TTransport>();
         return this.wrapExecutorWithInterceptors(executor, proxy, tracingRequestInterceptor, tracingResponseInterceptor);
     }
     
}