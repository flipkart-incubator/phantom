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
package com.flipkart.phantom.runtime.impl.server.netty.handler.thrift;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TTransport;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flipkart.phantom.event.ServiceProxyEvent;
import com.flipkart.phantom.event.ServiceProxyEventProducer;
import com.flipkart.phantom.runtime.impl.server.netty.channel.thrift.ThriftNettyChannelBuffer;
import com.flipkart.phantom.task.impl.TaskRequestWrapper;
import com.flipkart.phantom.task.impl.TaskResult;
import com.flipkart.phantom.task.impl.collector.EventDispatchingSpanCollector;
import com.flipkart.phantom.task.impl.interceptor.ServerRequestInterceptor;
import com.flipkart.phantom.task.spi.Executor;
import com.flipkart.phantom.task.spi.RequestContext;
import com.flipkart.phantom.task.spi.repository.ExecutorRepository;
import com.flipkart.phantom.thrift.impl.ThriftProxy;
import com.flipkart.phantom.thrift.impl.ThriftProxyExecutor;
import com.flipkart.phantom.thrift.impl.ThriftRequestWrapper;
import com.github.kristofa.brave.BraveContext;
import com.github.kristofa.brave.FixedSampleRateTraceFilter;
import com.github.kristofa.brave.ServerTracer;
import com.github.kristofa.brave.TraceFilter;
import com.google.common.base.Optional;

/**
 * <code>ThriftChannelHandler</code> is a sub-type of {@link SimpleChannelHandler} that acts as a proxy for Apache Thrift calls using the binary protocol.
 * It wraps the Thrift call using a {@link ThriftProxyExecutor} that provides useful features like monitoring, fallback etc.
 *
 * @author Regunath B
 * @version 1.0, 26 Mar 2013
 */
public class ThriftChannelHandler extends SimpleChannelUpstreamHandler {

    /** Logger for this class*/
    private static final Logger LOGGER = LoggerFactory.getLogger(ThriftChannelHandler.class);

    /** The default name of the server/service this channel handler is serving*/
    private static final String DEFAULT_SERVICE_NAME = "Thrift Proxy";
    
    /** Event Type for publishing all events which are generated here */
    private final static String THRIFT_HANDLER = "THRIFT_HANDLER";

    /** The default response size for creating dynamic channel buffers*/
    private static final int DEFAULT_RESPONSE_SIZE = 4096;

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
    
    /** The Thrift TaskRepository to lookup ThriftServiceProxyClient from */
    private ExecutorRepository<ThriftRequestWrapper, TTransport, ThriftProxy> repository;

    /** The ThriftHandler of this channel  */
    private String thriftProxy;

    /** The dynamic buffer response size*/
    private int responseSize = DEFAULT_RESPONSE_SIZE;

    /** The Thrift binary protocol factory*/
    private TProtocolFactory protocolFactory =  new TBinaryProtocol.Factory();

	/** The publisher used to broadcast events to Service Proxy Subscribers */
	private ServiceProxyEventProducer eventProducer;

    /** The request tracing frequency for this channel handler*/
    private TraceFilter traceFilter = NO_TRACING;	
    
    /** The EventDispatchingSpanCollector instance used in tracing requests*/
    private EventDispatchingSpanCollector eventDispatchingSpanCollector;    

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
     * Overriden superclass method. Adds the newly created Channel to the default channel group and calls the super class {@link #channelOpen(ChannelHandlerContext, ChannelStateEvent)} method
     * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#channelOpen(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
     */
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent event) throws Exception {
        super.channelOpen(ctx, event);
		this.defaultChannelGroup.add(event.getChannel());
    }

    /**
     * Overridden method. Reads and processes Thrift calls sent to the service proxy. Expects data in the Thrift binary protocol.
     * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
     */
    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent event) throws Exception {
        long receiveTime = System.currentTimeMillis();
        if (MessageEvent.class.isAssignableFrom(event.getClass())) {

            // Prepare input and output
            ChannelBuffer input = (ChannelBuffer) ((MessageEvent) event).getMessage();
            ChannelBuffer output = ChannelBuffers.dynamicBuffer(responseSize);
            TTransport clientTransport = new ThriftNettyChannelBuffer(input, output);

            //Get command name
            ThriftNettyChannelBuffer ttransport = new ThriftNettyChannelBuffer(input, null);
            TProtocol iprot = this.protocolFactory.getProtocol(ttransport);
            input.markReaderIndex();
            TMessage message = iprot.readMessageBegin();
            input.resetReaderIndex();

            ThriftRequestWrapper thriftRequestWrapper = new ThriftRequestWrapper();
            thriftRequestWrapper.setClientSocket(clientTransport);
            thriftRequestWrapper.setMethodName(message.name);
            // set the service name for the request
            thriftRequestWrapper.setServiceName(Optional.of(this.serviceName));

            // Create and process a Server request interceptor. This will initialize the server tracing
            ServerRequestInterceptor<ThriftRequestWrapper, TTransport> serverRequestInterceptor = this.initializeServerTracing(thriftRequestWrapper);

            //Execute
            Executor<ThriftRequestWrapper,TTransport> executor = this.repository.getExecutor(message.name, this.thriftProxy, thriftRequestWrapper);
            // set the service name for the request
            thriftRequestWrapper.setServiceName(executor.getServiceName());
            
            Optional<RuntimeException> transportError = Optional.absent();            
            try {
                executor.execute();
            } catch (Exception e) {
            	RuntimeException runtimeException = new RuntimeException("Error in executing Thrift request: " + thriftProxy + ":" + message.name, e);
            	transportError = Optional.of(runtimeException);
                throw runtimeException;
            } finally {
            	// finally inform the server request tracer
            	serverRequestInterceptor.process(clientTransport, transportError);            	
                if (eventProducer != null) {
                    // Publishes event both in case of success and failure.
                    ServiceProxyEvent.Builder eventBuilder;
                    if (executor == null) {
                        eventBuilder = new ServiceProxyEvent.Builder(thriftProxy + ":" + message.name, THRIFT_HANDLER).withEventSource(getClass().getName());
                    } else {
                        eventBuilder = executor.getEventBuilder().withCommandData(executor).withEventSource(executor.getClass().getName());
                    }
                    eventBuilder.withRequestReceiveTime(receiveTime);
                    eventProducer.publishEvent(eventBuilder.build());
                } else {
                    LOGGER.debug("eventProducer not set, not publishing event");
                }
            }
            // write the result to the output channel buffer
            Channels.write(ctx, event.getFuture(), ((ThriftNettyChannelBuffer) clientTransport).getOutputBuffer());
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
        super.exceptionCaught(ctx, event);
    }
    
    /**
     * Initializes server tracing for the specified request
     * @param executorHttpRequest the Http request 
     * @return the initialized ServerRequestInterceptor
     */
    private ServerRequestInterceptor<ThriftRequestWrapper, TTransport> initializeServerTracing(ThriftRequestWrapper executorRequest) {
		// set the server request context on the received request
    	BraveContext serverTracingContext = new BraveContext();
    	RequestContext serverRequestContext = new RequestContext();
    	serverRequestContext.setRequestTracingContext(serverTracingContext);		
    	executorRequest.setRequestContext(Optional.of(serverRequestContext));
        ServerRequestInterceptor<ThriftRequestWrapper, TTransport> serverRequestInterceptor = new ServerRequestInterceptor<ThriftRequestWrapper, TTransport>();
    	List<TraceFilter> traceFilters = Arrays.<TraceFilter>asList(this.traceFilter);    
    	ServerTracer serverTracer = serverTracingContext.getServerTracer(this.eventDispatchingSpanCollector, traceFilters);
    	serverRequestInterceptor.setEndPointSubmitter(serverTracingContext.getEndPointSubmitter());
        serverRequestInterceptor.setServerTracer(serverTracer);
        serverRequestInterceptor.setServiceHost(ThriftChannelHandler.hostName);
        serverRequestInterceptor.setServicePort(this.hostPort);
        serverRequestInterceptor.setServiceName(this.serviceName);   
        // now process the request to initialize tracing
        serverRequestInterceptor.process(executorRequest); 
        return serverRequestInterceptor;
    }
    
    /** Start Getter/Setter methods*/
	public ChannelGroup getDefaultChannelGroup() {
		return this.defaultChannelGroup;
	}
    public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}	
	public void setDefaultChannelGroup(ChannelGroup defaultChannelGroup) {
		this.defaultChannelGroup = defaultChannelGroup;
	}    
    public ExecutorRepository<ThriftRequestWrapper,TTransport, ThriftProxy> getRepository() {
        return this.repository;
    }
    public void setRepository(ExecutorRepository<ThriftRequestWrapper,TTransport, ThriftProxy> repository) {
        this.repository = repository;
    }
    public int getResponseSize() {
        return this.responseSize;
    }
    public void setResponseSize(int responseSize) {
        this.responseSize = responseSize;
    }
    public String getThriftProxy() {
        return thriftProxy;
    }
    public void setThriftProxy(String thriftProxy) {
        this.thriftProxy = thriftProxy;
    }
	public void setEventProducer(ServiceProxyEventProducer eventProducer) {
		this.eventProducer = eventProducer;
	}
	public void setTraceFilter(TraceFilter traceFilter) {
		this.traceFilter = traceFilter;
	}
	public void setEventDispatchingSpanCollector(EventDispatchingSpanCollector eventDispatchingSpanCollector) {
		this.eventDispatchingSpanCollector = eventDispatchingSpanCollector;
	}    
    /** End Getter/Setter methods */
}
