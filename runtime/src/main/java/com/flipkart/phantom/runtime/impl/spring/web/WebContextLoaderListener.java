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
package com.flipkart.phantom.runtime.impl.spring.web;

import java.io.File;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.web.context.ContextLoaderListener;
import org.trpr.platform.core.PlatformException;
import org.trpr.platform.runtime.impl.config.FileLocator;

import com.flipkart.phantom.runtime.impl.spring.ServiceProxyComponentContainer;

/**
 * A custom {@link ContextLoaderListener} that uses the ServiceProxy Runtime {@link ServiceProxyComponentContainer#getCommonProxyHandlerBeansContext()} as the parent ApplicationContext
 * by default. Additionally looks for a file by name {@link WebContextLoaderListener#COMMON_WEB_CONFIG} that may be used by apps to define additional common beans. When such a file exists,
 * the parent ApplicationContext includes beans defined in the file.
 * 
 * @author Regunath B
 * @version 1.0, 14 Mar 2013
 */
public class WebContextLoaderListener extends ContextLoaderListener {

	/** Constant for the config file containing common beans available to web application contexts */
	public static final String COMMON_WEB_CONFIG = "common-web-config.xml"; // files picked up from config locations
	
    /** The prefix to be added to file absolute paths when loading Spring XMLs using the FileSystemXmlApplicationContext */
    private static final String FILE_PREFIX = "file:";
	
    /** Logger for this class*/
    private static final Logger LOGGER = LoggerFactory.getLogger(WebContextLoaderListener.class);
	
	/**
	 * Overriden template method. Uses the {@link ServiceProxyComponentContainer#getCommonProxyHandlerBeansContext()} as the parent application context
	 * @see org.springframework.web.context.ContextLoader#loadParentContext(javax.servlet.ServletContext)
	 */
	protected ApplicationContext loadParentContext(ServletContext servletContext) {
		ApplicationContext parentContext = ServiceProxyComponentContainer.getCommonProxyHandlerBeansContext(); 
        File[] commonWebConfigFiles = FileLocator.findFiles(WebContextLoaderListener.COMMON_WEB_CONFIG);
        if (commonWebConfigFiles.length > 0) {
            if (commonWebConfigFiles.length == 1) {
            	parentContext = new FileSystemXmlApplicationContext(new String[]{FILE_PREFIX + commonWebConfigFiles[0].getAbsolutePath()},parentContext);
                LOGGER.info("Loaded Common Proxy Handler Config: " + commonWebConfigFiles[0]);
            } else {
                final String errorMessage = "Found multiple common-web-config.xml, only one is allowed";
                LOGGER.error(errorMessage);
                for (File commonHandlerConfig : commonWebConfigFiles) {
                    LOGGER.error(commonHandlerConfig.getAbsolutePath());
                }
                throw new PlatformException(errorMessage);
            }
        }
		return parentContext;
	}
}
