/*
 * Copyright 2012-2015, Flipkart Internet Pvt Ltd. All rights reserved.
 * 
 * This software is the confidential and proprietary information of Flipkart Internet Pvt Ltd. ("Confidential Information").  
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of the license 
 * agreement you entered into with Flipkart.    
 * 
 */
package com.flipkart.phantom.runtime.impl.spring.admin;

import com.flipkart.phantom.runtime.impl.server.AbstractNetworkServer;
import com.flipkart.phantom.runtime.impl.spring.ServiceProxyComponentContainer;
import com.flipkart.phantom.runtime.spi.spring.admin.SPConfigService;
import com.flipkart.phantom.task.impl.TaskContextFactory;
import com.flipkart.phantom.task.impl.registry.TaskHandlerRegistry;
import com.flipkart.phantom.task.spi.TaskContext;
import com.flipkart.phantom.task.spi.TaskHandler;
import com.flipkart.phantom.thrift.impl.registry.ThriftProxyRegistry;
import com.flipkart.phantom.thrift.spi.ThriftProxy;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.trpr.platform.core.PlatformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * <code>SPConfigServiceImpl</code> is an implementation of {@link SPConfigService}.
 * 
 * @author devashishshankar
 * @version 1.0, 2nd May, 2013
 */
public class SPConfigServiceImpl  implements SPConfigService{

	/** Logger instance for this class*/
	private static final Logger LOGGER = LoggerFactory.getLogger(SPConfigServiceImpl.class);
	
	/** The previous handler file (save file) */
	public static final String PREV_HANDLER_FILE = "/spring-proxy-handler-prev.xml";
	
	/** The taskHandler registry instance containing the Registered TaskHandlers */
	private TaskHandlerRegistry taskHandlerRegistry;

    /** The registry holding the list of Thrift Proxy Handlers */
    private ThriftProxyRegistry thriftProxyRegistry;

	/** The componentCOntainer instance for reloading the config file */
	private ServiceProxyComponentContainer componentContainer;
	
	/** The map holding the mapping of a config file to it's handler name */
	private Map<URI, List<TaskHandler> > configURItoHandlerName= new HashMap<URI,List<TaskHandler>>();
	
	/** The list of deployed TCP Servers */
	private List<AbstractNetworkServer> deployedNetworkServers = new LinkedList<AbstractNetworkServer>();

    /**
     * @param taskHandler The name of the TaskHandler to be re-inited
     * @throws Exception in case of errors
     */
    public void reinitTaskHandler(String taskHandler) throws Exception {
        TaskHandler toInit = this.taskHandlerRegistry.getTaskHandlerByName(taskHandler);
        if(toInit==null) {
            throw new UnsupportedOperationException("Handler to be inited not found: "+taskHandler);
        }
        this.taskHandlerRegistry.unregisterTaskHandler(toInit);
        //Get the current taskContext
        TaskContext taskContext = TaskContextFactory.getTaskContext();
        toInit.shutdown(taskContext);
        toInit.init(taskContext);
        this.taskHandlerRegistry.registerTaskHandler(toInit);
    }
	/**
	 * Interface method implementation.
	 * @see com.flipkart.phantom.runtime.spi.spring.admin.SPConfigService#getHandlerConfig(java.lang.String)
	 */
	public Resource getHandlerConfig(String handlerName) {
		for(URI configFile  : this.configURItoHandlerName.keySet()) {
			for(TaskHandler taskHandler : this.configURItoHandlerName.get(configFile)) {
				if(taskHandler.getName().equals(handlerName)) {
					return new FileSystemResource(new File(configFile));
				}
			}
		}
		return null;
	}
	
	/**
	 * Interface method implementation.
	 * @see com.flipkart.phantom.runtime.spi.spring.admin.SPConfigService#modifyHandlerConfig(java.lang.String, org.springframework.core.io.ByteArrayResource)
	 */
	public void modifyHandlerConfig(String handlerName, ByteArrayResource modifiedHandlerConfigFile) {
		//Check if Handler file can be read
		File oldHandlerFile = null;
		try {
			oldHandlerFile = this.getHandlerConfig(handlerName).getFile();
		} catch (IOException e1) {
			LOGGER.error("Handler Config File for handler: "+handlerName+" not found. Returning");
			throw new PlatformException("File not found for handler: "+handlerName,e1);
		}
		if(!oldHandlerFile.exists()) {
			LOGGER.error("Handler Config File: "+oldHandlerFile.getAbsolutePath()+" doesn't exist. Returning");
			throw new PlatformException("File not found: "+oldHandlerFile.getAbsolutePath());
		}
		if(!oldHandlerFile.canRead()) {
			LOGGER.error("No read permission for: "+oldHandlerFile.getAbsolutePath()+". Returning");
			throw new PlatformException("Read permissions not found for file: "+oldHandlerFile.getAbsolutePath());
		}
		//Check if write permission is there
		if(!oldHandlerFile.canWrite()) {
			LOGGER.error("No write permission for: "+oldHandlerFile.getAbsolutePath()+". Write permissions are required to modify handler confib");
			throw new PlatformException("Required permissions not found for modifying file: "+oldHandlerFile.getAbsolutePath());
		}
		LOGGER.debug("Reloading configuration for handler: "+handlerName);
		this.createPrevConfigFile(handlerName);
		LOGGER.debug("Created previous configuration file");
		try {
			this.upload(modifiedHandlerConfigFile.getByteArray(), oldHandlerFile.getAbsolutePath());
		} catch (IOException e) {
			LOGGER.error("IOException while uploading file to path: "+oldHandlerFile.getAbsolutePath());
			this.restorePrevConfigFile(handlerName);
			throw new PlatformException(e);
		}
		//Unregister the TaskHandlers in the file
		for(TaskHandler taskHandler : this.configURItoHandlerName.get(oldHandlerFile.toURI())) {
			this.taskHandlerRegistry.unregisterTaskHandler(taskHandler);
			LOGGER.debug("Unregistered TaskHandler: "+taskHandler.getName());
		}
		try {
			this.componentContainer.loadComponent(this.getHandlerConfig(handlerName));
		} catch(Exception e) {
			this.restorePrevConfigFile(handlerName);
			this.componentContainer.loadComponent(this.getHandlerConfig(handlerName));
			throw new PlatformException(e);
		}
		this.removePrevConfigFile(handlerName);
	}

	/**
	 * Interface method implementation.
	 * @see com.flipkart.phantom.runtime.spi.spring.admin.SPConfigService#getAllTaskHandlers()
	 */
	public TaskHandler[] getAllTaskHandlers() {
		return this.taskHandlerRegistry.getAllTaskHandlers();
	}
	
	/**
	 * Interface method implementation.
	 * @see com.flipkart.phantom.runtime.spi.spring.admin.SPConfigService#getAllThriftProxies()
	 */
	public ThriftProxy[] getAllThriftProxies() {
		return this.thriftProxyRegistry.getAllThriftProxies();
	}

	/**
	 * Creates a temporary file, which is a duplicate of the current config file,
	 * with the name {@link SPConfigServiceImpl#PREV_HANDLER_FILE}
	 * @param handlerName Name of the Handler
	 */
	private void createPrevConfigFile(String handlerName) {
		File configFile = null;
		try {
			configFile = this.getHandlerConfig(handlerName).getFile();
		} catch (IOException e1) {
			LOGGER.error("Exception while getting handlerConfigFile",e1);
			throw new PlatformException("Exception while getting handlerConfigFile",e1);
		}
		File prevFile = new File(configFile.getParent()+"/"+SPConfigServiceImpl.PREV_HANDLER_FILE);
		try {
			prevFile.createNewFile();
		} catch (IOException e1) {
			LOGGER.error("Unable to create file: "+prevFile.getAbsolutePath()+". Please check permissions",e1);
			throw new PlatformException("Unable to create file: "+prevFile.getAbsolutePath()+". Please check permissions",e1);
		}
		if(configFile.exists()) {
			if(prevFile.exists()) {
				prevFile.delete();
			}
			configFile.renameTo(prevFile);
			try {
				configFile.createNewFile();
			} catch (IOException e) {
				LOGGER.error("IOException while clearing config File",e);
				throw new PlatformException("IOException while clearing config File",e);
				}
			prevFile.deleteOnExit();
		}
	}


	/**
	 * This method removes the temporary previous config File
	 * @param handlerName Name of the job
	 */
	private void removePrevConfigFile(String handlerName) {
		File configFile = null;
		try {
			configFile = this.getHandlerConfig(handlerName).getFile();
		} catch (IOException e) {
			LOGGER.error("IOException while getting HandlerConfigFile",e);
		}
		String prevFilePath = configFile.getParent()+SPConfigServiceImpl.PREV_HANDLER_FILE;
		File prevFile = new File(prevFilePath);
		if(prevFile.exists()){
			prevFile.delete();  // DELETE previous XML File
		}
	}

	/**
	 * Restores the previous config file, if found
	 * @param handlerName Name of the job
	 */
	private void restorePrevConfigFile(String handlerName) {
		File configFile = null;
		try {
			configFile = this.getHandlerConfig(handlerName).getFile();
		} catch (IOException e) {
			LOGGER.error("IOException while getting HandlerConfigFile",e);
		}
		if(configFile.exists()) {
			configFile.delete();
		}
		File prevFile = new File(configFile.getParent()+"/"+SPConfigServiceImpl.PREV_HANDLER_FILE);
		if(prevFile.exists()) {
			prevFile.renameTo(configFile);
		} 
		//TODO: In case of new handler, add: else { this.jobXMLFile.remove(handlerName); }
	}

	/**
	 * Uploads the file to the given path. Creates the file and directory structure, if the file
	 * or parent directory doesn't exist
	 */
	private void upload(byte[] fileContents, String destPath) throws IOException {
		File destFile = new File(destPath);
		LOGGER.debug("Uploading fileContents to path: "+destPath);
		//If exists, overwrite
		if(destFile.exists()) {
			destFile.delete();
			destFile.createNewFile();
		}
		//Creating directory structure
		try {
			destFile.getParentFile().mkdirs();
		} catch(Exception e) {
			LOGGER.error("Unable to create directory structure for uploading file");
			throw new PlatformException("Unable to create directory structure for uploading file");
		}
		FileOutputStream fos = new FileOutputStream(destFile);
		fos.write(fileContents);						
	}
	
	/**
	 * Interface method implementation.
	 * @see com.flipkart.phantom.runtime.spi.spring.admin.SPConfigService#addTaskHandlerConfigPath(java.io.File, com.flipkart.phantom.task.spi.TaskHandler)
	 */
	public void addTaskHandlerConfigPath(File taskHandlerFile, TaskHandler taskHandler) {
		if(this.configURItoHandlerName.get(taskHandlerFile.toURI())==null) {
			this.configURItoHandlerName.put(taskHandlerFile.toURI(), new LinkedList<TaskHandler>());
		}
		this.configURItoHandlerName.get(taskHandlerFile.toURI()).add(taskHandler);
	}

    /** Getter/Setter methods */
	public TaskHandlerRegistry getTaskHandlerRegistry() {
		return taskHandlerRegistry;
	}
	public void setTaskHandlerRegistry(TaskHandlerRegistry taskHandlerRegistry) {
		this.taskHandlerRegistry = taskHandlerRegistry;
	}
	public ThriftProxyRegistry getThriftProxyRegistry() {
		return thriftProxyRegistry;
	}
	public void setThriftProxyRegistry(ThriftProxyRegistry thriftProxyRegistry) {
		this.thriftProxyRegistry = thriftProxyRegistry;
	}
	public ServiceProxyComponentContainer getComponentContainer() {
		return componentContainer;
	}
	public void setComponentContainer(ServiceProxyComponentContainer componentContainer) {
		this.componentContainer = componentContainer;
	}
	public List<AbstractNetworkServer> getDeployedNetworkServers() {
		return deployedNetworkServers;
	}
	/** End Getter/Setter methods */
}
