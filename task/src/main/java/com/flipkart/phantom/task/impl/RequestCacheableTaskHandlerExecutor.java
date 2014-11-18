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

package com.flipkart.phantom.task.impl;

import com.flipkart.phantom.task.spi.Decoder;
import com.flipkart.phantom.task.spi.TaskContext;

/**
 * <code> RequestCacheableTaskHandlerExecutor </code> is a {@link TaskHandlerExecutor} that implements
 * {@link com.netflix.hystrix.HystrixCommand}'s request caching semantics.
 * Please refer to Hystrix Request Caching Documentation at
 * https://github.com/Netflix/Hystrix/wiki/How-To-Use#Caching
 * https://github.com/Netflix/Hystrix/wiki/How-it-Works#RequestCaching
 *
 * The fundamental idea behind Request caching is to eliminate redundant outbound requests in the context of a single
 * Inbound request. Eg: a servlet executing on a single servlet container thread can eliminate redundant requests to
 * backend services in the context of a single servlet container request.
 */
public class RequestCacheableTaskHandlerExecutor extends TaskHandlerExecutor{

    protected RequestCacheableTaskHandlerExecutor(RequestCacheableHystrixTaskHandler taskHandler, TaskContext taskContext,
                                                  String commandName, int timeout, String threadPoolName, int threadPoolSize,
                                                  TaskRequestWrapper taskRequestWrapper) {
        super(taskHandler, taskContext, commandName, timeout, threadPoolName, threadPoolSize, taskRequestWrapper);
    }

    @SuppressWarnings("rawtypes")
	protected RequestCacheableTaskHandlerExecutor(RequestCacheableHystrixTaskHandler taskHandler, TaskContext taskContext,
                                                  String commandName, int timeout, String threadPoolName, int threadPoolSize,
                                                  TaskRequestWrapper taskRequestWrapper, Decoder decoder) {
        super(taskHandler, taskContext, commandName, timeout, threadPoolName, threadPoolSize, taskRequestWrapper, decoder);
    }

    protected RequestCacheableTaskHandlerExecutor(RequestCacheableHystrixTaskHandler taskHandler, TaskContext taskContext,
                                                  String commandName, TaskRequestWrapper taskRequestWrapper,
                                                  int concurrentRequestSize) {
        super(taskHandler, taskContext, commandName, taskRequestWrapper, concurrentRequestSize);
    }

    @SuppressWarnings("rawtypes")
	protected  RequestCacheableTaskHandlerExecutor(RequestCacheableHystrixTaskHandler taskHandler, TaskContext taskContext,
                                                  String commandName, TaskRequestWrapper taskRequestWrapper,
                                                  int concurrentRequestSize,Decoder decoder) {
        super(taskHandler, taskContext, commandName, taskRequestWrapper, concurrentRequestSize, decoder);
    }

    /**
     * This method returns a valid {@link com.netflix.hystrix.HystrixCommand} cache key
     * from the underlying {@link HystrixTaskHandler} that is used to cache futures of requests,
     * thereby eliminating redundant requests in the context of a single {@link com.netflix.hystrix.strategy.concurrency.HystrixRequestContext}
     * If the task handler is not a {@link HystrixTaskHandler} then it returns null, which bypasses the request
     * caching mechanism in hystrix
     *
     * @return String cache key
     */
    @Override
    protected String getCacheKey(){
        if (this.taskHandler instanceof RequestCacheableHystrixTaskHandler){
            RequestCacheableHystrixTaskHandler requestCacheableHystrixTaskHandler = (RequestCacheableHystrixTaskHandler) this.taskHandler;
            return requestCacheableHystrixTaskHandler.getCacheKey(params, data);
        }
        return null;
    }
}
