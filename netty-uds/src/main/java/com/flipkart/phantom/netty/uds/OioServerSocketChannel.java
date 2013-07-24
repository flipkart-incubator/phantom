/*
 * Copyright 2012-2015, Flipkart Internet Pvt Ltd. All rights reserved.
 * 
 * This software is the confidential and proprietary information of Flipkart Internet Pvt Ltd. ("Confidential Information").  
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of the license 
 * agreement you entered into with Flipkart.    
 * 
 */

package com.flipkart.phantom.netty.uds;

import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.DefaultServerSocketChannelConfig;
import org.jboss.netty.channel.socket.ServerSocketChannel;
import org.jboss.netty.channel.socket.ServerSocketChannelConfig;
import org.newsclub.net.unix.AFUNIXServerSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.jboss.netty.channel.Channels.fireChannelOpen;

/**
 * <code>OioServerSocketChannel</code> is based on org.jboss.netty.channel.socket.oio.OioServerSocketChannel,
 * but uses Unix Domain Sockets ({@link AFUNIXServerSocket}) instead of ServerSocket
 * 
 * @author devashishshankar
 * @version 1.0, 19th April, 2013
 */
public class OioServerSocketChannel extends AbstractServerChannel implements ServerSocketChannel {

	/** Logger for this class*/
	private static final Logger LOGGER = LoggerFactory.getLogger(OioServerSocketChannel.class);

	/** Lock to prevent shutting down this channel until Boss Thread exists */
    final Lock shutdownLock = new ReentrantLock();
    
    /** The UNIX domain socket instance */
	public AFUNIXServerSocket socket;
	
	/** The config for the Socket */
	private final ServerSocketChannelConfig config;

	/** Default controller */
	OioServerSocketChannel(ChannelFactory factory, ChannelPipeline pipeline, ChannelSink sink) {
		super(factory, pipeline, sink);
		try {
			socket = AFUNIXServerSocket.newInstance(); //New Instance for UDS
		} catch (IOException e) {
			throw new ChannelException("Failed to open a server socket.", e);
		}
		config = new DefaultServerSocketChannelConfig(socket);
		fireChannelOpen(this); //Fire a request to bind
	}

	/** Getter/Setter methods */
	public ServerSocketChannelConfig getConfig() {
		return config;
	}
	public InetSocketAddress getLocalAddress() {
		return (InetSocketAddress) socket.getLocalSocketAddress();
	}
	public InetSocketAddress getRemoteAddress() {
		return null;
	}
	public boolean isBound() {
		return isOpen() & socket.isBound();
	}
	public boolean isConnected() {
		return false;
	}
	@Override
	protected boolean setClosed() {
		return super.setClosed();
	}
	@Override
	protected ChannelFuture getSucceededFuture() {
		return super.getSucceededFuture();
	}
	/** End Getter/Setter methods */
}