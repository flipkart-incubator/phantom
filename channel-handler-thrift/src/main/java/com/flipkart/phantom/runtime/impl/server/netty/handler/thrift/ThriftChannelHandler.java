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

import com.flipkart.phantom.event.ServiceProxyEventProducer;
import com.flipkart.phantom.runtime.impl.server.netty.channel.thrift.ThriftNettyChannelBuffer;
import com.flipkart.phantom.task.spi.Executor;
import com.flipkart.phantom.task.spi.repository.ExecutorRepository;
import com.flipkart.phantom.thrift.impl.ThriftProxyExecutor;
import com.flipkart.phantom.thrift.impl.ThriftRequestWrapper;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TTransport;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>ThriftChannelHandler</code> is a sub-type of {@link SimpleChannelHandler} that acts as a proxy for Apache Thrift calls using the binary protocol.
 * It wraps the Thrift call using a {@link ThriftProxyExecutor} that provides useful features like monitoring, fallback etc.
 *
 * @author Regunath B
 * @version 1.0, 26 Mar 2013
 */
public class ThriftChannelHandler extends SimpleChannelUpstreamHandler {

    /** The default response size for creating dynamic channel buffers*/
    private static final int DEFAULT_RESPONSE_SIZE = 4096;

    /** Logger for this class*/
    private static final Logger LOGGER = LoggerFactory.getLogger(ThriftChannelHandler.class);

    /** The Thrift TaskRepository to lookup ThriftServiceProxyClient from */
    private ExecutorRepository repository;

    /** The ThriftHandler of this channel  */
    private String thriftProxy;

    /** The dynamic buffer response size*/
    private int responseSize = DEFAULT_RESPONSE_SIZE;

    /** The Thrift binary protocol factory*/
    private TProtocolFactory protocolFactory =  new TBinaryProtocol.Factory();

	/** The publisher used to broadcast events to Service Proxy Subscribers */
	private ServiceProxyEventProducer eventProducer;

    /** Event Type for publishing all events which are generated here */
    private final static String THRIFT_HANDLER = "THRIFT_HANDLER";

    /**
     * Overriden superclass method. Adds the newly created Channel to the default channel group and calls the super class {@link #channelOpen(ChannelHandlerContext, ChannelStateEvent)} method
     * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#channelOpen(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
     */
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent event) throws Exception {
        super.channelOpen(ctx, event);
    }

    /**
     * Interface method implementation. Reads and processes Thrift calls sent to the service proxy. Expects data in the Thrift binary protocol.
     * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
     */
    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent event) throws Exception {
        if (MessageEvent.class.isAssignableFrom(event.getClass())) {

            // Prepare input and output
            ChannelBuffer input = (ChannelBuffer) ((MessageEvent) event).getMessage();
			ChannelBuffer output = ChannelBuffers.dynamicBuffer(responseSize);
			TTransport clientTransport = new ThriftNettyChannelBuffer(input, output);

            //Get command name
            ThriftNettyChannelBuffer ttransport = new ThriftNettyChannelBuffer(input,null);
            TProtocol iprot = this.protocolFactory.getProtocol(ttransport);
            input.markReaderIndex();
            TMessage message = iprot.readMessageBegin();
            input.resetReaderIndex();

            ThriftRequestWrapper thriftRequestWrapper = new ThriftRequestWrapper();

            thriftRequestWrapper.setClientSocket(clientTransport);

            //Execute
            Executor executor = this.repository.getExecutor(message.name, this.thriftProxy, thriftRequestWrapper);
            try {
                executor.execute();
            } catch (Exception e) {
                throw new RuntimeException("Error in executing Thrift request: " + thriftProxy + ":" + message.name, e);
            } finally {
	            // Publishes event both in case of success and failure.
	            Class eventSource = (executor == null) ? this.getClass() : executor.getClass();
                if (eventProducer != null)
                    eventProducer.publishEvent(executor, message.name, eventSource, THRIFT_HANDLER);
                else
                    LOGGER.debug("eventProducer not set, not publishing event");
            }
            // write the result to the output channel buffer
			Channels.write(ctx, event.getFuture(), ((ThriftNettyChannelBuffer)clientTransport).getOutputBuffer());
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
    public ExecutorRepository getRepository() {
        return this.repository;
    }
    public void setRepository(ExecutorRepository repository) {
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
    /** End Getter/Setter methods */
}
