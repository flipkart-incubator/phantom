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

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 * <code>HttpConnectionPool</code> does the connection pool management for HTTP proxy requests
 *
 * @author kartikbu
 * @created 16/7/13 1:54 AM
 * @version 1.0
 */
public class HttpConnectionPool {

    /** logger */
    private static Logger logger = LoggerFactory.getLogger(HttpConnectionPool.class);

    /** The HTTP client */
    private HttpClient client;

    /** Host to connect to */
    private String host = "localhost";

    /** port to connect to */
    private Integer port = 80;

    /** are the urls secure? */
    private Boolean secure = false;

    /** connection timeout in milis */
    private int connectionTimeout = 1000;

    /** socket timeout in milis */
    private int operationTimeout = 1000;

    /** max number of connections allowed */
    private int maxConnections = 20;

    /** max size of request queue */
    private int requestQueueSize = 0;

    /** the semaphore to separate the process queue */
    private Semaphore processQueue;

    /** Headers which can be set as part of init */
    private Map<String, String> headers;

    /**
     * Initialize the connection pool
     */
    public void initConnectionPool() {

        // max concurrent requests = max connections + request queue size
        this.processQueue = new Semaphore(requestQueueSize + maxConnections);

        // create scheme
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme(this.secure ? "https" : "http", port, PlainSocketFactory.getSocketFactory()));

        // create connection manager
        PoolingClientConnectionManager cm = new PoolingClientConnectionManager(schemeRegistry);

        // Max pool size
        cm.setMaxTotal(maxConnections);

        // Increase default max connection per route to 20
        cm.setDefaultMaxPerRoute(maxConnections);

        // Increase max connections for host:port
        HttpHost httpHost = new HttpHost(host, port);
        cm.setMaxPerRoute(new HttpRoute(httpHost), maxConnections);

        // set timeouts
        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeout);
        HttpConnectionParams.setSoTimeout(httpParams, operationTimeout);

        // create client pool
        this.client = new DefaultHttpClient(cm, httpParams);
    }

    /**
     * Method to execute a request
     * @param request HttpRequestBase object
     * @return response HttpResponse object
     */
    public HttpResponse execute(HttpRequestBase request, List<Map.Entry<String,String>> headers) throws Exception {
        setRequestHeaders(request, headers);
        logger.debug("Sending request: "+request.getURI());
        if (processQueue.tryAcquire()) {
            HttpResponse response;
            try {
                response = client.execute(request);
            } catch (Exception e) {
                processQueue.release();
                throw e;
            }
            processQueue.release();
            return response;
        } else {
            throw new Exception("Process queue full!");
        }
    }

    /**
     * This method sets the headers to the http request base.
     *
     * @param request {@link HttpRequestBase} to add headers to.
     * @param headers the List of header tuples which are added to the request
     */
    private void setRequestHeaders(HttpRequestBase request, List<Map.Entry<String,String>> headers) {
        if(this.headers != null && this.headers.isEmpty()) {
            for(String headerKey : this.headers.keySet()) {
                request.addHeader(headerKey, this.headers.get(headerKey));
            }
        }
        if(headers != null && !headers.isEmpty()) {
            for(Map.Entry<String,String> headerMap : headers) {
                request.addHeader(headerMap.getKey(), headerMap.getValue());
            }
        }
    }

    public String constructUrl(String uri) {
        return "http" + (secure ? "s" : "") + "://" + host + ":" + port + uri;
    }

    /** shutdown the client connections */
    public void shutdown() {
        client.getConnectionManager().shutdown();
    }

    /** Getters / Setters */

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Boolean getSecure() {
        return secure;
    }

    public void setSecure(Boolean secure) {
        this.secure = secure;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getOperationTimeout() {
        return operationTimeout;
    }

    public void setOperationTimeout(int operationTimeout) {
        this.operationTimeout = operationTimeout;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public int getRequestQueueSize() {
        return requestQueueSize;
    }

    public void setRequestQueueSize(int requestQueueSize) {
        this.requestQueueSize = requestQueueSize;
    }

    public Map<String,String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String,String> headers) {
        this.headers = headers;
    }
    /** Getters / Setters */

}

