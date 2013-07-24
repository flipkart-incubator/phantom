/*
 * Copyright 2012-2015, Flipkart Internet Pvt Ltd. All rights reserved.
 * 
 * This software is the confidential and proprietary information of Flipkart Internet Pvt Ltd. ("Confidential Information").  
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of the license 
 * agreement you entered into with Flipkart.    
 * 
 */
package com.flipkart.phantom.runtime.impl.server.netty;

import com.flipkart.phantom.runtime.impl.server.AbstractNetworkServer;
import org.jboss.netty.bootstrap.Bootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * <code>AbstractNettyNetworkServer</code> is a sub-type of {@link AbstractNetworkServer} that uses {@link http://netty.io/} 
 * for creating NetworkServerS for various {@link TransmissionProtocol}
 * 
 * @author Regunath B
 * @version 1.0, 14 Mar 2013
 */

public abstract class AbstractNettyNetworkServer extends AbstractNetworkServer {
		
	/** The default channel group*/
	protected ChannelGroup defaultChannelGroup;
	
	/** The Netty Bootstrap instance*/
	protected Bootstrap serverBootstrap;
	
	/** The Netty ChannelPipelineFactory instance*/
	protected ChannelHandlerPipelineFactory pipelineFactory;
		
	/** Map of Netty Bootstrap options*/
	private Map<String,Object> bootstrapOptions = new HashMap<String,Object>();	
		
	/** No args constructor*/
	public AbstractNettyNetworkServer() {
	}
	
	/**
	 * Interface method implementation. Checks if all manadatory properties have been set
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {		
		Assert.notNull(this.pipelineFactory, "The 'pipelineFactory' may not be null");	
		this.serverBootstrap = this.createServerBootstrap();
		this.serverBootstrap.setPipelineFactory(this.pipelineFactory);
		this.serverBootstrap.setOptions(this.bootstrapOptions);
		// call the super class implementation to start up this server
		super.afterPropertiesSet();
	}

	/**
	 * Overriden superclass method. Creates a new Server Channel and adds it to the default channel group
	 * @see com.flipkart.phantom.runtime.impl.server.AbstractNetworkServer#doStartServer()
	 */
	protected void doStartServer() throws RuntimeException {
		this.defaultChannelGroup.add(createChannel());
	}
	
	/**
	 * Overriden superclass method. Closes all channels registered with the default channel group, the pipleline factory and the serverbootstrap
	 * @see com.flipkart.phantom.runtime.impl.server.AbstractNetworkServer#doStopServer()
	 */
	protected void doStopServer() throws RuntimeException {
		// close all channels registered with the default channel group
		ChannelGroupFuture future = this.defaultChannelGroup.close();
		future.awaitUninterruptibly();
		// close the pipleline factory and instruct the serverbootstrap to release external resources
		this.pipelineFactory.close();
		this.serverBootstrap.releaseExternalResources();
	}
	
	/**
	 * Delegated method to concrete implementations to create the appropriate Netty Bootstrap instance
	 * @return the transmission protocol specific Bootstrap instance
	 */
	protected abstract Bootstrap createServerBootstrap() throws RuntimeException ;
	
	/**
	 * Delegated method to concrete implementations to create the appropriate Netty Channel
	 */
	protected abstract Channel createChannel() throws RuntimeException;
	
	/** Start Getter/Setter methods */
	public Bootstrap getServerBootstrap() {
		return this.serverBootstrap;
	}	
	public ChannelHandlerPipelineFactory getPipelineFactory() {
		return this.pipelineFactory;
	}
	public void setPipelineFactory(ChannelHandlerPipelineFactory pipelineFactory) {
		this.pipelineFactory = pipelineFactory;
	}
	public Map<String, Object> getBootstrapOptions() {
		return this.bootstrapOptions;
	}
	public void setBootstrapOptions(Map<String, Object> bootstrapOptions) {
		this.bootstrapOptions = bootstrapOptions;
	}
	public ChannelGroup getDefaultChannelGroup() {
		return this.defaultChannelGroup;
	}
	public void setDefaultChannelGroup(ChannelGroup defaultChannelGroup) {
		this.defaultChannelGroup = defaultChannelGroup;
	}			
	/** End Getter/Setter methods */
}
