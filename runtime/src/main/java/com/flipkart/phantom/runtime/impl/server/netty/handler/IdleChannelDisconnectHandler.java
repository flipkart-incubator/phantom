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
package com.flipkart.phantom.runtime.impl.server.netty.handler;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.handler.timeout.IdleState;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler;
import org.jboss.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * <code>IdleChannelDisconnectHandler</code> is a subtype of {@link IdleStateAwareChannelHandler} that closes the underlying channel when an idle state event
 * is received or an exception event on the channel is caught
 * 
 * @author Regunath B
 * @version 1.0, 15 Mar 2013
 */

public class IdleChannelDisconnectHandler extends IdleStateAwareChannelHandler {
	
	/** Logger for this class*/
	private static final Logger LOGGER = LoggerFactory.getLogger(IdleChannelDisconnectHandler.class);
	
	/**
	 * Interface method implementation. Closes the underlying channel after logging a warning message
	 * @see org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler#channelIdle(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.handler.timeout.IdleStateEvent)
	 */
	public void channelIdle(ChannelHandlerContext ctx, IdleStateEvent event) throws Exception {
		if(event.getState() == IdleState.ALL_IDLE){
			LOGGER.warn("Channel {} is idle. Disconnect initiated",event.getChannel());
			event.getChannel().close();
		}
	}

	/**
	 * Interface method implementation. Closes the underlying channel after logging a warning message
	 * @see org.jboss.netty.channel.SimpleChannelHandler#exceptionCaught(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ExceptionEvent)
	 */
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent event) throws Exception {
		StringWriter errors = new StringWriter();
		event.getCause().printStackTrace(new PrintWriter(errors));
		LOGGER.warn("Exception {} thrown on Channel {}. Disconnect initiated. Stack Trace: "+errors,event,event.getCause());
		event.getChannel().close();
	}
	
}
