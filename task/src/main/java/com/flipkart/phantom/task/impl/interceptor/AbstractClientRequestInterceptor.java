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
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import com.flipkart.phantom.task.spi.RequestWrapper;
import com.flipkart.phantom.task.spi.interceptor.RequestInterceptor;
import com.flipkart.phantom.task.spi.interceptor.SpanNameFilter;
import com.github.kristofa.brave.BraveHttpHeaders;
import com.github.kristofa.brave.ClientTracer;
import com.github.kristofa.brave.SpanId;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

/**
 * <code>AbstractClientRequestInterceptor</code> is an implementation of {@link RequestInterceptor} that traces client requests to services and task handlers.
 * This implementation is based on the Brave ClientRequestInterceptor code. 
 * 
 * @author Regunath B
 * @version 1.0, 13th Nov, 2014
 */
public abstract class AbstractClientRequestInterceptor implements RequestInterceptor<RequestWrapper>, InitializingBean {

	/** The request annotation and value strings*/
    private static final String REQUEST_ANNOTATION = "request";
    private static final String TRUE = "true";
    private static final String FALSE = "false";

    /** The ClientTracer implementation used in client request tracing*/
    private ClientTracer clientTracer;
    
    /** The optional Span name filter*/
    private Optional<SpanNameFilter> spanNameFilter;

    /**
     * Interface method implementation. Checks if all mandatory properties have been set
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(this.clientTracer, "The 'clientTracer' may not be null");
    }
    
	/**
	 * Interface method implementation. Performs client request tracing.
	 * @see com.flipkart.phantom.task.spi.interceptor.RequestInterceptor#process(com.flipkart.phantom.task.spi.RequestWrapper)
	 */
	public void process(RequestWrapper request) {
		String spanName = this.getSpanName(request);
		SpanId newSpanId = clientTracer.startNewSpan(spanName);
		this.addTracingHeaders(request, newSpanId, spanName);
        final Optional<String> serviceName = request.getServiceName();
        if (serviceName.isPresent()) {
            clientTracer.setCurrentClientServiceName(serviceName.get());
        }
        final Optional<String> requestMetadata = request.getRequestMetaData();
        if (requestMetadata.isPresent()) {
        	clientTracer.submitBinaryAnnotation(REQUEST_ANNOTATION, requestMetadata.get());
        }
        clientTracer.setClientSent();
	}
	
	/**
	 * Gets the span name from the request wrapper
	 * @param request the request wrapper instance
	 * @return the span name
	 */
	protected abstract String getSpanName(RequestWrapper request);
	
	/**
	 * Adds tracing headers to the request wrapper for the specified span.
	 * This implementation is based on the Brave ClientRequestHeaders class code
	 * @param request the request wrapper to add tracing headers to 
	 * @param spanId the span being executed
	 * @param spanName the name of the span
	 */
	protected void addTracingHeaders(RequestWrapper request, SpanId spanId, String spanName) {
		Map<String, String> headers = new HashMap<String, String>();
        if (spanId != null) {
        	headers.put(BraveHttpHeaders.Sampled.getName(), TRUE);
        	headers.put(BraveHttpHeaders.TraceId.getName(), Long.toString(spanId.getTraceId(), 16));
        	headers.put(BraveHttpHeaders.SpanId.getName(), Long.toString(spanId.getSpanId(), 16));
            if (spanId.getParentSpanId() != null) {
            	headers.put(BraveHttpHeaders.ParentSpanId.getName(),Long.toString(spanId.getParentSpanId(), 16));
            }
            if (spanName != null) {
            	headers.put(BraveHttpHeaders.SpanName.getName(), spanName);
            }
        } else {
        	headers.put(BraveHttpHeaders.Sampled.getName(), FALSE);
        }
        request.setHeaders(Lists.newArrayList(headers.entrySet()));
	}

	/** Getter/Setter methods */
	public ClientTracer getClientTracer() {
		return clientTracer;
	}
	public void setClientTracer(ClientTracer clientTracer) {
		this.clientTracer = clientTracer;
	}
	public Optional<SpanNameFilter> getSpanNameFilter() {
		return spanNameFilter;
	}
	public void setSpanNameFilter(Optional<SpanNameFilter> spanNameFilter) {
		this.spanNameFilter = spanNameFilter;
	}	

}
