/*
 * Copyright 2012-2015, Flipkart Internet Pvt Ltd. All rights reserved.
 * 
 * This software is the confidential and proprietary information of Flipkart Internet Pvt Ltd. ("Confidential Information").  
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of the license 
 * agreement you entered into with Flipkart.    
 * 
 */
package com.flipkart.phantom.runtime.impl.server.netty;

import com.flipkart.phantom.runtime.impl.server.concurrent.NamedThreadFactory;
import org.jboss.netty.bootstrap.Bootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <code>TCPNettyServer</code> is a concrete implementation of the {@link AbstractNettyNetworkServer} for {@link TRANSMISSION_PROTOCOL#TCP}
 * 
 * @author Regunath B
 * @version 1.0, 15 Mar 2013
 */
public class TCPNettyServer extends AbstractNettyNetworkServer {
	
	/** The default counts (invalid one) for server and worker pool counts*/
	private static final int INVALID_POOL_SIZE = -1;
	
	/** The server and worker thread pool sizes*/
	private int serverPoolSize = INVALID_POOL_SIZE;
	private int workerPoolSize = INVALID_POOL_SIZE;

	/** The server and worker ExecutorService instances*/
	private ExecutorService serverExecutors;
	private ExecutorService workerExecutors;
	
	/**
	 * Interface method implementation. Returns {@link TRANSMISSION_PROTOCOL#TCP}
	 * @see com.flipkart.phantom.runtime.spi.server.NetworkServer#getTransmissionProtocol()
	 */
	public TransmissionProtocol getTransmissionProtocol() {
		return TRANSMISSION_PROTOCOL.TCP;
	}

	/**
	 * Interface method implementation. Creates server and worker thread pools if required and then calls {@link #afterPropertiesSet()} on the super class
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {		
		if (this.getServerExecutors() == null) { // no executors have been set for server listener
			if (this.getServerPoolSize() != TCPNettyServer.INVALID_POOL_SIZE) { // thread pool size has been set. create and use a fixed thread pool
				this.setServerExecutors(Executors.newFixedThreadPool(this.getServerPoolSize(), new NamedThreadFactory("TCPServer-Listener")));
			} else { // default behavior of creating and using a cached thread pool
				this.setServerExecutors(Executors.newCachedThreadPool(new NamedThreadFactory("TCPServer-Listener")));
			}
		}
		if (this.getWorkerExecutors() == null) {  // no executors have been set for workers
			if (this.getWorkerPoolSize() != TCPNettyServer.INVALID_POOL_SIZE) { // thread pool size has been set. create and use a fixed thread pool
				this.setWorkerExecutors(Executors.newFixedThreadPool(this.getWorkerPoolSize(), new NamedThreadFactory("TCPServer-Worker")));
			}else { // default behavior of creating and using a cached thread pool
				this.setWorkerExecutors(Executors.newCachedThreadPool(new NamedThreadFactory("TCPServer-Worker")));
			}
		}
		super.afterPropertiesSet();
	}

	/**
	 * Overriden super class method. Returns a readable string for this TCPNettyServer
	 * @see java.lang.Object#toString()
	 */
	public String toString(){
		return "TCPNettyServer [socketAddress=" + socketAddress + ", portNumber=" + portNumber + "] " + this.getPipelineFactory();
	}
	
	/**
	 * Interface method implementation. Creates and returns a Netty ServerBootstrap instance
	 * @see com.flipkart.phantom.runtime.impl.server.netty.AbstractNetworkServer#createServerBootstrap()
	 */
	protected Bootstrap createServerBootstrap() throws RuntimeException {
		return new ServerBootstrap(new NioServerSocketChannelFactory(this.getServerExecutors(), this.getWorkerExecutors()));
	}

	/**
	 * Abstract method implementation. Creates and returns a Netty Channel from the ServerBootstrap that was
	 * previously created in {@link TCPNettyServer#createServerBootstrap()}
	 * @see com.flipkart.phantom.runtime.impl.server.netty.AbstractNetworkServer#createChannel()
	 */
	protected Channel createChannel() throws RuntimeException {
		if (this.getServerBootstrap() == null) {
			throw new RuntimeException("Error creating Channel. Bootstrap instance cannot be null. See TCPNettyServer#createServerBootstrap()");
		}
		return ((ServerBootstrap)this.serverBootstrap).bind(this.socketAddress);
	}

    /**
     * Abstract method implementation. Returns server type as string.
     */
    public String getServerType() {
        return "TCP Netty Server";
    }

    /**
     * Abstract method implementation. Returns server endpoint as string.
     */
    public String getServerEndpoint() {
        return ""+this.portNumber;
    }

	/** Start Getter/Setter methods */
	public int getServerPoolSize() {
		return this.serverPoolSize;
	}
	public void setServerPoolSize(int serverPoolSize) {
		this.serverPoolSize = serverPoolSize;
	}
	public int getWorkerPoolSize() {
		return this.workerPoolSize;
	}
	public void setWorkerPoolSize(int workerPoolSize) {
		this.workerPoolSize = workerPoolSize;
	}
	public ExecutorService getServerExecutors() {
		return this.serverExecutors;
	}
	public void setServerExecutors(ExecutorService serverExecutors) {
		this.serverExecutors = serverExecutors;
	}
	public ExecutorService getWorkerExecutors() {
		return this.workerExecutors;
	}
	public void setWorkerExecutors(ExecutorService workerExecutors) {
		this.workerExecutors = workerExecutors;
	}	
	/** End Getter/Setter methods */
	
}
