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

package com.flipkart.phantom.task.spi;

import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;

/**
 * <code>RequestWrapper</code> wraps request data. Specific sub-types contains data relevant to the protocol/transport  
 * @author : arya.ketan
 * @author Regunath B
 * @version : 2.0
 * @date : 13 Nov 2014
 */
public abstract class RequestWrapper {
	
	/** The RequestContext instance*/
	private Optional<RequestContext> requestContext = Optional.absent();
	
	/** The name of the target service for this request wrapper*/
	private Optional<String> serviceName;
	
	/**
	 * Returns a name that best describes the request being executed. For e.g. it could be the URI part of a Http request, the Thrift command being executed
	 * or the Command name in case of the Command protocol. 
	 * @return String name of the request
	 */
	public abstract String getRequestName();
	
	/**
	 * Returns an optional string describing request meta data
	 * @return an optional string containing request meta data.
	 */
	public abstract Optional<String> getRequestMetaData();

	/**
	 * Sets a set of headers for the request. Specific sub-types may honor propagating or ignoring these headers as supported by the underlying protocol
	 * @param headers request headers
	 */
	public abstract void setHeaders(List<Map.Entry<String, String>> headers);

	/** Setter/Getter methods */
	public Optional<RequestContext> getRequestContext() {
		return requestContext;
	}
	public void setRequestContext(Optional<RequestContext> requestContext) {
		this.requestContext = requestContext;
	}
	public Optional<String> getServiceName() {
		return serviceName;
	}
	public void setServiceName(Optional<String> serviceName) {
		this.serviceName = serviceName;
	}
	
}
