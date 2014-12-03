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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import com.flipkart.phantom.event.ServiceProxyEvent;
import com.flipkart.phantom.event.ServiceProxyEventProducer;
import com.flipkart.phantom.task.impl.TaskHandlerExecutor;
import com.flipkart.phantom.task.impl.TaskHandlerExecutorRepository;
import com.flipkart.phantom.task.impl.TaskRequestWrapper;
import com.flipkart.phantom.task.impl.TaskResult;
import com.flipkart.phantom.task.impl.collector.EventDispatchingSpanCollector;
import com.flipkart.phantom.task.impl.interceptor.ServerRequestInterceptor;
import com.flipkart.phantom.task.spi.RequestContext;
import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.FixedSampleRateTraceFilter;
import com.github.kristofa.brave.ServerSpan;
import com.github.kristofa.brave.ServerTracer;
import com.github.kristofa.brave.TraceFilter;
import com.google.common.base.Optional;

/**
 * <code>AsyncCommandProcessingChannelHandler</code> is similar to @link{CommandProcessingChannelHandler} except it executes the
 * commands asynchronously.
 *
 * @author devashish.shankar
 * @version 1.0, 10 Jun 2013
 */
@SuppressWarnings("rawtypes")
public class AsyncCommandProcessingChannelHandler extends SimpleChannelUpstreamHandler implements InitializingBean {

	/** Logger for this class*/
	private static final Logger LOGGER = LoggerFactory.getLogger(AsyncCommandProcessingChannelHandler.class);

	/** The param key for pool name */
	public static final String POOL_PARAM = "pool";

    /** Event Type for publishing all events which are generated here */
    private final static String ASYNC_COMMAND_HANDLER = "ASYNC_COMMAND_HANDLER";

    /** The default name of the server/service this channel handler is serving*/
    private static final String DEFAULT_SERVICE_NAME = "Async Command Proxy";

    /** The default value for tracing frequency. This value indicates that tracing if OFF*/
    private static final TraceFilter NO_TRACING = new FixedSampleRateTraceFilter(-1);    
    
	/** Default host name and port where this ChannelHandler is available */
	public static final String DEFAULT_HOST = "localhost"; // unresolved local host name
	public static final int DEFAULT_PORT = -1; // no valid port really
    
    /** The local host name value*/
    private static String hostName = DEFAULT_HOST;
    static {
    	try {
			hostName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			LOGGER.warn("Unable to resolve local host name. Will use default host name : " + DEFAULT_HOST);
		}
    }
    
    /** The name for the service/server*/
    private String serviceName = DEFAULT_SERVICE_NAME;
    
    /** The port where the server for this handler is listening on*/
    private int hostPort;
    
	/** The default channel group*/
	private ChannelGroup defaultChannelGroup;

	/** The TaskRepository to lookup TaskHandlerExecutors from */
	private TaskHandlerExecutorRepository repository;

    /** The publisher used to broadcast events to Service Proxy Subscribers */
    private ServiceProxyEventProducer eventProducer;

    /** The request tracing frequency for this channel handler*/
    private TraceFilter traceFilter = NO_TRACING;	
    
    /** The EventDispatchingSpanCollector instance used in tracing requests*/
    private EventDispatchingSpanCollector eventDispatchingSpanCollector;    

    /**
     * Interface method implementation. Checks if all mandatory properties have been set
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(this.eventDispatchingSpanCollector, "The 'eventDispatchingSpanCollector' may not be null");        
    }
    
    /**
     * Overriden superclass method. Stores the host port that this handler's server is listening on
     * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#channelBound(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
     */
    public void channelBound(ChannelHandlerContext ctx,ChannelStateEvent event) throws Exception {
    	super.channelBound(ctx, event);
    	if (InetSocketAddress.class.isAssignableFrom(event.getValue().getClass())) {
    		this.hostPort = ((InetSocketAddress)event.getValue()).getPort();
    	}
    }
        
    /**
	 * Overriden superclass method. Adds the newly created Channel to the default channel group and calls the super class {@link #channelOpen(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)} method
	 * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#channelOpen(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
	 */
	public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent event) throws Exception {
		super.channelOpen(ctx, event);
		this.defaultChannelGroup.add(event.getChannel());
	}

    /**
	 * Overridden method. Reads and processes commands sent to the service proxy. Expects data in the command protocol defined in the class summary.
	 * Discards commands that do not have a {@link com.flipkart.phantom.task.impl.TaskHandler} mapping.
	 * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent event) throws Exception {
        long receiveTime = System.currentTimeMillis();
        if (MessageEvent.class.isAssignableFrom(event.getClass())) {
            CommandInterpreter commandInterpreter = new CommandInterpreter();
            CommandInterpreter.ProxyCommand readCommand = commandInterpreter.readCommand((MessageEvent) event);
            LOGGER.debug("Read Command : " + readCommand);
            String pool = readCommand.getCommandParams().get("pool");
            String commandName = readCommand.getCommand();
            String poolName;
            if (pool != null) {
                poolName = pool;
            } else {
                poolName = commandName;
            }

            /** Prepare the request Wrapper */
            TaskRequestWrapper taskRequestWrapper = new TaskRequestWrapper();
            taskRequestWrapper.setCommandName(commandName);
            taskRequestWrapper.setData(readCommand.getCommandData());
            taskRequestWrapper.setParams(readCommand.getCommandParams());
            // set the service name for the request
            taskRequestWrapper.setServiceName(Optional.of(this.serviceName));

            // Create and process a Server request interceptor. This will initialize the server tracing
            ServerRequestInterceptor<TaskRequestWrapper, TaskResult> serverRequestInterceptor = this.initializeServerTracing(taskRequestWrapper);
            
            TaskHandlerExecutor executor = (TaskHandlerExecutor) this.repository.getExecutor(commandName, poolName, taskRequestWrapper);

            /** Execute */
            Optional<RuntimeException> transportError = Optional.absent();            
            try {
                this.repository.executeAsyncCommand(commandName, poolName, taskRequestWrapper);
                LOGGER.debug("Successfully started execution for async command " + commandName);
            } catch (Exception e) {
            	RuntimeException runtimeException = new RuntimeException("Error asynchronously executing the command : " + readCommand, e);
            	transportError = Optional.of(runtimeException);
                LOGGER.error("Error asynchronously executing the command", e); // we just log the error as it is async anyway and callee will not be able to do much
            } finally {
            	// finally inform the server request tracer
            	serverRequestInterceptor.process(new TaskResult(true, TaskHandlerExecutor.ASYNC_QUEUED), transportError);
                if (eventProducer != null) {
                    // Publishes event both in case of success and failure.
                    final String requestID = readCommand.getCommandParams().get("requestID");
                    ServiceProxyEvent.Builder eventBuilder;
                    if (executor == null) {
                        eventBuilder = new ServiceProxyEvent.Builder(readCommand.getCommand(), ASYNC_COMMAND_HANDLER).withEventSource(getClass().getName());
                    } else {
                        eventBuilder = executor.getEventBuilder().withCommandData(executor).withEventSource(executor.getClass().getName());
                    }
                    eventBuilder.withRequestId(requestID).withRequestReceiveTime(receiveTime);
                    eventProducer.publishEvent(eventBuilder.build());
                } else {
                    LOGGER.debug("eventProducer not set, not publishing event");
                }
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

    /**
     * Initializes server tracing for the specified request
     * @param executorHttpRequest the Http request 
     * @return the initialized ServerRequestInterceptor
     */
    private ServerRequestInterceptor<TaskRequestWrapper, TaskResult> initializeServerTracing(TaskRequestWrapper executorRequest) {
        ServerRequestInterceptor<TaskRequestWrapper, TaskResult> serverRequestInterceptor = new ServerRequestInterceptor<TaskRequestWrapper, TaskResult>();
    	List<TraceFilter> traceFilters = Arrays.<TraceFilter>asList(this.traceFilter);    
    	ServerTracer serverTracer = Brave.getServerTracer(this.eventDispatchingSpanCollector, traceFilters);
    	serverRequestInterceptor.setEndPointSubmitter(Brave.getEndPointSubmitter());
        serverRequestInterceptor.setServerTracer(serverTracer);
        serverRequestInterceptor.setServiceHost(AsyncCommandProcessingChannelHandler.hostName);
        serverRequestInterceptor.setServicePort(this.hostPort);
        serverRequestInterceptor.setServiceName(this.serviceName);   
        // now process the request to initialize tracing
        serverRequestInterceptor.process(executorRequest); 
		// set the server request context on the received request
    	ServerSpan serverSpan = Brave.getServerSpanThreadBinder().getCurrentServerSpan();
    	RequestContext serverRequestContext = new RequestContext();
    	serverRequestContext.setCurrentServerSpan(serverSpan);	
    	executorRequest.setRequestContext(Optional.of(serverRequestContext));
        return serverRequestInterceptor;
    }
	
	/** Start Getter/Setter methods */
	public ChannelGroup getDefaultChannelGroup() {
		return this.defaultChannelGroup;
	}
    public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
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


