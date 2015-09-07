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

import org.apache.http.HttpResponse;

import com.flipkart.phantom.task.impl.interceptor.AbstractClientResponseInterceptor;
import com.google.common.base.Optional;

/**
 * <code>HttpClientResponseInterceptor</code> is a sub-type of {@link AbstractClientResponseInterceptor} for Http.
 * 
 * @author Regunath B
 * @version 1.0, 18th Nov, 2014
 */

public class HttpClientResponseInterceptor<T extends HttpResponse> extends AbstractClientResponseInterceptor<T> {

	/**
	 * Abstract method implementation. Returns true for Http status codes between 200 and 299
	 * @see com.flipkart.phantom.task.impl.interceptor.AbstractClientResponseInterceptor#isResponseSuccess(java.lang.Object)
	 */
	protected boolean isResponseSuccess(T response) {
		if (response == null || response.getStatusLine() == null) {
			return false;
		}		
		return (response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() <= 299);
	}

	/**
	 * Abstract method implementation. Returns the Http respone status code
	 * @see com.flipkart.phantom.task.impl.interceptor.AbstractClientResponseInterceptor#getResponseStatusCode(java.lang.Object)
	 */
	protected Optional<Integer> getResponseStatusCode(T response) {
		if (response == null || response.getStatusLine() == null) {
			return Optional.absent();
		}
		return Optional.of(response.getStatusLine().getStatusCode());
	}

}
