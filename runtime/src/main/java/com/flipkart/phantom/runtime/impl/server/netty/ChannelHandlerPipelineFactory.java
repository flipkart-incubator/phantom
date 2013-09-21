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
package com.flipkart.phantom.runtime.impl.server.netty;

import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <code>ChannelHandlerPipelineFactory</code> is an implementation of the {@link ChannelPipelineFactory} that creates a default channel pipeline.
 * Allows setting up a chain of channel handlers configured on this factory. Additionally sets up handlers to process idle state on the channel, if specified.
 * This factory also implements the Spring {@link ApplicationContextAware} interface so that it may create new instances for handlers for every call to 
 * {@link ChannelPipelineFactory#getPipeline()}
 * 
 * @author Regunath B
 * @version 1.0, 15 Mar 2013
 */

public class ChannelHandlerPipelineFactory implements ChannelPipelineFactory, ApplicationContextAware {
	
	/** The default idle check time in milliseconds*/
	private static final long DFFAULT_IDLE_TIME_MILLIS = 200;
	
	/** The channel idle time*/
	private long channelIdleTimeMillis = DFFAULT_IDLE_TIME_MILLIS;
	
	/** Logger for this class*/
	private static final Logger LOGGER = LoggerFactory.getLogger(ChannelHandlerPipelineFactory.class);
	
	/** The Timer for idle time checking*/ // this is set using Spring DI. The timer is however stopped when #close() is called to ensure that it exits
	private Timer timer;
	
	/** The IdleStateAwareChannelHandler bean name to handle channel timeouts*/
	private String idleStateAwareChannelHandlerBean;
	
	/** The ApplicationContext instance for instantiating ChannelHandlers*/
	private ApplicationContext applicationContext;
	
	/** Map of channel handler names and bean names to add to the pipeline*/
	private Map<String, String> channelHandlerBeanNamesMap = new HashMap<String, String>();

	/**
	 * Interface call back method. Stores the passed in ApplicationContext for ChannelHandler instantiation in {@link #getPipeline()}
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
	
	/**
	 * Interface implementation. Creates a default channel pipeline and sets up timeout handlers, if specified 
	 * @see org.jboss.netty.channel.ChannelPipelineFactory#getPipeline()
	 */
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline channelPipeline = Channels.pipeline();
		if (this.getIdleStateAwareChannelHandlerBean()!= null) {
			channelPipeline.addLast("idleStateCheck", new IdleStateHandler(timer, 0L, 0L, this.getChannelIdleTimeMillis(),TimeUnit.MILLISECONDS));
			channelPipeline.addLast("idleCheckHandler", (ChannelHandler)this.applicationContext.getBean(this.getIdleStateAwareChannelHandlerBean()));
		}
		for (String handlerKey : this.getChannelHandlerBeanNamesMap().keySet()) {
			channelPipeline.addLast(handlerKey, (ChannelHandler)this.applicationContext.getBean(this.getChannelHandlerBeanNamesMap().get(handlerKey)));
		}
		return channelPipeline;
	}
	
	/**
	 * Closes this ChannelPipelineFactory and releases all external resources
	 */
	public void close() {
		if (this.getTimer() != null) {
			LOGGER.debug("Closing ChannelPipelineFactory : {}", this.getClass().getName());
			this.getTimer().stop(); // stop the Timer here explicitly
		}
	}
	
	/**
	 * Returns a Map containing ChannelHandler instances
	 * @return Map containing ChannelHandler instances keyed by the handler bean Ids
	 */
	public Map<String,ChannelHandler> getChannelHandlersMap() {
		Map<String,ChannelHandler> channelHandlersMap = new HashMap<String,ChannelHandler>();
		for (String handlerKey : this.getChannelHandlerBeanNamesMap().keySet()) {
			channelHandlersMap.put(handlerKey, (ChannelHandler)this.applicationContext.getBean(this.getChannelHandlerBeanNamesMap().get(handlerKey)));
		}		
		return channelHandlersMap;
	}
	
	/**
	 * Returns a string containing bean names of ChannelHandlers in the ChannelPipelin created by this pipeline factory
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer("Registered Channel Handlers[");
		if (this.getIdleStateAwareChannelHandlerBean()!= null) {
			buffer.append("idleStateCheck,");
			buffer.append("idleCheckHandler,");
		}
		for (String handlerKey : this.getChannelHandlerBeanNamesMap().keySet()) {
			buffer.append(handlerKey + ",");
		}
		buffer.append("]");
		return buffer.toString();
	}
	
	/** Start Getter/Setter methods */
	public long getChannelIdleTimeMillis() {
		return this.channelIdleTimeMillis;
	}
	public void setChannelIdleTimeMillis(long channelIdleTimeMillis) {
		this.channelIdleTimeMillis = channelIdleTimeMillis;
	}	
	public Timer getTimer() {
		return this.timer;
	}
	public void setTimer(Timer timer) {
		this.timer = timer;
	}	
	public String getIdleStateAwareChannelHandlerBean() {
		return this.idleStateAwareChannelHandlerBean;
	}
	public void setIdleStateAwareChannelHandlerBean(String idleStateAwareChannelHandlerBean) {
		this.idleStateAwareChannelHandlerBean = idleStateAwareChannelHandlerBean;
	}
	public Map<String, String> getChannelHandlerBeanNamesMap() {
		return this.channelHandlerBeanNamesMap;
	}
	public void setChannelHandlerBeanNamesMap(Map<String, String> channelHandlerBeanNamesMap) {
		this.channelHandlerBeanNamesMap = channelHandlerBeanNamesMap;
	}	
	/** End Getter/Setter methods */

}
