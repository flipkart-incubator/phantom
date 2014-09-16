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

package com.flipkart.phantom.task.spi.registry;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import java.io.File;

/**
 * The <code>HandlerConfigInfo</code> class is a structure that holds proxy handler configuration information and the ApplicationContext for the proxy handler
 * 
 * @author Regunath B
 * @version 1.0, 14 Mar 2013
 */
public class HandlerConfigInfo {
	
	/** Constants defining the load order */
	public static final int FIRST_ORDER = 0;
	public static final int DEFAULT_ORDER = 1;
	
	/** The load order*/
	private int loadOrder = HandlerConfigInfo.DEFAULT_ORDER;

	/** The sub-folder containing proxy handler and dependent binaries. This is used in addition to the proxy runtime classpath.
	 *  This path is relative to the location where ServiceProxyFrameworkConstants.SPRING_PROXY_HANDLER_CONFIG file is found 
	 */
	public static final String BINARIES_PATH = "lib";

	/** The prefix to be added to file absolute paths when loading Spring XMLs using the FileSystemXmlApplicationContext*/
	public static final String FILE_PREFIX = "file:";
	
	/** The the ServiceProxyFrameworkConstants.SPRING_PROXY_HANDLER_CONFIG file containing proxy handler bean */
	private File xmlConfigFile;
	
	/** The path to proxy handler and dependent binaries*/
	private String binariesPath = HandlerConfigInfo.BINARIES_PATH;
	
	/** The Spring ApplicationContext initialized using information contained in this HandlerConfigInfo*/
	private AbstractApplicationContext proxyHandlerContext;
	
	/**
	 * Constructors
	 */
	public HandlerConfigInfo(File xmlConfigFile) {
		this.xmlConfigFile = xmlConfigFile;
	}
	public HandlerConfigInfo(File xmlConfigFile, String binariesPath) {
		this(xmlConfigFile);
		this.binariesPath = binariesPath;
	}
	public HandlerConfigInfo(File xmlConfigFile, String binariesPath, AbstractApplicationContext proxyHandlerContext) {
		this(xmlConfigFile,binariesPath);
		this.proxyHandlerContext = proxyHandlerContext;
	}

	/**
	 * Loads and returns an AbstractApplicationContext using data contained in this class
	 * @return the proxy handler's AbstractApplicationContext
	 */
	public AbstractApplicationContext loadProxyHandlerContext(ClassLoader classLoader, AbstractApplicationContext applicationContext) {
		ClassLoader existingTCCL = Thread.currentThread().getContextClassLoader();
		// set the custom classloader as the tccl for loading the proxy handler
		Thread.currentThread().setContextClassLoader(classLoader);
		// add the "file:" prefix to file names to get around strange behavior of FileSystemXmlApplicationContext that converts absolute path 
		// to relative path
		this.proxyHandlerContext = new FileSystemXmlApplicationContext(
                new String[]{FILE_PREFIX + this.xmlConfigFile.getAbsolutePath()},
				applicationContext
        );
		// now reset the thread's TCCL to the one that existed prior to loading the proxy handler
		Thread.currentThread().setContextClassLoader(existingTCCL);
		return this.proxyHandlerContext;
	}

	/**
	 * Overriden super type method. Returns true if the path to the proxy handler context is the same i.e. loaded from the same file
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object object) {
		HandlerConfigInfo otherConfigInfo = (HandlerConfigInfo)object;
		return this.getXmlConfigFile().getAbsolutePath().equalsIgnoreCase(otherConfigInfo.getXmlConfigFile().getAbsolutePath());
	}
	
	/**
	 * Overriden superclass method. Prints the xmlConfigFile details
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return  "HandlerConfigInfo [xmlConfigFile=" + xmlConfigFile + ", binariesPath=" + binariesPath + "]";
	}
	
	/** Getter methods*/
	/**
	 * Returns the proxy handler's ApplicationContext, if loaded, else null
	 * @return null or the proxy handler's AbstractApplicationContext
	 */
	public AbstractApplicationContext getProxyHandlerContext() {
		return this.proxyHandlerContext;
	}
	public File getXmlConfigFile() {
		return this.xmlConfigFile;
	}
	public String getBinariesPath() {
		return this.binariesPath;
	}
	public int getLoadOrder() {
		return loadOrder;
	}
	public void setLoadOrder(int loadOrder) {
		this.loadOrder = loadOrder;
	}	
}
