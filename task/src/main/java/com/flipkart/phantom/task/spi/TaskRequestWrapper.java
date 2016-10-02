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

package com.flipkart.phantom.task.spi;

import java.util.List;
import java.util.Map;

import com.flipkart.phantom.task.spi.RequestWrapper;
import com.google.common.base.Optional;

/**
 *
 * <code>TaskRequestWrapper</code> has the data bytes and the parameters map for the Command protocol-request.
 *
 * @author : arya.ketan
 * @version : 1.0
 * @date : 28/10/13
 */
public class TaskRequestWrapper<S> extends RequestWrapper {

    /** Data bytes */
    private S data;

    /** Map of parameters */
    private Map<String, Object> params;
    
    /** All header names and values of this task request.*/
    private Optional<List<Map.Entry<String, String>>> headers = Optional.absent();
    
    /** The command name being executed */
    private String commandName;
    
    /**
     * Returns the command name as the request name
     * @see com.flipkart.phantom.task.spi.RequestWrapper#getRequestName()
     */
    public String getRequestName() {
    	return this.getCommandName();
    }
    
    /**
     * Abstract method implementation. Returns a concat string of the request method name (i.e. GET, POST) and the request params
     * @see com.flipkart.phantom.task.spi.RequestWrapper#getRequestMetaData()
     */
    public Optional<String> getRequestMetaData() {
    	return Optional.of(getRequestName() + " " + this.getParams());
    }    
    
    /**
     * Abstract method implementation. Stores the headers in a local variable for use by respective handler, if the transport
     * can support it.
     * @see com.flipkart.phantom.task.spi.RequestWrapper#setHeaders(java.util.List)
     */
    public  void setHeaders(List<Map.Entry<String, String>> headers) {
        this.headers = Optional.of(headers);
    }

    /**
     * Abstract method implementation. Returns the stored headers, if any that has been set.
     * @see com.flipkart.phantom.task.spi.RequestWrapper#getHeaders()
     */
    public Optional<List<Map.Entry<String, String>>> getHeaders() {
        return headers;
    }
    
    /**Start Getter/Setter methods */
    public S getData(){
        return data;
    }
    
    public void setData(S data){
        this.data = data;
    }
    public Map<String, Object> getParams(){
        return params;
    }
    public void setParams(Map<String, Object> params){
        this.params = params;
    }
	public String getCommandName() {
		return commandName;
	}
	public void setCommandName(String commandName) {
		this.commandName = commandName;
	}    
    /**End Getter/Setter methods */

}
