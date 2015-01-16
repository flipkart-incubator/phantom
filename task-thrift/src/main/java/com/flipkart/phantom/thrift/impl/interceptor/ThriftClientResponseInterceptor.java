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
package com.flipkart.phantom.thrift.impl.interceptor;

import org.apache.thrift.transport.TTransport;

import com.flipkart.phantom.task.impl.interceptor.AbstractClientResponseInterceptor;
import com.google.common.base.Optional;

/**
 * <code>ThriftClientResponseInterceptor</code> is a sub-type of {@link AbstractClientResponseInterceptor} for Thrift.
 * 
 * @author Regunath B
 * @version 1.0, 18th Nov, 2014
 */

public class ThriftClientResponseInterceptor <T extends TTransport> extends AbstractClientResponseInterceptor<T> {

	/**
	 * Abstract method implementation. Returns true always. Calls that timeout or that have other Thrift Transport level errors fail much before this call is made 
	 * @see com.flipkart.phantom.task.impl.interceptor.AbstractClientResponseInterceptor#isResponseSuccess(java.lang.Object)
	 */
	protected boolean isResponseSuccess(T response) {
		return true;
	}

	/**
	 * Abstract method implementation. Does not return any status code as the Thrift protocol does not support it
	 * @see com.flipkart.phantom.task.impl.interceptor.AbstractClientResponseInterceptor#getResponseStatusCode(java.lang.Object)
	 */
	protected Optional<Integer> getResponseStatusCode(T response) {
		return Optional.absent();
	}
}
