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

import com.flipkart.phantom.http.impl.registry.HttpProxyRegistry;
import com.flipkart.phantom.task.spi.TaskContext;

/**
 * Provides a repository of HttpProxyExecutor classes which execute HTTP requests using Hystrix commands
 *
 * @author kartikbu
 * @created 16/7/13 1:54 AM
 * @version 1.0
 */
public class HttpProxyExecutorRepository {

    /** repository */
    private HttpProxyRegistry registry;

    /** The TaskContext instance */
    private TaskContext taskContext;

    /**
     * Returns {@link HttpProxyExecutor} for the specified request
     * @param name the HttpProxy name
     * @param method the HTTP request method
     * @param uri the HTTP request URI
     * @param requestData the HTTP request payload
     * @return an HttpProxyExecutor instance
     */
    public HttpProxyExecutor getHttpProxyExecutor (String name, String method, String uri, byte[] requestData) throws Exception {
        HttpProxy proxy = (HttpProxy) registry.getHandler(name);
        if (proxy.isActive()) {
            return new HttpProxyExecutor(proxy, this.taskContext, method, uri, requestData);
        }
        throw new RuntimeException("The HttpProxy is not active.");
    }

    /** Getter/Setter methods */
    public HttpProxyRegistry getRegistry() {
        return registry;
    }
    public void setRegistry(HttpProxyRegistry registry) {
        this.registry = registry;
    }
    public TaskContext getTaskContext() {
        return this.taskContext;
    }
    public void setTaskContext(TaskContext taskContext) {
        this.taskContext = taskContext;
    }
    /** End Getter/Setter methods */
}
