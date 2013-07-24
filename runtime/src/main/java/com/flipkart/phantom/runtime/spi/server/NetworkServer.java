/*
 * Copyright 2012-2015, Flipkart Internet Pvt Ltd. All rights reserved.
 * 
 * This software is the confidential and proprietary information of Flipkart Internet Pvt Ltd. ("Confidential Information").  
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of the license 
 * agreement you entered into with Flipkart.    
 * 
 */
package com.flipkart.phantom.runtime.spi.server;

import java.net.InetSocketAddress;

/**
 * <code>NetworkServer</code> is a general purpose network server that can receive and process requests on a specific transmission protocol
 * 
 * @author Regunath B
 * @version 1.0, 14 Mar 2013
 */

public interface NetworkServer {

	/**
	 * Notion of a transmission protocol
	 */
	public interface TransmissionProtocol{
	}

	/**
	 * The supported protocols
	 */
	public enum TRANSMISSION_PROTOCOL implements TransmissionProtocol {
		TCP,UDP,UDS;
	}

	/**
	 * Returns the TransmissionProtocol supported by this network server
	 * @return the valid TransmissionProtocol instance
	 */
	public TransmissionProtocol getTransmissionProtocol();

	/**
	 * Starts this network server at the default port
	 * @throws RuntimeException in case of errors in server startup
	 */
	public void startServer() throws RuntimeException;

	/**
	 * Starts this server on the specified port 
	 * @param port the port number to listen for incoming requests
	 * @throws RuntimeException in case of errors in server startup
	 */
	public void startServer(int port) throws RuntimeException;

	/**
	 * Starts this server on the specified InetSocketAddress 
	 * @param socketAddress the InetSocketAddress from which to process incoming requests 
	 * @throws RuntimeException in case of errors in server startup
	 */
	public void startServer(InetSocketAddress socketAddress) throws RuntimeException;

	/**
	 * Stops this network server
	 * @throws RuntimeException in case of errors in server stop
	 */
	public void stopServer() throws RuntimeException;

	/**
	 * Returns the InetSocketAddress for this network server
	 * @return the InetSocketAddress for this network server
	 */
	public InetSocketAddress getSocketAddress();

}
