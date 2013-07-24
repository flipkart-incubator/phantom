/*
 * Copyright 2012-2015, Flipkart Internet Pvt Ltd. All rights reserved.
 * 
 * This software is the confidential and proprietary information of Flipkart Internet Pvt Ltd. ("Confidential Information").  
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of the license 
 * agreement you entered into with Flipkart.    
 * 
 */
package com.flipkart.phantom.runtime.impl.server.netty.handler.command;

import com.flipkart.phantom.task.impl.TaskHandlerExecutor;
import com.flipkart.phantom.task.impl.TaskHandlerExecutorRepository;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * <code>AsyncCommandProcessingChannelHandler</code> is similar to @link{CommandProcessingChannelHandler} except it executes the
 * commands asynchronously.
 *
 * @author devashish.shankar
 * @version 1.0, 10 Jun 2013
 */
public class AsyncCommandProcessingChannelHandler extends SimpleChannelUpstreamHandler {

	/** Logger for this class*/
	private static final Logger LOGGER = LoggerFactory.getLogger(AsyncCommandProcessingChannelHandler.class);

	/** The param key for pool name */
	public static final String POOL_PARAM = "pool";

	/** The default channel group*/
	private ChannelGroup defaultChannelGroup;

	/** The TaskRepository to lookup TaskHandlerExecutors from */
	private TaskHandlerExecutorRepository repository;

	/**
	 * Overriden superclass method. Adds the newly created Channel to the default channel group and calls the super class {@link #channelOpen(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)} method
	 * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#channelOpen(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
	 */
	public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent event) throws Exception {
		super.channelOpen(ctx, event);
		this.defaultChannelGroup.add(event.getChannel());
	}

	/**
	 * Interface method implementation. Reads and processes commands sent to the service proxy. Expects data in the command protocol defined in the class summary.
	 * Discards commands that do not have a {@link com.flipkart.sp.task.spi.task.TaskHandler} mapping.
	 * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent event) throws Exception {    
		if (MessageEvent.class.isAssignableFrom(event.getClass())) {			
			CommandInterpreter commandInterpreter = new CommandInterpreter();
			CommandInterpreter.ProxyCommand readCommand = commandInterpreter.readCommand((MessageEvent)event);	
			LOGGER.debug("Read Command : " + readCommand);
			String pool = readCommand.getCommandParams().get("pool");
			TaskHandlerExecutor executor;
            String commandName = readCommand.getCommand();
            String poolName;
            if(pool!=null) {
                poolName = pool;
            } else {
                poolName = commandName;
            }
            Map params = readCommand.getCommandParams();
            byte[] data = readCommand.getCommandData();
            try {
                this.repository.executeAsyncCommand(commandName,poolName,data,params);
                LOGGER.debug("Successfully started execution for async command "+commandName);
            } catch(Exception e) {
                LOGGER.error("Error asynchronously executing the command", e);
            }
		} 		
		super.handleUpstream(ctx, event);
	}	

	/**
	 * Interface method implementation. Closes the underlying channel after logging a warning message
	 * @see org.jboss.netty.channel.SimpleChannelHandler#exceptionCaught(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ExceptionEvent)
	 */
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent event) throws Exception {
		LOGGER.warn("Exception {} thrown on Channel {}. Disconnect initiated",event,event.getChannel());
		event.getChannel().close();
	}

	/** Start Getter/Setter methods */
	public ChannelGroup getDefaultChannelGroup() {
		return this.defaultChannelGroup;
	}
	public void setDefaultChannelGroup(ChannelGroup defaultChannelGroup) {
		this.defaultChannelGroup = defaultChannelGroup;
	}
	public TaskHandlerExecutorRepository getRepository() {
		return this.repository;
	}
	public void setRepository(TaskHandlerExecutorRepository repository) {
		this.repository = repository;
	}
	/** End Getter/Setter methods */
}


