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

import com.flipkart.phantom.task.spi.AbstractHandler;
import com.flipkart.phantom.task.spi.TaskContext;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ByteArrayEntity;

/**
 * Abstract class for handling HTTP proxy requests
 *
 * @author kartikbu
 * @created 16/7/13 1:54 AM
 * @version 1.0
 */
public abstract class HttpProxy extends AbstractHandler {

    /** The default thread pool size*/
    public static final int DEFAULT_THREAD_POOL_SIZE = 10;
    
    /** Name of the proxy */
    private String name;

    /** The connection pool implementation instance */
    private HttpConnectionPool pool;

    /** The thread pool size for this proxy*/
    private int threadPoolSize = HttpProxy.DEFAULT_THREAD_POOL_SIZE;
    
    /**
     *  Init hook provided by the HttpProxy
     */
    public void init(TaskContext context) throws Exception {
        if (pool == null) {
            throw new AssertionError("HttpConnectionPool object 'pool' must be given");
        } else {
            pool.initConnectionPool();
        }
    }

    /**
     * Shutdown hooks provided by the HttpProxy
     */
    public void shutdown(TaskContext context) throws Exception {
        pool.shutdown();
    }

    /**
     * The main method which makes the HTTP request
     */
    public HttpResponse doRequest(HttpRequestWrapper httpRequestWrapper) throws Exception {
        /** get necessary data required for the output */
        return pool.execute(createRequest(httpRequestWrapper.getMethod(),httpRequestWrapper.getUri(),
                httpRequestWrapper.getData()), httpRequestWrapper.getHeaders());
    }

    /**
     * Creates a HttpRequestBase object understood by the apache http library
     * @param method HTTP request method
     * @param uri HTTP request URI
     * @param data HTTP request data
     * @return
     * @throws Exception
     */
    private HttpRequestBase createRequest(String method, String uri, byte[] data) throws Exception {

        // get
        if ("GET".equals(method)) {
            HttpGet r = new HttpGet(pool.constructUrl(uri));
            return r;

            // put
        } else if ("PUT".equals(method)) {
            HttpPut r = new HttpPut(pool.constructUrl(uri));
            r.setEntity(new ByteArrayEntity(data));
            return r;

            // post
        } else if ("POST".equals(method)) {
            HttpPost r = new HttpPost(pool.constructUrl(uri));
            r.setEntity(new ByteArrayEntity(data));
            return r;

            // delete
        } else if ("DELETE".equals(method)) {
            HttpDelete r = new HttpDelete(pool.constructUrl(uri));
            return r;

            // invalid
        } else {
            return null;
        }
    }

    /**
     * Abstract fallback request method
     * @param httpRequestWrapper the http Request Wrapper object
     * @return HttpResponse response after executing the fallback
     */
    public abstract HttpResponse fallbackRequest(HttpRequestWrapper httpRequestWrapper);

    /**
     * Abstract method which gives group key
     * @return String group key
     */
    public abstract String getGroupKey();

    /**
     * Abstract method which gives command name
     * @return String command name
     */
    public abstract String getCommandKey();

    /**
     * Abstract method which gives the thread pool name
     * @return String thread pool name
     */
    public abstract String getThreadPoolKey();

    /**
     * Returns the thread pool size
     * @return thread pool size
     */
    public int getThreadPoolSize() {
        return this.threadPoolSize;
    }

    /**
     * Abstract method implementation
     * @see com.flipkart.phantom.task.spi.AbstractHandler#getDetails()
     */
    public String getDetails() {
        if (pool != null) {
            String details = "Endpoint: ";
            details += (pool.getSecure() ? "https://" : "http://") + pool.getHost() + ":" + pool.getPort() + "\n";
            details += "Connection Timeout: " + pool.getConnectionTimeout() + "ms\n";
            details += "Operation Timeout: " + pool.getOperationTimeout() + "ms\n";
            details += "Max Connections: " + pool.getMaxConnections() + "\n";
            details += "Request Queue Size: " + pool.getRequestQueueSize() + "\n";
            return details;
        }
        return "No endpoint configured";
    }

    /**
     * Abstract method implementation
     * @see AbstractHandler#getType()
     */
    @Override
    public String getType() {
        return "HttpProxy";
    }


    /** getters / setters */
    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return this.name;
    }
    public HttpConnectionPool getPool() {
        return pool;
    }
    public void setPool(HttpConnectionPool pool) {
        this.pool = pool;
    }
    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }
    /** getters / setters */



}
