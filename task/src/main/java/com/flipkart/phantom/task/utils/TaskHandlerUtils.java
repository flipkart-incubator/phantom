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

package com.flipkart.phantom.task.utils;

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
