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
	public static final String COMMON_PROXY_HANDLER_CONFIG = "common-proxy-handler-config.xml"; // files picked up from config locations

}
