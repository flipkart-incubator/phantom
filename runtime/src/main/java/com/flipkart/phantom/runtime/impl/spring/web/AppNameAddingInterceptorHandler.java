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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.trpr.platform.core.PlatformConstants;
import org.trpr.platform.runtime.common.RuntimeVariables;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * User: root
 * Date: 10/6/13
 * Time: 5:03 PM
 */
public class AppNameAddingInterceptorHandler extends HandlerInterceptorAdapter implements InitializingBean {

    /** Default application name in case application name is not defined in bootstrap */
    public static String DEFAULT_APPLICATION_NAME = "Phantom";

    /** The property name for application name in the view (ftl) */
    public static String APP_NAME_PROPERTY = "appName";

    /** Application name to be displayed on the Admin console */
    public String applicationName = DEFAULT_APPLICATION_NAME;

    /**
     * Interface method implementation.
     * Initializes the application Name.
     */
    public void afterPropertiesSet(){
        String appName = RuntimeVariables.getVariable(PlatformConstants.TRPR_APP_NAME);
        if(appName!=null) {
            this.applicationName = appName;
        }
    }


    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        if(modelAndView!=null) {
            modelAndView.addObject(APP_NAME_PROPERTY,this.applicationName);
        }
        super.postHandle(request, response, handler, modelAndView);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
