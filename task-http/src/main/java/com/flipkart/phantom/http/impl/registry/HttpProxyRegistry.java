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

import com.flipkart.phantom.http.spi.HttpProxy;
import com.flipkart.phantom.task.spi.TaskContext;
import com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry;
import com.flipkart.phantom.task.spi.registry.ProxyHandlerConfigInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trpr.platform.core.PlatformException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of AbstractHandlerRegistry for HttpProxy instances
 *
 * @author kartikbu
 * @version 1.0
 * @created 30/7/13 1:57 AM
 */
public class HttpProxyRegistry extends AbstractHandlerRegistry {

    /** logger */
    private static Logger LOGGER = LoggerFactory.getLogger(HttpProxyRegistry.class);

    /** list of proxies by name */
    private Map<String,HttpProxy> proxies = new HashMap<String,HttpProxy>();

    /**
     * Abstract method implementation
     * @see com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry#init(java.util.List, com.flipkart.phantom.task.spi.TaskContext)
     */
    @Override
    public void init(List<ProxyHandlerConfigInfo> proxyHandlerConfigInfoList, TaskContext taskContext) throws Exception {
        for (ProxyHandlerConfigInfo proxyHandlerConfigInfo : proxyHandlerConfigInfoList) {
            String[] proxyBeanIds = proxyHandlerConfigInfo.getProxyHandlerContext().getBeanNamesForType(HttpProxy.class);
            for (String proxyBeanId : proxyBeanIds) {
                HttpProxy proxy = (HttpProxy) proxyHandlerConfigInfo.getProxyHandlerContext().getBean(proxyBeanId);
                try {
                    proxy.init();
                    proxy.setStatus(HttpProxy.ACTIVE);
                } catch (Exception e) {
                    LOGGER.error("Error initing HttpProxy{} . Error is : " + e.getMessage(),proxy.getName(), e);
                    throw new PlatformException("Error initing HttpProxy: " + proxy.getName(), e);
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
        HttpProxy proxy = this.proxies.get(handlerName);
        try {
            proxy.setStatus(HttpProxy.INACTIVE);
            this.proxies.get(handlerName).init();
            proxy.setStatus(HttpProxy.ACTIVE);
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
        for (String name : proxies.keySet()) {
            try {
                proxies.get(name).shutdown();
            } catch (Exception e) {
                LOGGER.warn("Failed to shutdown thrift proxy: " + name, e);
            }
        }
    }

    /**
     * Abstract method implementation
     * @see com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry#getHandlers()
     */
    @Override
    public Map<String, String> getHandlers() {
        Map<String,String> handlers = new HashMap<String, String>();
        for (String name : proxies.keySet()) {
            String desc = "";
            desc += "Host: " + proxies.get(name).getPool().getHost();
            desc += " Port: " + proxies.get(name).getPool().getPort();
            desc += " Operation Timeout: " + proxies.get(name).getPool().getOperationTimeout() + "ms";
            handlers.put(name,desc);
        }
        return handlers;
    }

    /**
     * Gives a HttpProxy given name. Used by ExecutorRepository to instantiate HttpProxyExecutor
     * @param name String Name of the proxy
     * @return HttpProxy instance of proxy handler class
     */
    public HttpProxy getProxy(String name) {
        return proxies.get(name);
    }

}
