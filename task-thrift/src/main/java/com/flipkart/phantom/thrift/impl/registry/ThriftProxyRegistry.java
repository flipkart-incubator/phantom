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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.trpr.platform.core.PlatformException;
import org.trpr.platform.core.impl.logging.LogFactory;
import org.trpr.platform.core.spi.logging.Logger;

import com.flipkart.phantom.task.spi.AbstractHandler;
import com.flipkart.phantom.task.spi.TaskContext;
import com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry;
import com.flipkart.phantom.task.spi.registry.HandlerConfigInfo;
import com.flipkart.phantom.thrift.impl.ThriftProxy;

/**
 * <code>ThriftProxyRegistry</code>  maintains a registry of ThriftProxys. Provides lookup 
 * methods to get a ThriftProxy using a command string.
 * 
 * @author devashishshankar
 * @version 1.0, 3rd April, 2013
 */
public class ThriftProxyRegistry extends AbstractHandlerRegistry {

	/** Logger for this class*/
	private static final Logger LOGGER = LogFactory.getLogger(ThriftProxyRegistry.class);

    /** Map of all thrift proxies against name */
    private Map<String,ThriftProxy> proxies = new HashMap<String,ThriftProxy>();

    /**
     * Abstract method implementation
     * @see com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry#init(java.util.List, com.flipkart.phantom.task.spi.TaskContext)
     */
    @Override
    public void init(List<HandlerConfigInfo> handlerConfigInfoList, TaskContext taskContext) throws Exception {
        for (HandlerConfigInfo handlerConfigInfo : handlerConfigInfoList) {
            String[] proxyBeanIds = handlerConfigInfo.getProxyHandlerContext().getBeanNamesForType(ThriftProxy.class);
            for (String proxyBeanId : proxyBeanIds) {
                ThriftProxy proxy = (ThriftProxy) handlerConfigInfo.getProxyHandlerContext().getBean(proxyBeanId);
                try {
                    LOGGER.info("Initializing ThriftProxy: " + proxy.getName());
                    proxy.init(taskContext);
                    proxy.activate();
                } catch (Exception e) {
                    LOGGER.error("Error initializing ThriftProxy {}. Error is: " + e.getMessage(), proxy.getName(), e);
                    throw new PlatformException("Error initializing ThriftProxy: " + proxy.getName(), e);
                }
                this.proxies.put(proxy.getName(),proxy);
            }
        }
    }

    /**
     * Abstract method implementation
     * @see com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry#reinitHandler(String, com.flipkart.phantom.task.spi.TaskContext)
     */
    @Override
    public void reinitHandler(String name, TaskContext taskContext) throws Exception {
        ThriftProxy proxy = this.proxies.get(name);
        if (proxy != null) {
            try {
                proxy.deactivate();
                proxy.shutdown(taskContext);
                proxy.init(taskContext);
                proxy.activate();
            } catch (Exception e) {
                LOGGER.error("Error initializing ThriftProxy {}. Error is: " + e.getMessage(), name, e);
                throw new PlatformException("Error reinitializing ThriftProxy: " + name, e);
            }
        }
    }

    /**
     * Abstract method implementation
     * @see com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry#shutdown(com.flipkart.phantom.task.spi.TaskContext)
     */
    @Override
    public void shutdown(TaskContext taskContext) throws Exception {
        for (String name: proxies.keySet()) {
            try {
                LOGGER.info("Shutting down ThriftProxy: " + name);
                proxies.get(name).shutdown(taskContext);
                proxies.get(name).deactivate();
            } catch (Exception e) {
                LOGGER.warn("Failed to shutdown ThriftProxy: " + name, e);
            }
        }
    }

    /**
     * Abstract method implementation
     * @see com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry#getHandlers()
     */
    @Override
    public List<AbstractHandler> getHandlers() {
        return new ArrayList<AbstractHandler>(proxies.values());
    }

    /**
     * Abstract method implementation
     * @see com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry#getHandler(String)
     */
    @Override
    public AbstractHandler getHandler(String name) {
        return proxies.get(name);
    }

    /** getters / setters */
    public Map<String,ThriftProxy> getProxies() {
        return proxies;
    }
    public void setProxies(Map<String,ThriftProxy> proxies) {
        this.proxies = proxies;
    }
    /** end getters / setters */

}
