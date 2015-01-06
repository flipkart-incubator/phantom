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

import com.flipkart.phantom.task.spi.TaskResult;
import com.flipkart.phantom.task.impl.interceptor.AbstractClientResponseInterceptor;
import com.google.common.base.Optional;

/**
 * <code>CommandClientResponseInterceptor</code> is a sub-type of {@link AbstractClientResponseInterceptor} for Command protocol.
 * 
 * @author Regunath B
 * @version 1.0, 18th Nov, 2014
 */

@SuppressWarnings("rawtypes")
public class CommandClientResponseInterceptor<S extends TaskResult> extends AbstractClientResponseInterceptor<S> {

	/**
	 * Abstract method implementation. Returns the {@link TaskResult#isSuccess()}
	 * @see com.flipkart.phantom.task.impl.interceptor.AbstractClientResponseInterceptor#isResponseSuccess(java.lang.Object)
	 */
	protected boolean isResponseSuccess(S response) {
		return response.isSuccess();
	}

	/**
	 * Abstract method implementation. Does not return any status code as the Command protocol does not support it
	 * @see com.flipkart.phantom.task.impl.interceptor.AbstractClientResponseInterceptor#getResponseStatusCode(java.lang.Object)
	 */
	protected Optional<Integer> getResponseStatusCode(S response) {
		return Optional.absent();
	}
	
}
