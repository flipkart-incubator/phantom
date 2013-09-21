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
package com.flipkart.phantom.runtime.impl.spring.utils;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.trpr.platform.core.PlatformException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
/**
 * <code> {@link ConfigFileUtils} </code> provides methods for performing useful 
 * operations on Task Handler configuration file contents.
 * 
 * @author devashishshankar
 * @version 1.0, 2nd May, 2013
 */
public class ConfigFileUtils {
	/** Tag names used in spring batch files */
	private static final String BATCH_JOB_TAG = "batch:job";
	private static final String ID_PROP = "id";

	/** Logger instance for this class*/
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigFileUtils.class);

	/**
	 * Gets the handler names from Config file
	 * @param configFile Task Handler config file or its contents as a <code> Resource </code>
	 * @return List of taskHandler names, null if unable to find a TaskHandler name.
	 */
	public static List<String> getHandlerNames(Resource configFile) {
		return ConfigFileUtils.getHandlerNames(new ByteArrayResource(ConfigFileUtils.getContents(configFile).getBytes()));
	}

	/**
	 * Gets the task handler names from Config file
	 * @param configFile job config file contents as a <code> ByteArrayResource </code>
	 * @return List of task handler names, null if unable to find a TaskHandler name.
	 */	
	public static List<String> getHandlerNames(ByteArrayResource configFile) {
		List<String> jobNameList = new LinkedList<String>();
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.parse(configFile.getInputStream());
			Element docEle = dom.getDocumentElement();
			//get a nodelist of nodes with the name "ConfigFileUtils.BATCH_JOB_TAG" 
			NodeList nl = docEle.getElementsByTagName(ConfigFileUtils.BATCH_JOB_TAG);
			//Loop over all found nodes
			if(nl != null && nl.getLength() > 0) {
				for(int i = 0 ; i < nl.getLength();i++) {
					//get the element
					Element el = (Element)nl.item(i);
					if(el.hasAttribute(ConfigFileUtils.ID_PROP)) {
						jobNameList.add(el.getAttribute(ConfigFileUtils.ID_PROP));
					}
				}
			}	
		}
		catch(Exception e) {
			LOGGER.error("Unable to get the job name from the given Spring Batch configuration file", e);
			throw new PlatformException(e);
		}
		return jobNameList;
	}
	
	/**
	 * Gets the contents of a <code>Resource</code> in a single String
	 * @param resource Resource to be read
	 * @return Contents as a <code>String<code/>
	 */
	public static String getContents(Resource resource) {
		StringWriter writer = new StringWriter();
		try {
			IOUtils.copy(resource.getInputStream(), writer, "UTF-8");
		} catch (IOException e) {
			LOGGER.error("Exception while reading file "+resource.getFilename(),e);
		}
		return writer.toString();
	}
}
