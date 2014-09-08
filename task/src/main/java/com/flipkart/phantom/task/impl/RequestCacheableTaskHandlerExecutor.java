package com.flipkart.phantom.task.impl;


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

    protected RequestCacheableTaskHandlerExecutor(RequestCacheableHystrixTaskHandler taskHandler, TaskContext taskContext, String commandName, int timeout, String threadPoolName, int threadPoolSize, TaskRequestWrapper taskRequestWrapper) {
        super(taskHandler, taskContext, commandName, timeout, threadPoolName, threadPoolSize, taskRequestWrapper);
    }

    protected RequestCacheableTaskHandlerExecutor(RequestCacheableHystrixTaskHandler taskHandler, TaskContext taskContext, String commandName, TaskRequestWrapper taskRequestWrapper, int concurrentRequestSize) {
        super(taskHandler, taskContext, commandName, taskRequestWrapper, concurrentRequestSize);
    }

    public RequestCacheableTaskHandlerExecutor(RequestCacheableHystrixTaskHandler taskHandler, TaskContext taskContext, String commandName, int executorTimeout, TaskRequestWrapper taskRequestWrapper) {
        super(taskHandler, taskContext, commandName, executorTimeout, taskRequestWrapper);
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
            return requestCacheableHystrixTaskHandler.getCacheKey(params);
        }
        return null;
    }

}
