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
import com.flipkart.phantom.task.impl.TaskResult;
import com.flipkart.phantom.task.spi.TaskHandler;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>CommandProcessingChannelHandler</code> is a sub-type of {@link SimpleChannelHandler} that implements command processing of the service proxy.
 * The command protocol is described in {@link CommandInterpreter}.
 * It wraps the service call using a {@link TaskHandlerExecutor} that provides useful features like monitoring, fallback etc.
 * 
 * @author Regunath B
 * @version 1.0, 18 Mar 2013
 */
public class CommandProcessingChannelHandler extends SimpleChannelUpstreamHandler {

	/** Logger for this class*/
	private static final Logger LOGGER = LoggerFactory.getLogger(CommandProcessingChannelHandler.class);

	/** The param key for pool name */
	public static final String POOL_PARAM = "pool";

	/** The default channel group*/
	private ChannelGroup defaultChannelGroup;

	/** The TaskRepository to lookup TaskHandlerExecutors from */
	private TaskHandlerExecutorRepository repository;

	/**
	 * Overriden superclass method. Adds the newly created Channel to the default channel group and calls the super class {@link #channelOpen(ChannelHandlerContext, ChannelStateEvent)} method
	 * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#channelOpen(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
	 */
	public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent event) throws Exception {
		super.channelOpen(ctx, event);
		this.defaultChannelGroup.add(event.getChannel());
	}

	/**
	 * Interface method implementation. Reads and processes commands sent to the service proxy. Expects data in the command protocol defined in the class summary.
	 * Discards commands that do not have a {@link TaskHandler} mapping. 
	 * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent event) throws Exception {    
		if (MessageEvent.class.isAssignableFrom(event.getClass())) {			
			CommandInterpreter commandInterpreter = new CommandInterpreter();
			CommandInterpreter.ProxyCommand readCommand = commandInterpreter.readCommand((MessageEvent)event);	
			LOGGER.debug("Read Command : " + readCommand);
			String pool = readCommand.getCommandParams().get("pool");
			TaskHandlerExecutor executor;
			//Try to execute command using ThreadPool, if "pool" is found in the command, else the command name
			if(pool!=null) {
				executor = this.repository.get(readCommand.getCommand(),pool);
			} else {
				executor = this.repository.get(readCommand.getCommand(),readCommand.getCommand());
			}
			executor.setParams(readCommand.getCommandParams());
			executor.setData(readCommand.getCommandData());
			try {
				TaskResult result = executor.execute();
				LOGGER.debug("The output is: "+ result);
				// write the results to the channel output
				commandInterpreter.writeCommandExecutionResponse(ctx, event, result);
			} catch(Exception e) {
				LOGGER.error("Error in executing command/fallBack : " + readCommand, e);
				throw new RuntimeException("Error in executing command : " + readCommand, e);
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


