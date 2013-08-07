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
import com.flipkart.phantom.task.spi.AbstractHandler;
import com.flipkart.phantom.task.spi.TaskContext;
import com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry;
import com.flipkart.phantom.task.spi.registry.HandlerConfigInfo;
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
    public void init(List<HandlerConfigInfo> handlerConfigInfoList, TaskContext taskContext) throws Exception {
        for (HandlerConfigInfo handlerConfigInfo : handlerConfigInfoList) {
            String[] proxyBeanIds = handlerConfigInfo.getProxyHandlerContext().getBeanNamesForType(HttpProxy.class);
            for (String proxyBeanId : proxyBeanIds) {
                HttpProxy proxy = (HttpProxy) handlerConfigInfo.getProxyHandlerContext().getBean(proxyBeanId);
                try {
                    LOGGER.info("Initializing HttpProxy: " + proxy.getName());
                    proxy.init(taskContext);
                    proxy.activate();
                } catch (Exception e) {
                    LOGGER.error("Error initializing HttpProxy {}. Error is: " + e.getMessage(), proxy.getName(), e);
                    throw new PlatformException("Error initializing HttpProxy: " + proxy.getName(), e);
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
        HttpProxy proxy = this.proxies.get(name);
        if (proxy != null) {
            try {
                proxy.deactivate();
                proxy.shutdown(taskContext);
                proxy.init(taskContext);
                proxy.activate();
            } catch (Exception e) {
                LOGGER.error("Error initializing HttpProxy {}. Error is: " + e.getMessage(), name, e);
                throw new PlatformException("Error reinitialising HttpProxy: " + name, e);
            }
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
                LOGGER.info("Shutting down HttpProxy: " + name);
                proxies.get(name).shutdown(taskContext);
                proxies.get(name).deactivate();
            } catch (Exception e) {
                LOGGER.warn("Failed to shutdown HttpProxy: " + name, e);
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

}
