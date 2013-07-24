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
