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
package com.flipkart.phantom.runtime.impl.jetty.filter;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.web.filter.DelegatingFilterProxy;

import com.flipkart.phantom.task.impl.collector.EventDispatchingSpanCollector;
import com.flipkart.phantom.task.spi.tracing.TraceData;
import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.BraveHttpHeaders;
import com.github.kristofa.brave.EndPointSubmitter;
import com.github.kristofa.brave.FixedSampleRateTraceFilter;
import com.github.kristofa.brave.ServerTracer;
import com.github.kristofa.brave.TraceFilter;

/**
 * <code>ServletTraceFilter</code> is an implementation of {@link Filter} that traces servlet requests.
 * This implementation is based on the Brave ServletTraceFilter code and may be used with any servlet container that supports dependency injection via setter methods - for example,
 * this filter may be used along with the Spring {@link DelegatingFilterProxy}
 * 
 * @author Regunath B
 * @version 1.0, 12th Dec, 2014
 */

public class ServletTraceFilter implements Filter, InitializingBean  {

    /** Logger for this class*/
    private static final Logger LOGGER = LoggerFactory.getLogger(ServletTraceFilter.class);
	
    /** The default value for tracing frequency. This value indicates that tracing if OFF*/
    private static final TraceFilter NO_TRACING = new FixedSampleRateTraceFilter(-1);    
	
    /** The request tracing frequency for this channel handler*/
    private TraceFilter traceFilter = NO_TRACING;	
    
    /** The EventDispatchingSpanCollector instance used in tracing requests*/
    private EventDispatchingSpanCollector eventDispatchingSpanCollector;    
	
	/** The ServerTrace implementation used in server request tracing*/
    private ServerTracer serverTracer;
    
    /** The EndPointSubmitter used for submitting service endpoint details*/
    private EndPointSubmitter endPointSubmitter;
    
    /** The context path override, if any*/
    private String appContextPath;
    
    /**
     * Interface method implementation. Checks if all mandatory properties have been set
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(this.eventDispatchingSpanCollector, "The 'eventDispatchingSpanCollector' may not be null");  
        this.serverTracer = Brave.getServerTracer(this.eventDispatchingSpanCollector,  Arrays.<TraceFilter>asList(this.traceFilter));
        this.endPointSubmitter = Brave.getEndPointSubmitter();
    }
    
    /**
     * Interface method implementation. Does nothing
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
	public void init(FilterConfig filterConfig) throws ServletException {
		// no op
	}
	/**
	 * Interface method implementation. Does nothing
	 * @see javax.servlet.Filter#destroy()
	 */
	public void destroy() {
		// no op
	}

	/**
	 * Interface method implementation. Initializes server tracing i.e. extract trace headers from the request and send
	 * sr (server received) and ss (server sent) annotations.
	 * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
	 */
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        this.serverTracer.clearCurrentSpan();
        if (request instanceof HttpServletRequest) {
            submitEndpoint((HttpServletRequest) request);
            TraceData traceData = getTraceDataFromHeaders(request);
            if (Boolean.FALSE.equals(traceData.shouldBeTraced())) {
            	this.serverTracer.setStateNoTracing();
            	LOGGER.debug("Not tracing request");
            } else {
                String spanName = getSpanName(traceData, (HttpServletRequest) request);
                if (traceData.getTraceId() != null && traceData.getSpanId() != null) {
                	LOGGER.debug("Received span information as part of request");
                    this.serverTracer.setStateCurrentTrace(traceData.getTraceId(), traceData.getSpanId(),
                            traceData.getParentSpanId(), spanName);
                } else {
                	LOGGER.debug("Received no span state");
                    this.serverTracer.setStateUnknown(spanName);
                }
            }
        }
        this.serverTracer.setServerReceived();
        filterChain.doFilter(request, response);
        this.serverTracer.setServerSend();		
	}

	/**
	 * Submits the service request endpoint data
	 * @param request the service request
	 */
    private void submitEndpoint(HttpServletRequest request) {
        if (!endPointSubmitter.endPointSubmitted()) {
        	// prefer the app specified override, else use context path from Http request
            String contextPath = this.getAppContextPath() != null ? this.getAppContextPath() : request.getContextPath(); 
            String localAddr = request.getLocalAddr();
            int localPort = request.getLocalPort();
            endPointSubmitter.submit(localAddr, localPort, contextPath);
            LOGGER.debug("Setting endpoint: addr: {}, port: {}, contextpath: " + contextPath, localAddr, localPort);
        }
    }

    /** Helper method to get the span name from trace data or the request */
    private String getSpanName(TraceData traceData, HttpServletRequest request) {
        if (traceData.getSpanName() == null || traceData.getSpanName().isEmpty()) {
            return request.getRequestURI();
        }
        return traceData.getSpanName();
    }

	/**
	 * Gets trace data from the request
	 * @param request the request wrapper
	 * @return trace data, if it exists
	 */
    private TraceData getTraceDataFromHeaders(ServletRequest request) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        TraceData traceData = new TraceData();
        traceData.setTraceId(longOrNull(httpRequest.getHeader(BraveHttpHeaders.TraceId.getName())));
        traceData.setSpanId(longOrNull(httpRequest.getHeader(BraveHttpHeaders.SpanId.getName())));
        traceData.setParentSpanId(longOrNull(httpRequest.getHeader(BraveHttpHeaders.ParentSpanId.getName())));
        traceData.setShouldBeSampled(nullOrBoolean(httpRequest.getHeader(BraveHttpHeaders.Sampled.getName())));
        traceData.setSpanName(httpRequest.getHeader(BraveHttpHeaders.SpanName.getName()));
        return traceData;
    }

    /** Helper methods to get null or valid interpreted values*/
    private Boolean nullOrBoolean(String value) {
        return (value == null) ? null : Boolean.valueOf(value);
    }
    private Long longOrNull(String value) {
        if (value == null) {
            return null;
        }
        return Long.parseLong(value, 16);
    }
	
    /** Start Getter/Setter methods */
	public void setTraceFilter(TraceFilter traceFilter) {
		this.traceFilter = traceFilter;
	}
	public void setEventDispatchingSpanCollector(EventDispatchingSpanCollector eventDispatchingSpanCollector) {
		this.eventDispatchingSpanCollector = eventDispatchingSpanCollector;
	}
	public String getAppContextPath() {
		return appContextPath;
	}
	public void setAppContextPath(String appContextPath) {
		this.appContextPath = appContextPath;
	}
    /** End Getter/Setter methods */
	

}
