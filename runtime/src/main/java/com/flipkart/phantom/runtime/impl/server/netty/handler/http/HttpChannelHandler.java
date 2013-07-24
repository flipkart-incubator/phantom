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
package com.flipkart.phantom.runtime.impl.server.netty.handler.http;

import com.flipkart.phantom.http.impl.HttpProxyExecutor;
import com.flipkart.phantom.http.impl.HttpProxyExecutorRepository;
import com.flipkart.phantom.http.spi.HttpProxy;
import com.flipkart.phantom.task.spi.TaskHandler;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.jboss.netty.handler.codec.http.HttpMethod;

/**
 * <code>HttpChannelHandler</code> is a sub-type of {@link SimpleChannelHandler} that implements a Http proxy.
 *
 * @author Regunath B
 * @version 1.0, 3 Apr 2013
 */

public class HttpChannelHandler extends SimpleChannelUpstreamHandler {

    /** Constant String literals in the Http protocol */
    private static final String LINE_FEED = "\n";
    private static final String ENCODING_HEADER = "Transfer-Encoding";

	/** Logger for this class*/
	private static final Logger LOGGER = LoggerFactory.getLogger(HttpChannelHandler.class);
	
	/** The default channel group*/
	private ChannelGroup defaultChannelGroup;
	
	/** The TaskRepository to lookup TaskHandlerExecutors from */
	private HttpProxyExecutorRepository repository;

    /** The HTTP proxy handler */
    private HttpProxy httpProxy;

	/**
	 * Overriden superclass method. Adds the newly created Channel to the default channel group and calls the super class {@link #channelOpen(ChannelHandlerContext, ChannelStateEvent)} method
	 * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#channelOpen(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
	 */
	public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent event) throws Exception {
		super.channelOpen(ctx, event);
		this.defaultChannelGroup.add(event.getChannel());
    }

	/**
	 * Interface method implementation. Reads and processes Http commands sent to the service proxy. Expects data in the Http protocol.
	 * Discards commands that do not have a {@link TaskHandler} mapping. The task handler look up happens using the Http header property {@value #COMMAND_HTTP_HEADER}
	 * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent event) throws Exception {
        LOGGER.debug("Handling upstream request");
		if (MessageEvent.class.isAssignableFrom(event.getClass())) {
            LOGGER.debug("Inside first");
			MessageEvent messageEvent = (MessageEvent)event;
            if (HttpRequest.class.isAssignableFrom(messageEvent.getMessage().getClass())) {
                LOGGER.debug("Inside second");
                HttpRequest request = (HttpRequest) messageEvent.getMessage();
                LOGGER.debug("Request is: " + request.getMethod() + " " + request.getUri());
                HttpProxyExecutor executor = this.repository.getHttpProxyExecutor(this.httpProxy,convertRequest(request));
                HttpResponse response = executor.execute();
                writeCommandExecutionResponse(ctx,event,response);
            } else {
                LOGGER.debug("Failed second");
            }
		} else {
            LOGGER.debug("Failed first");
        }
	    super.handleUpstream(ctx, event);
	}

    private HttpRequestBase convertRequest(HttpRequest request) throws Exception {

        // get data if any
        ChannelBuffer inputBuffer = request.getContent();
        byte[] requestData = new byte[inputBuffer.readableBytes()];
        inputBuffer.readBytes(requestData, 0, requestData.length);

        // get
        if (HttpMethod.GET.equals(request.getMethod())) {
            HttpGet r = new HttpGet(request.getUri());
            return r;

        // put
        } else if (HttpMethod.PUT.equals(request.getMethod())) {
            HttpPut r = new HttpPut(request.getUri());
            r.setEntity(new ByteArrayEntity(requestData));
            return r;

        // post
        } else if (HttpMethod.POST.equals(request.getMethod())) {
            HttpPost r = new HttpPost(request.getUri());
            r.setEntity(new ByteArrayEntity(requestData));
            return r;

        // delete
        } else if (HttpMethod.DELETE.equals(request.getMethod())) {
            HttpDelete r = new HttpDelete(request.getUri());
            return r;

        // invalid
        } else {
            return null;
        }
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
     * Writes the specified TaskResult data to the channel output. Only the raw output data is written and rest of the TaskResult fields are ignored 
     * @param ctx the ChannelHandlerContext
     * @param event the ChannelEvent
     * @param result the TaskResult data written to the channel response
     * @throws Exception in case of any errors
     */
    private void writeCommandExecutionResponse(ChannelHandlerContext ctx, ChannelEvent event, HttpResponse response) throws Exception {

        // Don't write anything if the response is null
        if (response == null) {
            return;
        }

        // create buffer
        StringBuffer buffer = new StringBuffer();

        // write status
        buffer.append(response.getStatusLine().toString());
        buffer.append(LINE_FEED);

        // write headers
        for (Header header : response.getAllHeaders()) {
            if (!header.toString().contains(ENCODING_HEADER)) { // skip the encoding header as some Http services use chunked encoding but we have received all data
                buffer.append(header.toString());
                buffer.append(LINE_FEED);
            }
        }
        buffer.append(LINE_FEED);

        // write entity
        HttpEntity responseEntity = response.getEntity();
        byte[] responseData = EntityUtils.toByteArray(responseEntity);
        buffer.append(new String(responseData));

        // write response
        ChannelFuture future = event.getChannel().write(buffer);
        future.addListener(ChannelFutureListener.CLOSE);  // explicitly close the Channel as Http client (say browser) would not initiate it by itself

    }
    
	/** Start Getter/Setter methods */
	public ChannelGroup getDefaultChannelGroup() {
		return this.defaultChannelGroup;
	}
	public void setDefaultChannelGroup(ChannelGroup defaultChannelGroup) {
		this.defaultChannelGroup = defaultChannelGroup;
	}
	public HttpProxyExecutorRepository getRepository() {
		return this.repository;
	}
	public void setRepository(HttpProxyExecutorRepository repository) {
		this.repository = repository;
	}
    public HttpProxy getHttpProxy() {
        return httpProxy;
    }
    public void setHttpProxy(HttpProxy httpProxy) {
        this.httpProxy = httpProxy;
    }
	/** End Getter/Setter methods */

}
