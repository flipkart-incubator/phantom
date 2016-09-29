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
import java.util.List;
import java.util.Map;

import com.flipkart.phantom.task.impl.collector.EventDispatchingSpanCollector;
import com.flipkart.phantom.task.spi.RequestWrapper;
import com.flipkart.phantom.task.spi.interceptor.RequestInterceptor;
import com.flipkart.phantom.task.spi.interceptor.SpanNameFilter;
import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.BraveHttpHeaders;
import com.github.kristofa.brave.ClientTracer;
import com.github.kristofa.brave.SpanId;
import com.github.kristofa.brave.TraceFilter;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

/**
 * <code>ClientRequestInterceptor</code> is an implementation of {@link RequestInterceptor} that traces client requests to services and task handlers.
 * This implementation is based on the Brave ClientRequestInterceptor code. 
 * 
 * @author Regunath B
 * @version 1.0, 13th Nov, 2014
 */
public class ClientRequestInterceptor<T extends RequestWrapper> implements RequestInterceptor<T> {

	/** The request annotation and value strings*/
    private static final String HOST_ANNOTATION = "host";
    private static final String REQUEST_ANNOTATION = "request";
    private static final String TRUE = "true";
    private static final String FALSE = "false";

    /** The optional Span name filter*/
    private Optional<SpanNameFilter> spanNameFilter = Optional.absent();

    /** The EventDispatchingSpanCollector instance used in tracing requests*/
    private EventDispatchingSpanCollector eventDispatchingSpanCollector;
    private List<TraceFilter> traceFilters;
    
	/**
	 * Interface method implementation. Performs client request tracing.
	 * @see com.flipkart.phantom.task.spi.interceptor.RequestInterceptor#process(com.flipkart.phantom.task.spi.RequestWrapper)
	 */
	public void process(T request) {
		// we let trace filter decide if client request tracing is needed. Handler level config takes precedence even if global trace is on.
		for (final TraceFilter traceFilter : traceFilters) {
			if (!traceFilter.trace(request.getRequestName())) { 
				return;
			}
		}
    	ClientTracer clientTracer = Brave.getClientTracer(this.eventDispatchingSpanCollector, this.traceFilters);
		String spanName = this.getSpanName(request);
		SpanId newSpanId = clientTracer.startNewSpan(spanName);
		this.addTracingHeaders(request, newSpanId, spanName);
		if (request.getRequestContext().isPresent() && request.getRequestContext().get().getCurrentClientEndpoint() != null) {
			// override the service name with the value contained in the request
			clientTracer.setCurrentClientServiceName(request.getRequestContext().get().getCurrentClientEndpoint().getServiceName());
			// submit an annotation so that host endpoint is visible in the span
			clientTracer.submitBinaryAnnotation(HOST_ANNOTATION, request.getRequestContext().get().getCurrentClientEndpoint().getHost() + 
					":" + request.getRequestContext().get().getCurrentClientEndpoint().getPort());
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
	protected String getSpanName(T request) {
		final Optional<String> spanNameFromRequest = this.getSpanNameFromRequest(request);
		if (spanNameFromRequest.isPresent()) {
			return spanNameFromRequest.get();
		}
		String spanName = request.getRequestName();
        if (this.spanNameFilter.isPresent()) {
            spanName = this.spanNameFilter.get().filterSpanName(spanName);
        }
        return spanName;
	}
	
	/**
	 * Gets a span name from the request. This implementation returns the span name from request headers, if it exists.
	 * @return an optional span name from the request
	 */
    protected Optional<String> getSpanNameFromRequest(T request) {
        Optional<String> spanName = Optional.absent();
        for (Map.Entry<String, String> entry : request.getHeaders().get()) {
        	if (entry.getKey().equalsIgnoreCase(BraveHttpHeaders.SpanName.getName())) {
        		spanName = Optional.of(entry.getValue());
        		break;
        	}
        }
        return spanName;
    }	
	
	/**
	 * Adds tracing headers to the request wrapper for the specified span.
	 * This implementation is based on the Brave ClientRequestHeaders class code
	 * @param request the request wrapper to add tracing headers to 
	 * @param spanId the span being executed
	 * @param spanName the name of the span
	 */
	protected void addTracingHeaders(T request, SpanId spanId, String spanName) {
		Map<String, String> headers = new HashMap<String,String>();
		// get all existing headers and then add the trace headers
		if (request.getHeaders().isPresent()) {
			List<Map.Entry<String, String>>existingHeaders = request.getHeaders().get();
			for(Map.Entry<String, String> e: existingHeaders) {
				headers.put(e.getKey(), e.getValue());
			}
		}
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
	public Optional<SpanNameFilter> getSpanNameFilter() {
		return spanNameFilter;
	}
	public void setSpanNameFilter(Optional<SpanNameFilter> spanNameFilter) {
		this.spanNameFilter = spanNameFilter;
	}
	public void setEventDispatchingSpanCollector(EventDispatchingSpanCollector eventDispatchingSpanCollector) {
		this.eventDispatchingSpanCollector = eventDispatchingSpanCollector;
	}
	public void setTraceFilters(List<TraceFilter> traceFilters) {
		this.traceFilters = traceFilters;
	}	

}
