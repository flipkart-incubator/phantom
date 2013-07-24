/*
 * Copyright 2012-2015, Flipkart Internet Pvt Ltd. All rights reserved.
 * 
 * This software is the confidential and proprietary information of Flipkart Internet Pvt Ltd. ("Confidential Information").  
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of the license 
 * agreement you entered into with Flipkart.    
 * 
 */
package com.flipkart.phantom.runtime;

/**
 * The <code>ServiceProxyFrameworkConstants</code> class is a placeholder for all service proxy runtime framework
 * constants.
 * 
 * @author Regunath B
 * @version 1.0, 14 Mar 2013
 */
public class ServiceProxyFrameworkConstants {

	/**
	 * Constants for the conventions on config file names
	 */
	public static final String COMMON_PROXY_CONFIG = "packaged/common-proxy-config.xml"; // its a file picked up from classpath
	public static final String COMMON_PROXY_SERVER_NATURE_CONFIG = "packaged/common-proxy-server-nature-config.xml"; // its a file picked up from classpath	
	public static final String SPRING_PROXY_LISTENER_CONFIG = "spring-proxy-listener-config.xml"; // files picked up from config locations
	public static final String SPRING_PROXY_HANDLER_CONFIG = "spring-proxy-handler-config.xml"; // files picked up from config locations

}
