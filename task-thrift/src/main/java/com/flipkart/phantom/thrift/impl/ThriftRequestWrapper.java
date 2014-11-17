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

package com.flipkart.phantom.thrift.impl;

import java.util.List;
import java.util.Map;

import com.flipkart.phantom.task.spi.RequestWrapper;
import com.google.common.base.Optional;

import org.apache.thrift.transport.TTransport;

/**
 * <code>ThriftRequestWrapper</code> has the input and output buffers for the Thrift-request.
 *
 * @author : arya.ketan
 * @version : 1.0
 * @date : 28/10/13
 */
public class ThriftRequestWrapper extends RequestWrapper {

     /** The client socket */
    private TTransport clientSocket;
    
    /** The method being invoked */
    private String methodName;
    
    /**
     * Interface method implementation. Returns the Thrift method name being invoked
     * @see com.flipkart.phantom.task.spi.RequestWrapper#getRequestName()
     */
    public String getRequestName() {
    	return this.getMethodName();
    }

    /**
     * Abstract method implementation. Returns the request method name 
     * @see com.flipkart.phantom.task.spi.RequestWrapper#getRequestMetaData()
     */
    public Optional<String> getRequestMetaData() {
    	return Optional.of(getRequestName());
    }    
    
    /**
     * Abstract method implementation. Ignores the headers as Thrift protocol does not support passing headers
     * @see com.flipkart.phantom.task.spi.RequestWrapper#setHeaders(java.util.List)
     */
    public  void setHeaders(List<Map.Entry<String, String>> headers) {
    	// no op as we dont have a way to define headers in the Thrift protocol
    }
    
    /** Start Getter/Setter methods */
    public TTransport getClientSocket(){
        return clientSocket;
    }
    public void setClientSocket(TTransport clientSocket){
        this.clientSocket = clientSocket;
    }
	public String getMethodName() {
		return methodName;
	}
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}    
    /**End Getter/Setter methods */
}
