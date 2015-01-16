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

package com.flipkart.phantom.http.impl.registry;

import com.flipkart.phantom.http.impl.HttpProxy;
import com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry;

/**
 * Implementation of {@link AbstractHandlerRegistry} for HttpProxy instances
 *
 * @author kartikbu
 * @version 1.0
 * @created 30/7/13 1:57 AM
 */
public class HttpProxyRegistry extends AbstractHandlerRegistry<HttpProxy> {

	/**
	 * Abstract method implementation. Returns the type of {@link HttpProxy}
	 * @see com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry#getHandlerType()
	 */
	protected Class<HttpProxy> getHandlerType() {
		return HttpProxy.class;
	}
    
}
