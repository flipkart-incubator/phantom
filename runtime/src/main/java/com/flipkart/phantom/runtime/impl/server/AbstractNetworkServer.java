/*
 * Copyright 2012-2015, Flipkart Internet Pvt Ltd. All rights reserved.
 * 
 * This software is the confidential and proprietary information of Flipkart Internet Pvt Ltd. ("Confidential Information").  
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of the license 
 * agreement you entered into with Flipkart.    
 * 
 */
package com.flipkart.phantom.runtime.impl.server;

import com.flipkart.phantom.runtime.spi.server.NetworkServer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * <code>AbstractNetworkServer</code> is an implementation of the {@link NetworkServer} that may be used as base-class
 * for creating NetworkServerS for various {@link TransmissionProtocol}
 * 
 * @author Regunath B
 * @version 1.0, 25 Jun 2013
 */
public abstract class AbstractNetworkServer implements NetworkServer, InitializingBean, DisposableBean {

	/** The default port for this server, if none is specified */
	protected static final int DEFAULT_PORT = 8181;

	/** The Logger instance for this class */
	protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractNetworkServer.class);
	
	/** The Host + port where this server is running, represented as a InetSocketAddress*/
	protected InetSocketAddress socketAddress;
	
	/** The port where this server is listening*/
	protected int portNumber = DEFAULT_PORT;
	
	/** No args constructor*/
	public AbstractNetworkServer() {
	}
	
	/**
	 * Interface method implementation. Checks if all manadatory properties have been set
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {		
		// start the server using the InetSocketAddress if specified, else use the port set
		if (this.getSocketAddress() != null) {
			this.startServer(this.getSocketAddress());
		} else {
			this.startServer(this.getPortNumber());
		}
	}

	/**
	 * Interface method implementation. Starts up this network server using the default port
	 * @see com.flipkart.phantom.runtime.spi.server.NetworkServer#startServer()
	 */
	public void startServer() throws RuntimeException {
		this.startServer(AbstractNetworkServer.DEFAULT_PORT);
	}

	/**
	 * Interface method implementation. Starts up this network server using the specified port
	 * @see com.flipkart.phantom.runtime.spi.server.NetworkServer#startServer(int)
	 */
	public void startServer(int port) throws RuntimeException {
		this.portNumber = port;
		this.startServer(new InetSocketAddress(portNumber));
	}

	/**
	 * Interface method implementation. Starts up this network server using the specified InetSocketAddress
	 * @see com.flipkart.phantom.runtime.spi.server.NetworkServer#startServer(java.net.InetSocketAddress)
	 */
	public void startServer(InetSocketAddress socketAddress) throws RuntimeException {
		this.socketAddress = socketAddress;
		this.doStartServer();
		LOGGER.info("Network Server : {} started", this.toString());
	}
	
	/**
	 * Interface method implementation. Closes the default channel group
	 * @see com.flipkart.phantom.runtime.spi.server.NetworkServer#stopServer()
	 */
	public void stopServer() throws RuntimeException {
		this.doStopServer();
		LOGGER.info("Network Server : {} stopped", this.toString());
	}
	
	/**
	 * Interface method implementation. Calls {@link #stopServer()}
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	public void destroy() throws Exception {
		this.stopServer();
	}
	
	/**
	 * Delegate method to perform implementation specific start up tasks
	 * @throws RuntimeException in case of server start up errors
	 */
	protected abstract void doStartServer() throws RuntimeException;

	/**
	 * Delegate method to perform implementation specific server shutdown tasks
	 * @throws RuntimeException in case of server stop errors
	 */
	protected abstract void doStopServer() throws RuntimeException;

    /**
     * Method to get server type
     */
    public abstract String getServerType();

    /**
     * Method do get server endpoint information
     */
    public abstract String getServerEndpoint();

	/** Start Getter/Setter methods */
	public InetSocketAddress getSocketAddress() {
		return this.socketAddress;
	}
	public void setSocketAddress(InetSocketAddress socketAddress) {
		this.socketAddress = socketAddress;
	}
	public int getPortNumber() {
		return this.portNumber;
	}
	public void setPortNumber(int portNumber) {
		this.portNumber = portNumber;
	}
	/** End Getter/Setter methods */

}
