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

package com.flipkart.sp.task.impl.thrift.proxy;

import java.net.InetSocketAddress;
import java.net.Socket;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.trpr.platform.core.impl.logging.LogFactory;
import org.trpr.platform.core.spi.logging.Logger;

import com.flipkart.sp.task.spi.thrift.ThriftProxy;

/**
 * <code>SocketObjectFactory</code> is a @link{PoolableObjectFactory} for Socket instances meant to be used with {@link GenericObjectPool}
 * It is initialized with a Thrift proxy or it's parameters and is passed onto a GenericObjectPool object
 *
 * @author devashishshankar
 * @author Regunath B
 * @version 1.0, 7th June, 2013
 * @version 2.0, 5th July, 2013
 */
public class SocketObjectFactory implements PoolableObjectFactory<Socket> {

	/** Logger for this class*/
	private static final Logger LOGGER = LogFactory.getLogger(SocketObjectFactory.class);
	
    /** Thrift Proxy instance for initializing the Factory */
    private ThriftProxy thriftProxy;

    /**
     * Constructor for initializing this Factory with a ThriftProxy
     * @param thriftProxy
     */
    public SocketObjectFactory(ThriftProxy thriftProxy) {
        this.setThriftProxy(thriftProxy);
    }

    /**
     * Interface method implementation. Creates and returns a new {@link Socket}
     * @see org.apache.commons.pool.PoolableObjectFactory#makeObject()
     */
    public Socket makeObject() throws Exception {
       Socket socket = new Socket();
       socket.setSoTimeout(this.getThriftProxy().getThriftTimeoutMillis());
       socket.connect(new InetSocketAddress(this.getThriftProxy().getThriftServer(),  this.getThriftProxy().getThriftPort()));
       LOGGER.info("Creating a new socket for server : {} at port : {}", this.getThriftProxy().getThriftServer(), this.getThriftProxy().getThriftPort());
       return socket;
    }

    /**
     * Interface method implementation. Closes the specified Socket instance
     * @see org.apache.commons.pool.PoolableObjectFactory#destroyObject(java.lang.Object)
     */
    public void destroyObject(Socket socket) throws Exception {
        LOGGER.info("Closing a socket for server : {} at port : {}", this.getThriftProxy().getThriftServer(), this.getThriftProxy().getThriftPort());
        socket.close();
    }

    /**
     * Interface method implementation. Checks if the socket is open and then attempts to set Thrift specific socket properties.
     * An error in any of these operations will invalidate the specified Socket.
     * @see org.apache.commons.pool.PoolableObjectFactory#validateObject(java.lang.Object)
     */
    public boolean validateObject(Socket socket) {
    	if (socket.isClosed()) {
    		return false;
    	}
    	try {
	      socket.setSoLinger(false, 0);
	      socket.setTcpNoDelay(true);
	      return true;
    	} catch (Exception e) {
	        LOGGER.info("Socket is not valid for server : {} at port : {}", this.getThriftProxy().getThriftServer(), this.getThriftProxy().getThriftPort());
			return false;
		}
    }

    /**
     * Interface method implementation. Does nothing
     * @see org.apache.commons.pool.PoolableObjectFactory#activateObject(java.lang.Object)
     */
    public void activateObject(Socket socket) throws Exception {
    	// no op
    }

    /**
     * Interface method implementation. Does nothing
     * @see org.apache.commons.pool.PoolableObjectFactory#passivateObject(java.lang.Object)
     */
    public void passivateObject(Socket socket) throws Exception {
    	// no op
    }

    /** Getter/Setter Methods */
    public ThriftProxy getThriftProxy() {
        return thriftProxy;
    }
    public void setThriftProxy(ThriftProxy thriftProxy) {
        this.thriftProxy = thriftProxy;
    }
    /** End Getter/Setter Methods */
}
