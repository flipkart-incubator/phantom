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
package com.flipkart.sp.dashboard.impl.spring.web;

import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicConfiguration;
import com.netflix.config.FixedDelayPollingScheduler;
import com.netflix.config.PolledConfigurationSource;
import com.netflix.config.sources.URLConfigurationSource;
import com.netflix.turbine.init.TurbineInit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.trpr.platform.runtime.impl.config.FileLocator;

import java.io.File;
import java.net.URL;

/**
 * <code>TurbineInitializer</code> implements {@link InitializingBean} and does the thrift of initializing turbine by loading
 * the properties file, registering the properties to Archiaus(https://github.com/Netflix/archaius/) and 
 * calling the {@link TurbineInit#init()}
 * 
 * @author devashishshankar
 * @version 1.0, 08 April, 2013
 */
public class TurbineInitializer implements InitializingBean {
	
	/** The prefix to be added to file absolute paths when loading the properties file */
	private static final String FILE_PREFIX = "file:";	

	/** Logger for this class*/
	private static final Logger LOGGER = LoggerFactory.getLogger(TurbineInitializer.class);

	/**
	 * Interface method implementation. @see InitializingBean#afterPropertiesSet
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		LOGGER.info("Initializing turbine");
		try {
			//Find the config file
			File turbineConfig = FileLocator.findUniqueFile("dashboard-config.properties");
			LOGGER.debug("Found dashboard config file: "+turbineConfig.getAbsolutePath());
			//Add it as a configuration source
			PolledConfigurationSource source = new URLConfigurationSource(new URL(FILE_PREFIX+turbineConfig.getAbsolutePath()));
			DynamicConfiguration configuration = new DynamicConfiguration(source, new FixedDelayPollingScheduler());
			//Add the configurations to Archiaus
			ConfigurationManager.install(configuration);
			//Init Turbine
			TurbineInit.init();
			LOGGER.debug("Successfully inited Turbine");
		}  catch(Exception e) {
			LOGGER.error("Error configuring and initing turbine",e);
		}
	}
}
