/*
 * Copyright 2012-2015, Flipkart Internet Pvt Ltd. All rights reserved.
 * 
 * This software is the confidential and proprietary information of Flipkart Internet Pvt Ltd. ("Confidential Information").  
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of the license 
 * agreement you entered into with Flipkart.    
 * 
 */

package com.flipkart.phantom.runtime.impl.spring;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import java.io.File;

/**
 * The <code>ProxyHandlerConfigInfo</code> class is a structure that holds proxy handler configuration information and the ApplicationContext for the proxy handler
 * 
 * @author Regunath B
 * @version 1.0, 14 Mar 2013
 */
public class ProxyHandlerConfigInfo {

	/** The sub-folder containing proxy handler and dependent binaries. This is used in addition to the proxy runtime classpath.
	 *  This path is relative to the location where ServiceProxyFrameworkConstants.SPRING_PROXY_HANDLER_CONFIG file is found 
	 */
	public static final String BINARIES_PATH = "lib";

	/** The prefix to be added to file absolute paths when loading Spring XMLs using the FileSystemXmlApplicationContext*/
	public static final String FILE_PREFIX = "file:";
	
	/** The the ServiceProxyFrameworkConstants.SPRING_PROXY_HANDLER_CONFIG file containing proxy handler bean */
	private File proxyHandlerConfigXML;
	
	/** The path to proxy handler and dependent binaries*/
	private String binariesPath = ProxyHandlerConfigInfo.BINARIES_PATH;
	
	/** The Spring ApplicationContext initialized using information contained in this ProxyHandlerConfigInfo*/
	private AbstractApplicationContext proxyHandlerContext;
	
	/**
	 * Constructors
	 */
	public ProxyHandlerConfigInfo(File proxyHandlerConfigXML) {
		this.proxyHandlerConfigXML = proxyHandlerConfigXML;
	}
	public ProxyHandlerConfigInfo(File proxyHandlerConfigXML, String binariesPath) {
		this(proxyHandlerConfigXML);
		this.binariesPath = binariesPath;
	}
	public ProxyHandlerConfigInfo(File proxyHandlerConfigXML, String binariesPath,AbstractApplicationContext proxyHandlerContext) {
		this(proxyHandlerConfigXML,binariesPath);
		this.proxyHandlerContext = proxyHandlerContext;
	}

	/**
	 * Loads and returns an AbstractApplicationContext using data contained in this class
	 * @return the proxy handler's AbstractApplicationContext
	 */
	protected AbstractApplicationContext loadProxyHandlerContext(ClassLoader classLoader) {
		ClassLoader existingTCCL = Thread.currentThread().getContextClassLoader();
		// set the custom classloader as the tccl for loading the proxy handler
		Thread.currentThread().setContextClassLoader(classLoader);
		// add the "file:" prefix to file names to get around strange behavior of FileSystemXmlApplicationContext that converts absolute path 
		// to relative path
		this.proxyHandlerContext = new FileSystemXmlApplicationContext(new String[]{FILE_PREFIX + this.proxyHandlerConfigXML.getAbsolutePath()}, 
				ServiceProxyComponentContainer.getCommonProxyHandlerBeansContext());
		// now reset the thread's TCCL to the one that existed prior to loading the proxy handler
		Thread.currentThread().setContextClassLoader(existingTCCL);
		return this.proxyHandlerContext;
	}

	/**
	 * Overriden super type method. Returns true if the path to the proxy handler context is the same i.e. loaded from the same file
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object object) {
		ProxyHandlerConfigInfo otherConfigInfo = (ProxyHandlerConfigInfo)object;
		return this.getProxyHandlerConfigXML().getAbsolutePath().equalsIgnoreCase(otherConfigInfo.getProxyHandlerConfigXML().getAbsolutePath());
	}
	
	/**
	 * Overriden superclass method. Prints the proxyHandlerConfigXML details
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return  "ProxyHandlerConfigInfo [proxyHandlerConfigXML=" + proxyHandlerConfigXML + ", binariesPath=" + binariesPath + "]";
	}
	
	/** Getter methods*/	
	/**
	 * Returns the proxy handler's ApplicationContext, if loaded, else null
	 * @return null or the proxy handler's AbstractApplicationContext
	 */
	public AbstractApplicationContext getProxyHandlerContext() {
		return this.proxyHandlerContext;
	}
	public File getProxyHandlerConfigXML() {
		return this.proxyHandlerConfigXML;
	}
	public String getBinariesPath() {
		return this.binariesPath;
	}
	
}
