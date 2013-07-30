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

import com.flipkart.phantom.http.spi.HttpProxy;
import com.flipkart.phantom.task.spi.TaskContext;
import org.apache.http.client.methods.HttpRequestBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a repository of HttpProxyExecutor classes which execute HTTP requests using Hystrix commands
 *
 * @author kartikbu
 * @created 16/7/13 1:54 AM
 * @version 1.0
 */
public class HttpProxyExecutorRepository {

    /** logger */
    private static Logger logger = LoggerFactory.getLogger(HttpProxyExecutorRepository.class);

    /** The TaskContext instance */
    private TaskContext taskContext;

    /**
     * Returns a {@link HttpProxyExecutor} for the specified request
     * @param proxy the HttpProxy impl
     * @param request the HTTP request
     * @return an HttpProxyExecutor instance
     */
    public HttpProxyExecutor getHttpProxyExecutor (HttpProxy proxy, String method, String uri, byte[] requestData) throws Exception {
        if (!proxy.isActive()) { // check if the Proxy is indeed active
            proxy.init();
        }
        return new HttpProxyExecutor(proxy, this.taskContext, method, uri, requestData);
    }

    /** Getter/Setter methods */
    public TaskContext getTaskContext() {
        return this.taskContext;
    }
    public void setTaskContext(TaskContext taskContext) {
        this.taskContext = taskContext;
    }
    /** End Getter/Setter methods */
}
