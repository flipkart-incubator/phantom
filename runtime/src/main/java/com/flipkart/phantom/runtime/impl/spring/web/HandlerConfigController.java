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

import com.flipkart.phantom.runtime.impl.spring.utils.ConfigFileUtils;
import com.flipkart.phantom.runtime.spi.spring.admin.SPConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * The <code>HandlerConfigController</code> is a controller for handling configuration changes.
 * It uses {@link SPConfigService} for getting/modifying configurations
 * 
 * @author devashishshankar
 * @version 1.0, 18 March 2013 
 */
@Controller
public class HandlerConfigController {

    /** Logger instance for this class*/
    private static final Logger LOGGER = LoggerFactory.getLogger(HandlerConfigController.class);

	/** Config service for fetching/modifying configuration files of Handlers */
	private SPConfigService configService;
	
	/**
	 * Finds the jobname from the request URL
	 */
	@ModelAttribute("handlerName")
	public String getJobName(HttpServletRequest request) {
		String path = request.getServletPath();
		int index = path.lastIndexOf("handler/") + 8;
		if (index >= 0) {
			path = path.substring(index);
		}
		return path;
	}

    @RequestMapping(value = {"/viewConfig/**"}, method = RequestMethod.GET)
    public String viewConfig(ModelMap model, HttpServletRequest request, @ModelAttribute("handlerName") String handlerName) {
        model.addAttribute("taskHandlers", this.configService.getAllTaskHandlers());
        model.addAttribute("servletPath", request.getContextPath());
        Resource handlerFile = this.configService.getHandlerConfig(handlerName);
        if(handlerFile==null) {
            model.addAttribute("XMLFileContents","Sorry, this file cannot be viewed. Maybe this TaskHandler wasn't defined in spring-proxy-handler-config.xml");
        } else {
            model.addAttribute("XMLFileContents",ConfigFileUtils.getContents(handlerFile));
        }
        return "viewConfig";
    }

    @RequestMapping(value = {"/reInit/**"}, method = RequestMethod.GET)
    public String reInitHandler(ModelMap model, HttpServletRequest request, @ModelAttribute("handlerName") String handlerName) {
        String message;
        try {
            this.configService.reinitTaskHandler(handlerName);
            message = "Successfully reinited "+handlerName;
        } catch (Exception e) {
            LOGGER.error("Error reiniting",e);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            message = "Error while re deploying handler: <br />";
            message += sw.toString(); // stack trace as a string
            if(e.getCause()!=null) {
                sw = new StringWriter();
                pw = new PrintWriter(sw);
                e.getCause().printStackTrace(pw);
                message+="<br /> Caused by: ";
                message += sw.toString(); // stack trace as a string
            }
        }
        model.addAttribute("message",message);
        return "message";
    }

	@RequestMapping(value = {"/deploy/**"}, method = RequestMethod.POST)
	public String deployModifiedConfig(ModelMap model,HttpServletRequest request,  @ModelAttribute("handlerName") String handlerName,
			@RequestParam(defaultValue = "") String XMLFileContents, 
			@RequestParam(defaultValue = "0") String identifier) {
		//Save the file
		XMLFileContents = XMLFileContents.trim();
		if(identifier.equals("Save")) {
			try {
				this.configService.modifyHandlerConfig(handlerName, new ByteArrayResource(XMLFileContents.getBytes()));
			} catch (Exception e) { //Loading failed
				model.addAttribute("XMLFileError", "Unable to deploy file");
				StringWriter errors = new StringWriter();
				e.printStackTrace(new PrintWriter(errors));
				model.addAttribute("LoadingError", errors.toString());
				if(errors.toString()==null) {
					model.addAttribute("LoadingError", "Unexpected error");
				}
				model.addAttribute("XMLFileContents",ConfigFileUtils.getContents(this.configService.getHandlerConfig(handlerName)));				
				return "modifyConfig";
			}
		}
		//Loading success
		model.addAttribute("SuccessMessage", "Successfully Deployed the new Handler Configuration");
		model.addAttribute("taskHandlers", this.configService.getAllTaskHandlers());
		Resource handlerFile = this.configService.getHandlerConfig(handlerName);
		model.addAttribute("XMLFileContents",ConfigFileUtils.getContents(handlerFile));
		try {
			model.addAttribute("XMLFileName",handlerFile.getURI());
		} catch (IOException e) {
			model.addAttribute("XMLFileName","File not found");
		}
		return "viewConfig";
	}
	
	@RequestMapping(value = {"**/modify/**"}, method = RequestMethod.GET)
	public String modifyConfig(ModelMap model, HttpServletRequest request, @ModelAttribute("handlerName") String handlerName,
			@RequestParam(defaultValue = "") String XMLFileContents, 
			@RequestParam(defaultValue = "0") String identifier) {
		model.addAttribute("taskHandlers", this.configService.getAllTaskHandlers());
		model.addAttribute("servletPath", request.getContextPath());
		Resource handlerFile = this.configService.getHandlerConfig(handlerName);
		model.addAttribute("XMLFileContents",ConfigFileUtils.getContents(handlerFile));
		try {
			model.addAttribute("XMLFileName",handlerFile.getURI());
		} catch (IOException e) {
			model.addAttribute("XMLFileName","File not found");
		}
		return "modifyConfig";
	}
	
	/** 
	 * Controller for configuration page
	 */
	@RequestMapping(value = {"/configuration"}, method = RequestMethod.GET)
	public String configuration(ModelMap model, HttpServletRequest request) {
		model.addAttribute("taskHandlers", this.configService.getAllTaskHandlers());
		model.addAttribute("thriftProxies", this.configService.getAllThriftProxies());
		model.addAttribute("networkServers", this.configService.getDeployedNetworkServers());
		//model.addAttribute("UDSServers", this.configService.getDeployedUDSServers());
		//model.addAttribute("UDSOIOServers", this.configService.getDeployedUDSOIOServers());

		model.addAttribute("servletPath", request.getContextPath());
		return "configuration";
	}

	/** Getter/Setter methods */
	public SPConfigService getConfigService() {
		return configService;
	}
	public void setConfigService(SPConfigService configService) {
		this.configService = configService;
	}
	/** End Getter/Setter methods */
}