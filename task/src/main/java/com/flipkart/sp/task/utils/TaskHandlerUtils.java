/* Copyright 2012-2015, Flipkart Internet Pvt Ltd. All rights reserved.
 * 
 * This software is the confidential and proprietary information of Flipkart Internet Pvt Ltd. ("Confidential Information").  
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of the license 
 * agreement you entered into with Flipkart.    
 * 
 */
package com.flipkart.sp.task.utils;

import java.util.Map;

/**
 * <code>TaskHandlerUtils</code> provides useful utility methods for Task Handlers. 
 * Methods are based on: com.flipkart.w3.agent.AbstractTaskHandler
 * 
 * @author devashishshankar
 * @version 1.0, 20th March, 2013
 */
public class TaskHandlerUtils {

	/**
	 * Gets the parameter as an integer
	 * @param params The map in which parameter has to be looked up
	 * @param key The parameter key
	 * @param def default
	 */
	public static final int getIntegerParam(Map<String,String> params, String key, int def) {
		try {
			return Integer.parseInt(params.get(key));
		} catch (Exception e) {
			return def;
		}
	}

	/**
	 * Gets the parameter as a boolean
	 * @param params The map in which parameter has to be looked up
	 * @param key The parameter key
	 * @param def default
	 */
	public static final boolean getBooleanParam(Map<String,String> params, String key, boolean def) {
		try {
			String val = params.get(key);
			if(val==null || (!"true".equals(val) && !"false".equals(val))) return def;
			return Boolean.parseBoolean(val);
		} catch (Exception e) {
			return def;
		}
	}

	/**
	 * Gets the parameter as a String
	 * @param params The map in which parameter has to be looked up
	 * @param key The parameter key
	 * @param def default
	 */
	public static final String getParam(Map<String,String> params, String key, String def) {
		String val = params.get(key);
		return val==null ? def : val;
	}

}
