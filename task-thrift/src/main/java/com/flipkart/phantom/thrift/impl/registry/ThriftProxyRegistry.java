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

package com.flipkart.phantom.thrift.impl.registry;

import com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry;
import com.flipkart.phantom.thrift.impl.ThriftProxy;

/**
 * <code>ThriftProxyRegistry</code>  maintains a registry of ThriftProxys. Provides lookup 
 * methods to get a ThriftProxy using a command string.
 * 
 * @author devashishshankar
 * @version 1.0, 3rd April, 2013
 */
public class ThriftProxyRegistry extends AbstractHandlerRegistry<ThriftProxy> {

	/**
	 * Abstract method implementation. Returns the type of {@link ThriftProxy}
	 * @see com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry#getHandlerType()
	 */
	protected Class<ThriftProxy> getHandlerType() {
		return ThriftProxy.class;
	}
	
}
