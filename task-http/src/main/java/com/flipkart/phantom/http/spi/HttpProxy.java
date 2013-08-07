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

package com.flipkart.phantom.http.spi;

import com.flipkart.phantom.http.impl.HttpConnectionPool;
import com.flipkart.phantom.task.spi.TaskContext;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ByteArrayEntity;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract class for handling HTTP proxy requests
 *
 * @author kartikbu
 * @created 16/7/13 1:54 AM
 * @version 1.0
 */
public abstract class HttpProxy {

    /** The status showing the HttpProxy is initiated and ready to use */
    public static int ACTIVE = 1;

    /** The status showing the HttpProxy is not initiated/has been shutdown and should not be used */
    public static int INACTIVE = 0;

    /** The status of this HttpProxy (active/inactive) */
    private AtomicInteger status = new AtomicInteger(INACTIVE);

    /** The connection pool implementation instance */
    private HttpConnectionPool pool;

    /**
     *  Init hook provided by the HttpProxy
     */
    public void init() throws Exception {
        if (pool == null) {
            throw new AssertionError("HttpConnectionPool object 'pool' must be given");
        } else {
            pool.initConnectionPool();
        }
    }

    /**
     * Shutdown hooks provided by the HttpProxy
     */
    public void shutdown() throws Exception {
        pool.shutdown();
    }

    /**
     * Gives the status of the HttpProxy (active/inactive)
     * @return boolean true if active
     */
    public boolean isActive() {
        if (this.status.intValue() == ACTIVE) {
            return true;
        }
        return false;
    }

    /**
     * The main method which makes the HTTP request
     */
    public HttpResponse doRequest(String method, String uri, byte[] data) throws Exception {
        return pool.execute(createRequest(method,uri,data));
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
     * Name of the proxy
     * @return String name
     */
    public String getName() {
        return pool.getName();
    }

    /**
     * Socket timeout of the proxy
     * @return int socket timeout
     */
    public int getTimeout() {
        return pool.getOperationTimeout();
    }

    /**
     * Abstract fallback request method
     * @param request request which failed
     * @param taskContext current task context
     * @return HttpResponse response after executing the fallback
     */
    public abstract HttpResponse fallbackRequest(HttpRequest request, TaskContext taskContext);

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

    /** getters / setters */
    public void setStatus(int status) {
        this.status.set(status);
    }
    public HttpConnectionPool getPool() {
        return pool;
    }
    public void setPool(HttpConnectionPool pool) {
        this.pool = pool;
    }
    /** getters / setters */


}
