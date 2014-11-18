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

import org.springframework.util.Assert;

import com.flipkart.phantom.task.spi.interceptor.ResponseInterceptor;
import com.github.kristofa.brave.ClientTracer;
import com.google.common.base.Optional;

/**
 * <code>AbstractClientResponseInterceptor</code> is an implementation of {@link ResponseInterceptor} that traces responses from services and task handlers.
 * This implementation is based on the Brave ClientResponseInterceptor code. 
 * 
 * @author Regunath B
 * @version 1.0, 17th Nov, 2014
 */

public abstract class AbstractClientResponseInterceptor<S> implements ResponseInterceptor<S> {

	/** Response annotations and values */
    private static final String FAILURE_ANNOTATION = "failure";
    private static final String RESPONSE_CODE_ANNOTATION = "responsecode";
	
    /** The ClientTracer implementation used in client request tracing*/
    private ClientTracer clientTracer;
    
    /**
     * Interface method implementation. Checks if all mandatory properties have been set
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(this.clientTracer, "The 'clientTracer' may not be null");
    }
	
	/**
	 * Interface method implementation. Interprets the response and submits suitable annotation to the client tracer and also marks receipt of the response on it. 
	 * @see com.flipkart.phantom.task.spi.interceptor.ResponseInterceptor#process(java.lang.Object)
	 */
	public void process(S response, Optional<RuntimeException> transportError) {
		if (transportError.isPresent()) {
			clientTracer.submitAnnotation(FAILURE_ANNOTATION);
		} else {
	        final Optional<Integer> statusCode = this.getResponseStatusCode(response);
	        if (statusCode.isPresent()) {
	        	clientTracer.submitBinaryAnnotation(RESPONSE_CODE_ANNOTATION, statusCode.get());
	        }
	        if (!this.isResponseSuccess(response)) {
	            // In this case response will be the error message.
	            clientTracer.submitAnnotation(FAILURE_ANNOTATION);
	        }
		}
		this.clientTracer.setClientReceived();
	}
	
	/**
	 * Gets a boolean response success status
	 * @param response the response from the service or handler
	 * @return response success status - true if success, false otherwise
	 */
	protected abstract boolean isResponseSuccess(S response);
	
	/**
	 * Returns an optional response status code
	 * @param response the response from the service or handler
	 * @return optional code describing the response status
	 */
	protected abstract Optional<Integer> getResponseStatusCode(S response);
	
	/** Getter/Setter methods */
	public ClientTracer getClientTracer() {
		return clientTracer;
	}
	public void setClientTracer(ClientTracer clientTracer) {
		this.clientTracer = clientTracer;
	}
	
}
