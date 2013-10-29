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

import com.flipkart.phantom.task.spi.RequestWrapper;
import org.apache.thrift.transport.TTransport;

/**
 * <code>ThriftRequestWrapper</code> has the input and output buffers for the Thrift-request.
 *
 * @author : arya.ketan
 * @version : 1.0
 * @date : 28/10/13
 */
public class ThriftRequestWrapper implements RequestWrapper {

     /** The client socket */
    private TTransport clientSocket;

    /** Start Getter/Setter methods */
    public TTransport getClientSocket(){
        return clientSocket;
    }

    public void setClientSocket(TTransport clientSocket){
        this.clientSocket = clientSocket;
    }
      /**End Getter/Setter methods */
}
