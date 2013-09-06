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

package com.flipkart.phantom.runtime.impl.server.netty.handler.http;

import org.jboss.netty.handler.codec.http.HttpRequest;

/**
 * <code>MethodRoutingHttpChannelHandler</code> is a sub-type of {@link RoutingHttpChannelHandler} that routes requests by request method i.e. GET, POST etc.
 *
 * @author Regunath B
 * @version 1.0, 6 Sep 2013
 */

public class MethodRoutingHttpChannelHandler extends RoutingHttpChannelHandler {

	/**
	 * Abstract method implementation. Returns the request method i.e. GET, POST etc.
	 * @see com.flipkart.phantom.runtime.impl.server.netty.handler.http.RoutingHttpChannelHandler#getRoutingKey(org.jboss.netty.handler.codec.http.HttpRequest)
	 */
	protected String getRoutingKey(HttpRequest request) {
		return request.getMethod().getName(); 
	}    

}
