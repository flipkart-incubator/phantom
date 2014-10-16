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

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
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
	@RequestMapping(value = {"/cluster-dashboard"}, method = RequestMethod.GET)
	public String dashboard(ModelMap model, HttpServletRequest request) {
		return "cluster-dashboard";
	}

    @RequestMapping(value = {"/command/{cmd}"}, method = RequestMethod.GET)
    public String command(@PathVariable String cmd, ModelMap model) {
        model.addAttribute("command", cmd);
        return "command";
    }


    @RequestMapping(value = {"/tp/{tp}"}, method = RequestMethod.GET)
    public String tp(@PathVariable String tp, ModelMap model) {
        model.addAttribute("tp", tp);
        return "tp";
    }


}