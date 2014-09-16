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

package com.flipkart.phantom.netty.uds;

import static org.jboss.netty.channel.Channels.fireChannelOpen;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.netty.channel.AbstractServerChannel;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelSink;
import org.jboss.netty.channel.socket.DefaultServerSocketChannelConfig;
import org.jboss.netty.channel.socket.ServerSocketChannel;
import org.jboss.netty.channel.socket.ServerSocketChannelConfig;
import org.newsclub.net.unix.AFUNIXServerSocket;

/**
 * <code>OioServerSocketChannel</code> is based on org.jboss.netty.channel.socket.oio.OioServerSocketChannel,
 * but uses Unix Domain Sockets ({@link AFUNIXServerSocket}) instead of ServerSocket
 * 
 * @author devashishshankar
 * @version 1.0, 19th April, 2013
 */
public class OioServerSocketChannel extends AbstractServerChannel implements ServerSocketChannel {

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