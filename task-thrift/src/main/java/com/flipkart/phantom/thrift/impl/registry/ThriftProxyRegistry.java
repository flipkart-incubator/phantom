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

import java.util.ArrayList;
import java.util.List;

import com.flipkart.phantom.task.spi.TaskContext;
import com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry;
import com.flipkart.phantom.thrift.spi.ThriftProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>ThriftProxyRegistry</code>  maintains a registry of ThriftProxys. Provides lookup 
 * methods to get a ThriftProxy using a command string.
 * 
 * @author devashishshankar
 * @version 1.0, 3rd April, 2013
 */
public class ThriftProxyRegistry extends AbstractHandlerRegistry {

	/** Logger for this class*/
	private static final Logger LOGGER = LoggerFactory.getLogger(ThriftProxyRegistry.class);

    /** List of all thrift proxies */
    private List<ThriftProxy> proxies = new ArrayList<ThriftProxy>();
	
	/**
	 * Returns all the ThriftProxy instances present in the registry
	 * @return Array of ThriftProxy
	 */
	public ThriftProxy[] getAllThriftProxies() {
        return (ThriftProxy[]) proxies.toArray();
	}

    @Override
    public void init(TaskContext taskContext) throws Exception {
        for (ThriftProxy proxy : proxies) {
            try {
                proxy.init();
            } catch (Exception e) {
                LOGGER.error("Failed to initialize thrift proxy: " + proxy.getName());
                throw new Exception("Failed to initialize thrift proxy: " + proxy.getName(), e);
            }
        }
    }

    @Override
    public void shutdown(TaskContext taskContext) throws Exception {
        for (ThriftProxy proxy: proxies) {
            try {
                proxy.shutdown();
            } catch (Exception e) {
                LOGGER.warn("Failed to shutdown thrift proxy: " + proxy.getName(), e);
            }
            proxy.setStatus(ThriftProxy.INACTIVE);
        }
    }

    /** getters / setters */
    public List<ThriftProxy> getProxies() {
        return proxies;
    }
    public void setProxies(List<ThriftProxy> proxies) {
        this.proxies = proxies;
    }

}
