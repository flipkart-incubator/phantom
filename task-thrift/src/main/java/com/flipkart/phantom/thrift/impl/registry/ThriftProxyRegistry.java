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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.flipkart.phantom.task.spi.TaskContext;
import com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry;
import com.flipkart.phantom.task.spi.registry.ProxyHandlerConfigInfo;
import com.flipkart.phantom.thrift.spi.ThriftProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trpr.platform.core.PlatformException;

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

    /** Map of all thrift proxies against name */
    private Map<String,ThriftProxy> proxies = new HashMap<String,ThriftProxy>();

    /**
     * Abstract method implementation
     * @see com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry#init(java.util.List, com.flipkart.phantom.task.spi.TaskContext)
     */
    @Override
    public void init(List<ProxyHandlerConfigInfo> proxyHandlerConfigInfoList, TaskContext taskContext) throws Exception {
        for (ProxyHandlerConfigInfo proxyHandlerConfigInfo : proxyHandlerConfigInfoList) {
            String[] proxyBeanIds = proxyHandlerConfigInfo.getProxyHandlerContext().getBeanNamesForType(ThriftProxy.class);
            for (String proxyBeanId : proxyBeanIds) {
                ThriftProxy proxy = (ThriftProxy) proxyHandlerConfigInfo.getProxyHandlerContext().getBean(proxyBeanId);
                try {
                    proxy.init();
                    proxy.setStatus(ThriftProxy.ACTIVE);
                } catch (Exception e) {
                    // TODO see if there are ThriftProxyS that can fail init but still permit others to load and the proxy can become active
                    LOGGER.error("Error initing ThriftProxy {} . Error is : " + e.getMessage(),proxy.getName(), e);
                    throw new PlatformException("Error initing ThriftProxy : " + proxy.getName(), e);
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
    public void reinitHandler(String handlerName, TaskContext taskContext) throws Exception {
        ThriftProxy proxy = this.proxies.get(handlerName);
        try {
            proxy.setStatus(ThriftProxy.INACTIVE);
            this.proxies.get(handlerName).init();
            proxy.setStatus(ThriftProxy.ACTIVE);
        } catch (Exception e) {
            LOGGER.error("Error initing HttpProxy {} . Error is : " + e.getMessage(),proxy.getName(), e);
            throw new PlatformException("Error reinitialising HttpProxy: " + proxy.getName(), e);
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
                proxies.get(name).shutdown();
            } catch (Exception e) {
                LOGGER.warn("Failed to shutdown thrift proxy: " + name, e);
            }
            proxies.get(name).setStatus(ThriftProxy.INACTIVE);
        }
    }

    /**
     * Abstract method implementation
     * @see com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry#getHandlers()
     */
    @Override
    public Map<String, String> getHandlers() {
        Map<String,String> handlers = new HashMap<String, String>();
        for (String name: proxies.keySet()) {
            String desc = "";
            desc += "Host: " + proxies.get(name).getThriftServer();
            desc += " Port: " + proxies.get(name).getThriftPort();
            desc += " Timeout: " + proxies.get(name).getThriftTimeoutMillis() + "ms";
            handlers.put(proxies.get(name).getName(),desc);
        }
        return handlers;
    }

    /**
     * Gets registered instance of ThriftProxy given name
     * @param name String name of the proxy
     * @return ThriftProxy instance registered
     */
    public ThriftProxy getProxy(String name) {
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
