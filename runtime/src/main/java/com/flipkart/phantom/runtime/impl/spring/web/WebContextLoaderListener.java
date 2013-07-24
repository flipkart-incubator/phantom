/*
 * Copyright 2012-2015, Flipkart Internet Pvt Ltd. All rights reserved.
 * 
 * This software is the confidential and proprietary information of Flipkart Internet Pvt Ltd. ("Confidential Information").  
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of the license 
 * agreement you entered into with Flipkart.    
 * 
 */
package com.flipkart.phantom.runtime.impl.spring.web;

import com.flipkart.phantom.runtime.impl.spring.ServiceProxyComponentContainer;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ContextLoaderListener;

import javax.servlet.ServletContext;

/**
 * A custom {@link ContextLoaderListener} that uses the ServiceProxy Runtime {@link ServiceProxyComponentContainer#getCommonProxyHandlerBeansContext()} as the parent ApplicationContext
 * 
 * @author Regunath B
 * @version 1.0, 14 Mar 2013
 */
public class WebContextLoaderListener extends ContextLoaderListener {

	/**
	 * Overriden template method. Uses the {@link ServiceProxyComponentContainer#getCommonProxyHandlerBeansContext()} as the parent application context
	 * @see org.springframework.web.context.ContextLoader#loadParentContext(javax.servlet.ServletContext)
	 */
	protected ApplicationContext loadParentContext(ServletContext servletContext) {
		return ServiceProxyComponentContainer.getCommonProxyHandlerBeansContext();
	}
}
