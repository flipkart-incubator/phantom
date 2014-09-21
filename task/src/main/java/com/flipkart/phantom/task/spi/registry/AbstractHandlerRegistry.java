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

package com.flipkart.phantom.task.spi.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.trpr.platform.core.PlatformException;
import org.trpr.platform.core.impl.logging.LogFactory;
import org.trpr.platform.core.spi.logging.Logger;

import com.flipkart.phantom.task.spi.AbstractHandler;
import com.flipkart.phantom.task.spi.TaskContext;

/**
 * Interface for handler registry. Controls lifecycle methods of all handlers understood by the registry.
 *
 * @author kartikbu
 * @version 1.0
 * @created 30/7/13 12:43 AM
 */
public abstract class AbstractHandlerRegistry<T extends AbstractHandler> {

	/** Logger for this class*/
	private static final Logger LOGGER = LogFactory.getLogger(AbstractHandlerRegistry.class);
	
	/** Default value for handler init concurrency */
	private static final int DEFAULT_HANDLER_INIT_CONCURRENCY = 5;
	
	/** The handler init concurrency */
	private int handlerInitConcurrency = AbstractHandlerRegistry.DEFAULT_HANDLER_INIT_CONCURRENCY;
	
    /** List of AbstractHandlerS */
    protected Map<String,T> handlers = new HashMap<String,T>();
	
    /**
     * Lifecycle init method. Initializes all individual handlers understood.
     * @param handlerConfigInfoList List of HandlerConfigInfo which is to be analyzed and initialized
     * @param taskContext The task context object
     * @return array of AbstractHandlerRegistry.InitedHandlerInfo instances for each inited handler
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
	public AbstractHandlerRegistry.InitedHandlerInfo<T>[] init(List<HandlerConfigInfo> handlerConfigInfoList, TaskContext taskContext) throws Exception {
    	List<AbstractHandlerRegistry.InitedHandlerInfo<T>> initedHandlerInfos = new LinkedList<AbstractHandlerRegistry.InitedHandlerInfo<T>>();
    	for (HandlerConfigInfo handlerConfigInfo : handlerConfigInfoList) {
	        String[] handlerBeanIds = handlerConfigInfo.getProxyHandlerContext().getBeanNamesForType(this.getHandlerType());
	        for (String taskHandlerBeanId : handlerBeanIds) {
	        	T handler = (T) handlerConfigInfo.getProxyHandlerContext().getBean(taskHandlerBeanId);
	            try {
	            	if (!handler.isActive()) { // init the handler only if not inited already
		                LOGGER.info("Initializing {} : " + handler.getName(), getHandlerType().getName());
		                handler.init(taskContext);
			            // call post init for any registry specific handling
			            postInitHandler(handler);
		                handler.activate();
		                initedHandlerInfos.add(new AbstractHandlerRegistry.InitedHandlerInfo<T>(handler,handlerConfigInfo));                    	            			
			            // put in all handlers map
			            handlers.put(handler.getName(),handler);
	            	}
	            } catch (Exception e) {
	                LOGGER.error("Error initializing " + getHandlerType().getName() + " : {}. Error is: " + e.getMessage(), handler.getName(), e);
	            	if (handler.getInitOutcomeStatus() == AbstractHandler.VETO_INIT) {        	            	
	                    throw new PlatformException("Error initializing vetoing handler " + getHandlerType().getName() + " : " + handler.getName());
	            	} else {
	            		LOGGER.warn("Continuing after init failed for non-vetoing handler " + getHandlerType().getName() + " : " + handler.getName());
	            	}
	            }
	        }
	    }
    	return initedHandlerInfos.toArray(new AbstractHandlerRegistry.InitedHandlerInfo[0]);        
    }

    /**
     * Method to reinitialize a handler.
     * @param name Name of the handler.
     * @param taskContext task context object
     * @throws Exception
     */
    public void reinitHandler(String name, TaskContext taskContext) throws Exception {
        T handler = this.handlers.get(name);
        if (handler != null) {
            try {
                handler.deactivate();
                handler.shutdown(taskContext);
                handler.init(taskContext);
                handler.activate();
            } catch (Exception e) {
                LOGGER.error("Error initializing " + this.getHandlerType().getName() + " : {}. Error is: " + e.getMessage(), handler.getName(), e);
                throw new PlatformException("Error reinitialising "  + this.getHandlerType().getName() + " : " + handler.getName(), e);
            }
        }    	
    }

    /**
     * Lifecycle shutdown method. Shuts down all individual handlers understood.
     * @param taskContext The task context object
     * @throws Exception
     */
    public void shutdown(TaskContext taskContext) throws Exception {
        for (String name : this.handlers.keySet()) {
            LOGGER.info("Shutting down {}: " + name, this.getHandlerType().getName());
            try {
            	this.handlers.get(name).shutdown(taskContext);
            	this.handlers.get(name).deactivate();
            } catch (Exception e) {
                LOGGER.warn("Failed to shutdown {}: " + name, this.getHandlerType().getName(), e);
            }
        }    	
    }

    /**
     * Enumeration method for all handlers. Returns a List of AbstractHandler instances
     * @return List
     */
    public List<T> getHandlers() {
        return new ArrayList<T>(this.handlers.values());
    }

    /**
     * Get a handler given name
     * @param name String name of the handler
     * @return AbstractHandler
     */
    public T getHandler(String name) {
        return this.handlers.get(name);
    }
    
	/**
	 * Unregisters (removes) a AbstractHandler from this registry.
	 * @param taskHandler the AbstractHandler to be removed
	 */
    public void unregisterTaskHandler(T handler) {
    	this.handlers.remove(handler.getName());
    	this.postUnregisterHandler(handler);
    };
    
    /**
     * Returns the {@link AbstractHandler} type that this registry manages 
     * @return the AbstractHandler type
     */
    protected abstract Class<T> getHandlerType();
    
    /**
     * Callback method after initing handler. Subtypes may override to perform custom post init operations
     * @param handler the AbstractHandler that was inited
     */
    protected void postInitHandler(T handler) {
    	// no op
    }
    
    /**
     * Callback method after unregistering handler. Subtypes may override to perform custom post unregister operations
     * @param handler the AbstractHandler that was unregistered
     */
    protected void postUnregisterHandler(T handler) {
    	// no op    	
    }
    
    /**
     *  Container object for inited handlers and the respective configuration
     */
    public static final class InitedHandlerInfo<T extends AbstractHandler> {
    	private T initedHandler;
    	private HandlerConfigInfo handlerConfigInfo;
    	public InitedHandlerInfo(T initedHandler, HandlerConfigInfo handlerConfigInfo) {
    		this.initedHandler = initedHandler;
    		this.handlerConfigInfo = handlerConfigInfo;
    	}
		public T getInitedHandler() {
			return initedHandler;
		}
		public HandlerConfigInfo getHandlerConfigInfo() {
			return handlerConfigInfo;
		}
    }
    
    /** Getter/Setter methods */
	public int getHandlerInitConcurrency() {
		return handlerInitConcurrency;
	}
	public void setHandlerInitConcurrency(int handlerInitConcurrency) {
		this.handlerInitConcurrency = handlerInitConcurrency;
	}

}
