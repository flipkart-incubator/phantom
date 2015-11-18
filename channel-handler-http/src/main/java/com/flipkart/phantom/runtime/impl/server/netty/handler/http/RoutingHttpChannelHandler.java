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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import com.flipkart.phantom.event.ServiceProxyEvent;
import com.flipkart.phantom.event.ServiceProxyEventProducer;
import com.flipkart.phantom.http.impl.HttpProxy;
import com.flipkart.phantom.http.impl.HttpRequestWrapper;
import com.flipkart.phantom.task.impl.collector.EventDispatchingSpanCollector;
import com.flipkart.phantom.task.impl.interceptor.ServerRequestInterceptor;
import com.flipkart.phantom.task.spi.Executor;
import com.flipkart.phantom.task.spi.RequestContext;
import com.flipkart.phantom.task.spi.repository.ExecutorRepository;
import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.FixedSampleRateTraceFilter;
import com.github.kristofa.brave.ServerSpan;
import com.github.kristofa.brave.ServerTracer;
import com.github.kristofa.brave.TraceFilter;
import com.google.common.base.Optional;

/**
 * <code>RoutingHttpChannelHandler</code> is a sub-type of {@link SimpleChannelHandler} that routes Http requests to one or more {@link HttpProxy} instances.
 *
 * @author Regunath B
 * @version 1.0, 6 Sep 2013
 */

public abstract class RoutingHttpChannelHandler extends SimpleChannelUpstreamHandler implements InitializingBean {

    /** Logger for this class*/
    private static final Logger LOGGER = LoggerFactory.getLogger(RoutingHttpChannelHandler.class);
    
    /** The default name of the server/service this channel handler is serving*/
    private static final String DEFAULT_SERVICE_NAME = "Http Proxy";

    /** The empty routing key which is default*/
    public static final String ALL_ROUTES = "";

    /** Set of Http headers that we want to remove */
    public static final Set<String> REMOVE_HEADERS = new HashSet<String>();
    static {
    	RoutingHttpChannelHandler.REMOVE_HEADERS.add(HTTP.TRANSFER_ENCODING);    	
    	RoutingHttpChannelHandler.REMOVE_HEADERS.add(HTTP.CONN_DIRECTIVE);
    	RoutingHttpChannelHandler.REMOVE_HEADERS.add(HTTP.CONN_KEEP_ALIVE);
    	RoutingHttpChannelHandler.REMOVE_HEADERS.add(HTTP.TARGET_HOST);
    	RoutingHttpChannelHandler.REMOVE_HEADERS.add("Proxy-Authenticate");
    	RoutingHttpChannelHandler.REMOVE_HEADERS.add("TE");
    	RoutingHttpChannelHandler.REMOVE_HEADERS.add("Trailers");
    	RoutingHttpChannelHandler.REMOVE_HEADERS.add("Upgrade");
    }

    /** Event Type for publishing all events which are generated here */
    private final static String HTTP_HANDLER = "HTTP_HANDLER";
    
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

    /** The HttpProxyRepository to lookup HttpProxy from */
    private ExecutorRepository<HttpRequestWrapper,HttpResponse, HttpProxy> repository;

    /** The HTTP proxy handler map*/
    private Map<String, String> proxyMap = new HashMap<String, String>();

    /** The default HTTP proxy handler */
    private String defaultProxy;

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
        Assert.notNull(this.defaultProxy, "The 'defaultProxy' may not be null");
        // add the default proxy for all routes i.e. default
        this.proxyMap.put(RoutingHttpChannelHandler.ALL_ROUTES, defaultProxy);
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
     * Overriden superclass method. Adds the newly created Channel to the default channel group and calls the super class {@link #channelOpen(ChannelHandlerContext, ChannelStateEvent)} method
     * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#channelOpen(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
     */
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent event) throws Exception {
        super.channelOpen(ctx, event);
		this.defaultChannelGroup.add(event.getChannel());
    }

    /**
     * Overridden method. Reads and processes Http commands sent to the service proxy. Expects data in the Http protocol.
     * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#messageReceived(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
     */
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent messageEvent) throws Exception {
        long receiveTime = System.currentTimeMillis();
        HttpRequest request = (HttpRequest) messageEvent.getMessage();
        
        if (LOGGER.isDebugEnabled()) {
	        LOGGER.debug("Http Request is: " + request.getMethod() + " " + request.getUri());
	        LOGGER.debug("Http Headers : " + request.getHeaders().toString());
        }

        this.processRequestHeaders(request);

        ChannelBuffer inputBuffer = request.getContent();
        byte[] requestData = new byte[inputBuffer.readableBytes()];
        inputBuffer.readBytes(requestData, 0, requestData.length);

        // Prepare request Wrapper
        HttpRequestWrapper executorHttpRequest = new HttpRequestWrapper();
        executorHttpRequest.setData(requestData);
        executorHttpRequest.setMethod(request.getMethod().toString());
        executorHttpRequest.setUri(request.getUri());
        executorHttpRequest.setHeaders(request.getHeaders());
        executorHttpRequest.setProtocol(request.getProtocolVersion().getProtocolName());
        executorHttpRequest.setMajorVersion(request.getProtocolVersion().getMajorVersion());
        executorHttpRequest.setMinorVersion(request.getProtocolVersion().getMinorVersion());
        // set the service name for the request
        executorHttpRequest.setServiceName(Optional.of(this.serviceName));

        // Create and process a Server request interceptor. This will initialize the server tracing
        ServerRequestInterceptor<HttpRequestWrapper, HttpResponse> serverRequestInterceptor = this.initializeServerTracing(executorHttpRequest);
        
        // executor
        String proxy = this.proxyMap.get(this.getRoutingKey(request));
        if (proxy == null) {
            proxy = this.proxyMap.get(RoutingHttpChannelHandler.ALL_ROUTES);
            LOGGER.info("Routing key for : " + request.getUri() + " returned null. Using default proxy instead.");
        }
        Executor<HttpRequestWrapper,HttpResponse> executor = this.repository.getExecutor(proxy, proxy, executorHttpRequest);
        
        // execute
        HttpResponse response = null;
        Optional<RuntimeException> transportError = Optional.absent();
        try {
            response = (HttpResponse) executor.execute();
        } catch (Exception e) {
        	RuntimeException runtimeException = new RuntimeException("Error in executing HTTP request:" + proxy + " URI:" + request.getUri(), e);
        	transportError = Optional.of(runtimeException);
            throw runtimeException;
        } finally {
        	// finally inform the server request tracer
        	serverRequestInterceptor.process(response, transportError);
            if (eventProducer != null) {
                // Publishes event both in case of success and failure.
                ServiceProxyEvent.Builder eventBuilder;
                if (executor == null) {
                    eventBuilder = new ServiceProxyEvent.Builder(request.getUri(), HTTP_HANDLER).withEventSource(getClass().getName());
                } else {
                    eventBuilder = executor.getEventBuilder().withCommandData(executor).withEventSource(executor.getClass().getName());
                }
                eventBuilder.withRequestReceiveTime(receiveTime);
                eventProducer.publishEvent(eventBuilder.build());
            } else {
                LOGGER.debug("eventProducer not set, not publishing event");
            }
        }
        // send response
        writeCommandExecutionResponse(ctx, messageEvent, request, response);
    }

    /**
     * Interface method implementation. Closes the underlying channel after logging a warning message
     * @see org.jboss.netty.channel.SimpleChannelHandler#exceptionCaught(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ExceptionEvent)
     */
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent event) throws Exception {
        LOGGER.error("Exception thrown on Channel. Disconnect initiated : " + event.getCause(), event.getCause());
        event.getChannel().close();
    }

    /**
     * Returns the routing key to use for proxy selection. Sub-types may use the passed-in request data attributes to determine routing
	 * @param request the HttpRequest object
     * @return the routing key to identify the proxy
     */
    protected abstract String getRoutingKey(HttpRequest request);

    /**
     * Helper method to remove or otherwise modify Http request headers that we dont want to propagate. This implementation removes all headers specified
     * under {@link RoutingHttpChannelHandler#REMOVE_HEADERS}. Sub-types may override this method to change this behavior
     * @param request the HttpRequest that needs to be processed for remove headers 
     */
    protected void processRequestHeaders(HttpRequest request) {
        for (String header : RoutingHttpChannelHandler.REMOVE_HEADERS) {
        	request.removeHeader(header);
        }    	
    }
    
    /**
     * Initializes server tracing for the specified request
     * @param executorHttpRequest the Http request 
     * @return the initialized ServerRequestInterceptor
     */
    private ServerRequestInterceptor<HttpRequestWrapper, HttpResponse> initializeServerTracing(HttpRequestWrapper executorRequest) {
        ServerRequestInterceptor<HttpRequestWrapper, HttpResponse> serverRequestInterceptor = new ServerRequestInterceptor<HttpRequestWrapper, HttpResponse>();
    	List<TraceFilter> traceFilters = Arrays.<TraceFilter>asList(this.traceFilter);    
    	ServerTracer serverTracer = Brave.getServerTracer(this.eventDispatchingSpanCollector, traceFilters);
    	serverRequestInterceptor.setEndPointSubmitter(Brave.getEndPointSubmitter());
        serverRequestInterceptor.setServerTracer(serverTracer);
        serverRequestInterceptor.setServiceHost(RoutingHttpChannelHandler.hostName);
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

    /**
     * Writes the specified TaskResult data to the channel output. Only the raw output data is written and rest of the TaskResult fields are ignored 
     * @param ctx the ChannelHandlerContext
     * @param event the ChannelEvent
     * @throws Exception in case of any errors
     */
    private void writeCommandExecutionResponse(ChannelHandlerContext ctx, ChannelEvent event, HttpRequest request, HttpResponse response) throws Exception {
        // Don't write anything if the response is null
        if (response == null || response.getEntity() == null) {
            // write empty response
            event.getChannel().write(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT)).addListener(ChannelFutureListener.CLOSE);
            return;
        }
        org.jboss.netty.handler.codec.http.HttpResponse httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(response.getStatusLine().getStatusCode()));
        // write headers
        for (Header header : response.getAllHeaders()) {
            if (!RoutingHttpChannelHandler.REMOVE_HEADERS.contains(header.getName())) {
                httpResponse.setHeader(header.getName(),header.getValue());
            }
        }
                
        // write entity
        HttpEntity responseEntity = response.getEntity();
        byte[] responseData = EntityUtils.toByteArray(responseEntity);
        
        // add the content length response header since we send the complete response body
        httpResponse.setHeader(HTTP.CONTENT_LEN,responseData.length);        

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Http Response status : " + response.getStatusLine().toString());
        	LOGGER.debug("Http Response : " + new String(responseData));
        }
        
        httpResponse.setContent(ChannelBuffers.copiedBuffer(responseData));
        // write response
        boolean keepAlive = HttpHeaders.isKeepAlive(request);
        ChannelFuture channelFuture = event.getChannel().write(httpResponse);
        if (!keepAlive) { // close the channel only if the client has requested
        	channelFuture.addListener(ChannelFutureListener.CLOSE);
        }
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
    public ExecutorRepository<HttpRequestWrapper,HttpResponse, HttpProxy> getRepository() {
        return this.repository;
    }
    public void setRepository(ExecutorRepository<HttpRequestWrapper,HttpResponse, HttpProxy> repository) {
        this.repository = repository;
    }
    public Map<String, String> getProxyMap() {
        return this.proxyMap;
    }
    public void setProxyMap(Map<String, String> proxyMap) {
        this.proxyMap = proxyMap;
    }
    public String getDefaultProxy() {
        return this.defaultProxy;
    }
    public void setDefaultProxy(String defaultProxy) {
        this.defaultProxy = defaultProxy;
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
