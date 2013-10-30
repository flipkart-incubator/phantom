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
package com.flipkart.phantom.runtime.impl.server.netty.handler.command;

import com.flipkart.phantom.event.ServiceProxyEventProducer;
import com.flipkart.phantom.task.impl.TaskHandlerExecutor;
import com.flipkart.phantom.task.impl.TaskHandlerExecutorRepository;
import com.flipkart.phantom.task.impl.TaskRequestWrapper;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /** The publisher used to broadcast events to Service Proxy Subscribers */
    private ServiceProxyEventProducer eventProducer;

    /** Event Type for publishing all events which are generated here */
    private final static String ASYNC_COMMAND_HANDLER = "ASYNC_COMMAND_HANDLER";

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
	 * Discards commands that do not have a {@link com.flipkart.phantom.task.impl.TaskHandler} mapping.
	 * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent event) throws Exception {    
		if (MessageEvent.class.isAssignableFrom(event.getClass())) {			
			CommandInterpreter commandInterpreter = new CommandInterpreter();
			CommandInterpreter.ProxyCommand readCommand = commandInterpreter.readCommand((MessageEvent)event);	
			LOGGER.debug("Read Command : " + readCommand);
			String pool = readCommand.getCommandParams().get("pool");
            String commandName = readCommand.getCommand();
            String poolName;
            if(pool!=null) {
                poolName = pool;
            } else {
                poolName = commandName;
            }

             /** Prepare the request Wrapper */
            TaskRequestWrapper taskRequestWrapper = new TaskRequestWrapper();
            taskRequestWrapper.setData(readCommand.getCommandData());
            taskRequestWrapper.setParams(readCommand.getCommandParams());
            TaskHandlerExecutor executor = (TaskHandlerExecutor) this.repository.getExecutor(commandName, poolName, taskRequestWrapper);

            /** Execute */
            try {
                this.repository.executeAsyncCommand(commandName,poolName,taskRequestWrapper);
                LOGGER.debug("Successfully started execution for async command "+commandName);
            } catch(Exception e) {
                LOGGER.error("Error asynchronously executing the command", e);
            }
            finally {
                // Publishes event both in case of success and failure.
                Class eventSource = (executor == null) ? this.getClass() : executor.getTaskHandler().getClass();
                commandName = (readCommand == null) ? null : readCommand.getCommand();
                eventProducer.publishEvent(executor, commandName, eventSource, ASYNC_COMMAND_HANDLER);
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
    public void setEventProducer(final ServiceProxyEventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }
    /** End Getter/Setter methods */
}


