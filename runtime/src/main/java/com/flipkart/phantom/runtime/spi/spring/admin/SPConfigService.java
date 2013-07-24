/*
 * Copyright 2012-2015, Flipkart Internet Pvt Ltd. All rights reserved.
 * 
 * This software is the confidential and proprietary information of Flipkart Internet Pvt Ltd. ("Confidential Information").  
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of the license 
 * agreement you entered into with Flipkart.    
 * 
 */

package com.flipkart.phantom.runtime.spi.spring.admin;

import com.flipkart.phantom.runtime.impl.server.AbstractNetworkServer;
import com.flipkart.phantom.runtime.spi.server.NetworkServer;
import com.flipkart.phantom.task.spi.TaskHandler;
import com.flipkart.phantom.thrift.spi.ThriftProxy;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.trpr.platform.core.PlatformException;

import java.io.File;
import java.util.List;

/**
 * <code>SPConfigService</code> provides methods for viewing configurations related with {@link TaskHandler},
 * {@link ThriftProxy}, and {@link NetworkServer} deployed in the ServiceProxy. It also provides methods 
 * to update {@link TaskHandler} configurations.
 * 
 * @author devashishshankar
 * @version 1.0, 2nd May, 2013
 */
public interface SPConfigService {

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
	 * Get all the registered Task Handlers
	 * @return List of all {@link TaskHandler}
	 */
	public TaskHandler[] getAllTaskHandlers();

	/**
	 * Get all the registered Thrift Proxies
	 * @return List of all {@link ThriftProxy}
	 */
	public ThriftProxy[] getAllThriftProxies();
	
	/**
	 * Get all the Deployed {@link com.flipkart.phantom.runtime.impl.server.netty.TCPNettyServer} instances
	 */
	public List<AbstractNetworkServer> getDeployedNetworkServers();

	/**
	 * Method to inject TaskHandler file name 
	 */
	void addTaskHandlerConfigPath(File taskHandlerFile, TaskHandler taskHandler);

    /**
     * Re-initializes a TaskHandler, if found. Calls the destroy() and init() methods.
     * @param taskHandler The name of the TaskHandler to be re-inited
     */
    public void reinitTaskHandler(String taskHandler) throws Exception;

}
