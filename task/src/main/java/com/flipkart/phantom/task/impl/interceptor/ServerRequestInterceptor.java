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
package com.flipkart.phantom.task.impl.interceptor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.flipkart.phantom.task.spi.RequestWrapper;
import com.flipkart.phantom.task.spi.interceptor.RequestInterceptor;
import com.flipkart.phantom.task.spi.interceptor.ResponseInterceptor;
import com.flipkart.phantom.task.spi.tracing.TraceData;
import com.github.kristofa.brave.BraveHttpHeaders;
import com.github.kristofa.brave.EndPointSubmitter;
import com.github.kristofa.brave.ServerTracer;
import com.google.common.base.Optional;

/**
 * <code>ServerRequestInterceptor</code> is an implementation of {@link RequestInterceptor} that traces server requests.
 * This implementation is based on the Brave ServletTraceFilter code that may be used with different protocol servers/services, wrapped suitably where required.
 * 
 * @author Regunath B
 * @version 1.0, 25th Nov, 2014
 */

public class ServerRequestInterceptor<T extends RequestWrapper, S> implements RequestInterceptor<T>, ResponseInterceptor<S> {

	/** Response annotations and values */
    private static final String FAILURE_ANNOTATION = "failure";
    private static final String REQUEST_ANNOTATION = "request";

	/** The service identifying attributes*/
	private String serviceHost;
	private int servicePort;
	private String serviceName;
	
	/** The ServerTrace implementation used in server request tracing*/
    private ServerTracer serverTracer;
    
    /** The EndPointSubmitter used for submitting service endpoint details*/
    private EndPointSubmitter endPointSubmitter;
    
    /**
     * Interface method implementation. Sets up the ServerTrace current trace and submits the service enpoint details to EndPointSubmitter
     * @see com.flipkart.phantom.task.spi.interceptor.RequestInterceptor#process(com.flipkart.phantom.task.spi.RequestWrapper)
     */
	public void process(T request) {
		this.endPointSubmitter.submit(this.serviceHost, this.servicePort, this.serviceName);
		TraceData traceData = getTraceDataFromHeaders(request);
        if (Boolean.FALSE.equals(traceData.shouldBeTraced())) {
            this.serverTracer.setStateNoTracing();
        } else {
            String spanName = this.getSpanName(traceData, request);
            if (traceData.getTraceId() != null && traceData.getSpanId() != null) {
            	this.serverTracer.setStateCurrentTrace(traceData.getTraceId(), traceData.getSpanId(),
                        traceData.getParentSpanId(), spanName);
            } else {
            	this.serverTracer.setStateUnknown(spanName);
            }
        }
        final Optional<String> requestMetadata = request.getRequestMetaData();
        if (requestMetadata.isPresent()) {
        	this.serverTracer.submitBinaryAnnotation(REQUEST_ANNOTATION, requestMetadata.get());
        }        
		this.serverTracer.setServerReceived();
	}
	
	/**
	 * Interface method implementation. Interprets the response for transport errors and submits suitable annotation to the server tracer and also marks receipt 
	 * of the response on it.
	 * @see com.flipkart.phantom.task.spi.interceptor.ResponseInterceptor#process(java.lang.Object, com.google.common.base.Optional)
	 */
	public void process(S response, Optional<RuntimeException> transportError) {
		// we submit the endpoint again as it could have been overriden by the client request interceptors
		this.endPointSubmitter.submit(this.serviceHost, this.servicePort, this.serviceName);
		if (transportError.isPresent()) {
			this.serverTracer.submitAnnotation(FAILURE_ANNOTATION);
		} 
	    this.serverTracer.setServerSend();
	}	

	/**
	 * Gets trace data from the request
	 * @param request the request wrapper
	 * @return trace data, if it exists
	 */
    private TraceData getTraceDataFromHeaders(T request) {
        TraceData traceData = new TraceData();
        if (request.getHeaders().isPresent()) {
        	Map<String,String> headersMap = new HashMap<String,String>();
        	for (Iterator<Map.Entry<String, String>> headers=request.getHeaders().get().iterator(); headers.hasNext();) {
        		Map.Entry<String, String> entry = headers.next();
            	headersMap.put(entry.getKey(), entry.getValue());
        	}
	        traceData.setTraceId(longOrNull(headersMap.get(BraveHttpHeaders.TraceId.getName())));
	        traceData.setSpanId(longOrNull(headersMap.get(BraveHttpHeaders.SpanId.getName())));
	        traceData.setParentSpanId(longOrNull(headersMap.get(BraveHttpHeaders.ParentSpanId.getName())));
	        traceData.setShouldBeSampled(nullOrBoolean(headersMap.get(BraveHttpHeaders.Sampled.getName())));
	        traceData.setSpanName(headersMap.get(BraveHttpHeaders.SpanName.getName()));
        }
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
    
    /** Helper method to get the span name from trace data or the request */
    private String getSpanName(TraceData traceData, T request) {
        if (traceData.getSpanName() == null || traceData.getSpanName().isEmpty()) {
            return request.getRequestName();
        }
        return traceData.getSpanName();
    }
	
	/** Getter/Setter methods */
	public void setServerTracer(ServerTracer serverTracer) {
		this.serverTracer = serverTracer;
	}
	public void setEndPointSubmitter(EndPointSubmitter endPointSubmitter) {
		this.endPointSubmitter = endPointSubmitter;
	}
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	public void setServiceHost(String serviceHost) {
		this.serviceHost = serviceHost;
	}
	public void setServicePort(int servicePort) {
		this.servicePort = servicePort;
	}
	
}
