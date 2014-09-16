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

package com.flipkart.phantom.runtime.spi.spring.admin;

import java.io.File;
import java.util.List;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.trpr.platform.core.PlatformException;

import com.flipkart.phantom.runtime.impl.server.AbstractNetworkServer;
import com.flipkart.phantom.task.spi.AbstractHandler;
import com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry;

/**
 * <code>SPConfigService</code> provides methods for viewing configurations
 *
 * @author devashishshankar
 * @version 1.0, 2nd May, 2013
 */
public interface SPConfigService<T extends AbstractHandler> {

    /**
     * Get all the Deployed {@link com.flipkart.phantom.runtime.impl.server.netty.TCPNettyServer} instances
     * @return List list of network servers
     */
    public List<AbstractNetworkServer> getDeployedNetworkServers();

    /**
     * Add an {@link AbstractNetworkServer} to the list of currently deployed network servers
     * @param server The {@link AbstractNetworkServer} instance
     */
    public void addDeployedNetworkServer(AbstractNetworkServer server);

    /**
     * Add an {@link AbstractHandlerRegistry} to the list of handler registries
     * @param registry The {@link AbstractHandlerRegistry} instance
     */
    public void addHandlerRegistry(AbstractHandlerRegistry<T> registry);

	/**
	 * Gets the Handler configuration file as a resource
	 * @param handlerName Name of the Handler
	 * @return Configuration file
	 */
	public Resource getHandlerConfig(String handlerName);
	
	/**
	 * Modifies the Handler config file for the given handler 
	 * @param modifiedHandlerConfigFile This will be set as the handler configuration file for all Handlers present in the File
	 * @throws PlatformException In case of inconsistencies
	 */
	public void modifyHandlerConfig(String handlerName, ByteArrayResource modifiedHandlerConfigFile) throws PlatformException;

	/**
	 * Method to inject TaskHandler file name 
	 */
	void addHandlerConfigPath(File taskHandlerFile, T handler);

    /**
     * Re-initializes a TaskHandler, if found. Calls the destroy() and init() methods.
     * @param taskHandler The name of the TaskHandler to be re-inited
     */
    public void reinitHandler(String taskHandler) throws Exception;

    /**
     * Get all handlers info
     */
    public List<AbstractHandler> getAllHandlers();

}
