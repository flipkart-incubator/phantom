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
package com.flipkart.phantom.http.impl.interceptor;

import java.util.Map;

import com.flipkart.phantom.http.impl.HttpRequestWrapper;
import com.flipkart.phantom.task.impl.interceptor.ClientRequestInterceptor;
import com.flipkart.phantom.task.spi.RequestWrapper;
import com.github.kristofa.brave.BraveHttpHeaders;
import com.google.common.base.Optional;

/**
 * <code>HttpClientRequestInterceptor</code> is a sub-type of {@link ClientRequestInterceptor} that overrides only the 
 * getSpanNameFromRequest() method to retrieve the span name from Http request headers, if it exists.
 * 
 * @author Regunath B
 * @version 1.0, 21st Nov, 2014
 */
public class HttpClientRequestInterceptor<S extends HttpRequestWrapper> extends ClientRequestInterceptor<S> {

	/**
	 * Overriden method. Returns a span name from Http headers, if it exists
	 * @see com.flipkart.phantom.task.impl.interceptor.ClientRequestInterceptor#getSpanNameFromRequest(RequestWrapper)
	 */
    protected Optional<String> getSpanNameFromRequest(S request) {
        Optional<String> spanName = super.getSpanNameFromRequest(request);
        for (Map.Entry<String, String> entry : request.getHeaders().get()) {
        	if (entry.getKey().equalsIgnoreCase(BraveHttpHeaders.SpanName.getName())) {
        		spanName = Optional.of(entry.getValue());
        		break;
        	}
        }
        return spanName;
    }	
	
}
