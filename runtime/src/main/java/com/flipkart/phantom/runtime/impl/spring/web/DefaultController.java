/*
 * Copyright 2012-2015, Flipkart Internet Pvt Ltd. All rights reserved.
 * 
 * This software is the confidential and proprietary information of Flipkart Internet Pvt Ltd. ("Confidential Information").  
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of the license 
 * agreement you entered into with Flipkart.    
 * 
 */

package com.flipkart.phantom.runtime.impl.spring.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;

/**
 * The <code>DefaultController</code> is the Default Controller for Services Proxy Admin
 * 
 * @author devashishshankar
 * @version 1.0, 18 March 2013 
 */
@Controller
public class DefaultController {
	
	/** 
	 * Controller for dashboard
	 */
	@RequestMapping(value = {"/dashboard"}, method = RequestMethod.GET)
	public String dashboard(ModelMap model, HttpServletRequest request) {
		return "dashboard";
	}
}