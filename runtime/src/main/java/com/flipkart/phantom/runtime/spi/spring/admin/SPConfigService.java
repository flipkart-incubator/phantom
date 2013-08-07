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

import com.flipkart.phantom.runtime.impl.server.AbstractNetworkServer;
import com.flipkart.phantom.task.spi.TaskHandler;
import com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.trpr.platform.core.PlatformException;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * <code>SPConfigService</code> provides methods for viewing configurations
 *
 * @author devashishshankar
 * @version 1.0, 2nd May, 2013
 */
public interface SPConfigService {

    /**
     * Get all the Deployed {@link com.flipkart.phantom.runtime.impl.server.netty.TCPNettyServer} instances
     */
    public List<AbstractNetworkServer> getDeployedNetworkServers();

    public void addDeployedNetworkServer(AbstractNetworkServer server);

    public void addHandlerRegistry(AbstractHandlerRegistry registry);

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
	//public void modifyHandlerConfig(String handlerName, ByteArrayResource modifiedHandlerConfigFile) throws PlatformException;

	/**
	 * Method to inject TaskHandler file name 
	 */
	void addTaskHandlerConfigPath(File taskHandlerFile, TaskHandler taskHandler);

    /**
     * Re-initializes a TaskHandler, if found. Calls the destroy() and init() methods.
     * @param taskHandler The name of the TaskHandler to be re-inited
     */
    public void reinitTaskHandler(String taskHandler) throws Exception;

    /**
     * Get all handlers info
     */
    public Map<String,String> getAllHandlers();

}
