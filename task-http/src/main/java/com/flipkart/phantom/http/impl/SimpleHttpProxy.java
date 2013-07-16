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
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

/**
 * A simple implementation for the HttpProxy abstract class
 *
 * @author kartikbu
 * @created 16/7/13 1:54 AM
 * @version 1.0
 */
public class SimpleHttpProxy extends HttpProxy {

    /**
     * Abstract method implementation
     * @param request request which failed
     * @param taskContext current task context
     * @return null
     */
    @Override
    public HttpResponse fallbackRequest(HttpRequest request, TaskContext taskContext) {
        return null;
    }

    /**
     * Abstract method implementation
     * @return String Group name
     */
    @Override
    public String getGroupKey() {
        return "SimpleHttpProxy";
    }

    /**
     * Abstract method implementation
     * @return String Command name
     */
    @Override
    public String getCommandKey() {
        return this.getName() + "HttpPool";
    }

    /**
     * Abstract method implementation
     * @return String Thread pool name
     */
    @Override
    public String getThreadPoolKey() {
        return "SimpleHttpThreadPool";
    }

}
