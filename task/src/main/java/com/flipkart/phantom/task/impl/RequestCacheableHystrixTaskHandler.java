package com.flipkart.phantom.task.impl;


import java.util.Map;

public abstract class RequestCacheableHystrixTaskHandler extends HystrixTaskHandler{

    /**
     * This method returns a valid {@link com.netflix.hystrix.HystrixCommand} cache key
     * that is used to cache futures of requests, thereby eliminating redundant requests
     * in the context of a single {@link com.netflix.hystrix.strategy.concurrency.HystrixRequestContext}
     * If it is not Overriden in the derived class, it returns null, which bypasses the request
     * caching mechanism in hystrix
     *
     * @return String cache key
     */
    public String getCacheKey(Map<String,String> params){
        return null;
    }

}
