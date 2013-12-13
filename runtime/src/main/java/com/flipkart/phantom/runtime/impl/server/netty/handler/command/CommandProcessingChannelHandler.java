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
import com.flipkart.phantom.task.impl.TaskHandler;
import com.flipkart.phantom.task.impl.TaskHandlerExecutor;
import com.flipkart.phantom.task.impl.TaskRequestWrapper;
import com.flipkart.phantom.task.impl.TaskResult;
import com.flipkart.phantom.task.spi.repository.ExecutorRepository;
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
	private ExecutorRepository repository;

    /** The publisher used to broadcast events to Service Proxy Subscribers */
    private ServiceProxyEventProducer eventProducer;

    /** Event Type for publishing all events which are generated here */
    private final static String COMMAND_HANDLER = "COMMAND_HANDLER";

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

            // Prepare the request Wrapper
            TaskRequestWrapper taskRequestWrapper = new TaskRequestWrapper();
            taskRequestWrapper.setData(readCommand.getCommandData());
            taskRequestWrapper.setParams(readCommand.getCommandParams());

			// Get the Executor :: Try to execute command using ThreadPool, if "pool" is found in the command, else the command name
			if(pool!=null) {
				executor = (TaskHandlerExecutor) this.repository.getExecutor(readCommand.getCommand(), pool,taskRequestWrapper);
			} else {
				executor = (TaskHandlerExecutor) this.repository.getExecutor(readCommand.getCommand(), readCommand.getCommand(),taskRequestWrapper);
			}
			try {
				TaskResult result = null;
				if (executor.getCallInvocationType() == TaskHandler.SYNC_CALL) {
					result = executor.execute();
				} else {
					executor.queue(); // dont wait for the result. send back a response that the call has been dispatched for async execution
					result = new TaskResult(true,TaskHandlerExecutor.ASYNC_QUEUED);
				}
				LOGGER.debug("The output is: "+ result);
				// write the results to the channel output
				commandInterpreter.writeCommandExecutionResponse(ctx, event, result);
			} catch(Exception e) {
				throw new RuntimeException("Error in executing command : " + readCommand, e);
			}
            finally {
                // Publishes event both in case of success and failure.
                Class eventSource = (executor == null) ? this.getClass() : executor.getClass();
                String commandName = (readCommand == null) ? null : readCommand.getCommand();
                final String requestID = readCommand.getCommandParams().get("requestID");
                if (eventProducer != null)
                    eventProducer.publishEvent(executor, commandName, eventSource, COMMAND_HANDLER, requestID);
                else
                    LOGGER.debug("eventProducer not set, not publishing event");
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
	public ExecutorRepository getRepository() {
		return this.repository;
	}
	public void setRepository(ExecutorRepository repository) {
		this.repository = repository;
	}
    public void setEventProducer(final ServiceProxyEventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }
    /** End Getter/Setter methods */
}


