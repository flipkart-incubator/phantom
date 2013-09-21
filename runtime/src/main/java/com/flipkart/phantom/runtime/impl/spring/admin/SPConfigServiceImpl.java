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
package com.flipkart.phantom.runtime.impl.spring.admin;

import com.flipkart.phantom.runtime.impl.server.AbstractNetworkServer;
import com.flipkart.phantom.runtime.impl.spring.ServiceProxyComponentContainer;
import com.flipkart.phantom.runtime.spi.spring.admin.SPConfigService;
import com.flipkart.phantom.task.impl.TaskContextFactory;
import com.flipkart.phantom.task.spi.AbstractHandler;
import com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.trpr.platform.core.PlatformException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * <code>SPConfigServiceImpl</code> is an implementation of {@link SPConfigService}.
 * 
 * @author devashishshankar
 * @version 1.0, 2nd May, 2013
 */
public class SPConfigServiceImpl  implements SPConfigService {

	/** Logger instance for this class*/
	private static final Logger LOGGER = LoggerFactory.getLogger(SPConfigServiceImpl.class);
	
	/** The previous handler file (save file) */
	public static final String PREV_HANDLER_FILE = "/spring-proxy-handler-prev.xml";
	
	/** The componentCOntainer instance for reloading the config file */
	private ServiceProxyComponentContainer componentContainer;
	
	/** The map holding the mapping of a config file to it's handler name */
	private Map<URI, List<AbstractHandler> > configURItoHandlerName = new HashMap<URI,List<AbstractHandler>>();
	
	/** The list of deployed TCP Servers */
	private List<AbstractNetworkServer> deployedNetworkServers = new LinkedList<AbstractNetworkServer>();

    /** List of repositories */
    private List<AbstractHandlerRegistry> registries = new ArrayList<AbstractHandlerRegistry>();

    /**
     * Interface method implementation
     * @see com.flipkart.phantom.runtime.spi.spring.admin.SPConfigService#getDeployedNetworkServers()
     */
    public List<AbstractNetworkServer> getDeployedNetworkServers() {
        return deployedNetworkServers;
    }

    /**
     * Interface method implementation
     * @see com.flipkart.phantom.runtime.spi.spring.admin.SPConfigService#addDeployedNetworkServer(com.flipkart.phantom.runtime.impl.server.AbstractNetworkServer)
     */
    public void addDeployedNetworkServer(AbstractNetworkServer server) {
        deployedNetworkServers.add(server);
    }

    /**
     * Interface method implementation
     * @see com.flipkart.phantom.runtime.spi.spring.admin.SPConfigService#addHandlerRegistry(com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry)
     */
    public void addHandlerRegistry(AbstractHandlerRegistry registry) {
        registries.add(registry);
    }

    /**
     * Interface method implementation.
     * @see com.flipkart.phantom.runtime.spi.spring.admin.SPConfigService#getHandlerConfig(java.lang.String)
     */
    public Resource getHandlerConfig(String handlerName) {
        for (URI configFile: this.configURItoHandlerName.keySet()) {
            for (AbstractHandler handler : this.configURItoHandlerName.get(configFile)) {
                if (handler.getName().equals(handlerName)) {
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

    	// Check if exists
    	File oldHandlerFile = null;
    	try {
    		oldHandlerFile = this.getHandlerConfig(handlerName).getFile();
    	} catch (IOException e1) {
    		LOGGER.error("Handler Config File for handler: "+handlerName+" not found. Returning");
    		throw new PlatformException("File not found for handler: "+handlerName,e1);
    	}
    	if (!oldHandlerFile.exists()) {
    		LOGGER.error("Handler Config File: "+oldHandlerFile.getAbsolutePath()+" doesn't exist. Returning");
    		throw new PlatformException("File not found: "+oldHandlerFile.getAbsolutePath());
    	}

        // Check for read permissions
    	if (!oldHandlerFile.canRead()) {
    		LOGGER.error("No read permission for: "+oldHandlerFile.getAbsolutePath()+". Returning");
    		throw new PlatformException("Read permissions not found for file: "+oldHandlerFile.getAbsolutePath());
    	}

    	// Check for write permissions
    	if (!oldHandlerFile.canWrite()) {
    		LOGGER.error("No write permission for: "+oldHandlerFile.getAbsolutePath()+". Write permissions are required to modify handler confib");
    		throw new PlatformException("Required permissions not found for modifying file: "+oldHandlerFile.getAbsolutePath());
    	}

        // create backup
    	this.createPrevConfigFile(handlerName);

        // file_put_contents Java :-/
    	try {
    		this.upload(modifiedHandlerConfigFile.getByteArray(), oldHandlerFile.getAbsolutePath());
    	} catch (IOException e) {
    		LOGGER.error("IOException while uploading file to path: "+oldHandlerFile.getAbsolutePath());
    		this.restorePrevConfigFile(handlerName);
    		throw new PlatformException(e);
    	}

    	// get the registered AbstractHandler for the file name
    	AbstractHandler handler = this.configURItoHandlerName.get(oldHandlerFile.toURI()).get(0);
        // re-load the handler
        // TODO
        // loading component destroys all beans in the config file given by the handler config info
        // this mean this only works if only one handler per handler xml file
    	try {
    		this.componentContainer.reloadHandler(handler, this.getHandlerConfig(handlerName));
    	} catch(Exception e) {
    		this.restorePrevConfigFile(handlerName);
    		this.componentContainer.loadComponent(this.getHandlerConfig(handlerName));
    		throw new PlatformException(e);
    	}
    	this.removePrevConfigFile(handlerName);
    }


	/**
	 * Creates a temporary file, which is a duplicate of the current config file,
	 * with the name {@link SPConfigServiceImpl#PREV_HANDLER_FILE}
	 * @param handlerName Name of the Handler
	 */
	private void createPrevConfigFile(String handlerName) {

        // get current file
		File configFile = null;
		try {
			configFile = this.getHandlerConfig(handlerName).getFile();
		} catch (IOException e1) {
			LOGGER.error("Exception while getting handlerConfigFile",e1);
			throw new PlatformException("Exception while getting handlerConfigFile",e1);
		}

        // create the backup file
		File prevFile = new File(configFile.getParent()+"/"+SPConfigServiceImpl.PREV_HANDLER_FILE);

        // move current -> backup, create new current file
		if (configFile.exists()) {
			if (prevFile.exists()) {
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
		if (prevFile.exists()) {
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
	}

	/**
	 * Uploads the file to the given path. Creates the file and directory structure, if the file
	 * or parent directory doesn't exist
	 */
	private void upload(byte[] fileContents, String destPath) throws IOException {

		File destFile = new File(destPath);

		// If exists, overwrite
		if (destFile.exists()) {
			destFile.delete();
			destFile.createNewFile();
		}
		// Creating directory structure
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
	 * @see com.flipkart.phantom.runtime.spi.spring.admin.SPConfigService#addHandlerConfigPath(java.io.File, com.flipkart.phantom.task.spi.AbstractHandler)
	 */
	public void addHandlerConfigPath(File taskHandlerFile, AbstractHandler handler) {
		if (this.configURItoHandlerName.get(taskHandlerFile.toURI()) == null) {
			this.configURItoHandlerName.put(taskHandlerFile.toURI(), new LinkedList<AbstractHandler>());
		}
		this.configURItoHandlerName.get(taskHandlerFile.toURI()).add(handler);
	}

    /**
     * Interface method implementation
     * @see com.flipkart.phantom.runtime.spi.spring.admin.SPConfigService#reinitHandler(String)
     * @param handlerName The name of the TaskHandler to be re-inited
     * @throws Exception in case of errors
     */
    public void reinitHandler(String handlerName) throws Exception {
        for (AbstractHandlerRegistry registry : registries) {
            if (registry.getHandler(handlerName) != null) {
                registry.reinitHandler(handlerName,TaskContextFactory.getTaskContext());
            }
        }
    }

    /**
     * Interface method implementation
     * @see com.flipkart.phantom.runtime.spi.spring.admin.SPConfigService#getAllHandlers()
     */
    public List<AbstractHandler> getAllHandlers() {
        List<AbstractHandler> list = new ArrayList<AbstractHandler>();
        for (AbstractHandlerRegistry registry : registries) {
            list.addAll(registry.getHandlers());
        }
        return list;
    }

    /** Getter/Setter methods */
	public ServiceProxyComponentContainer getComponentContainer() {
		return componentContainer;
	}
	public void setComponentContainer(ServiceProxyComponentContainer componentContainer) {
		this.componentContainer = componentContainer;
	}
	/** End Getter/Setter methods */
}
