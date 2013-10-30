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

import com.flipkart.phantom.task.spi.AbstractHandler;
import com.flipkart.phantom.task.spi.TaskContext;

import java.util.List;

/**
 * Interface for handler registry. Controls lifecycle methods of all handlers understood by the registry.
 *
 * @author kartikbu
 * @version 1.0
 * @created 30/7/13 12:43 AM
 */
public interface AbstractHandlerRegistry {

    /**
     * Lifecycle init method. This should initialize all individual handlers understood.
     * @param handlerConfigInfoList List of HandlerConfigInfo which is to be analysed and initialize
     * @param taskContext The task context object
     * @return array of AbstractHandlerRegistry.InitedHandlerInfo instances for each inited handler
     * @throws Exception
     */
    public AbstractHandlerRegistry.InitedHandlerInfo[] init(List<HandlerConfigInfo> handlerConfigInfoList, TaskContext taskContext) throws Exception;

    /**
     * Method to reinitialize a handler.
     * @param name Name of the handler.
     * @param taskContext task context object
     * @throws Exception
     */
    public void reinitHandler(String name, TaskContext taskContext) throws Exception;

    /**
     * Lifecycle shutdown method. This should shutdown all individual handlers understood.
     * @param taskContext The task context object
     * @throws Exception
     */
    public void shutdown(TaskContext taskContext) throws Exception;

    /**
     * Enumeration method for all handlers. This should returns a List of AbstractHandler instances
     * @return List
     */
    public List<AbstractHandler> getHandlers();

    /**
     * Get a handler given name
     * @param name String name of the handler
     * @return AbstractHandler
     */
    public AbstractHandler getHandler(String name);
    
	/**
	 * Unregisters (removes) a AbstractHandler from this registry.
	 * @param taskHandler the AbstractHandler to be removed
	 */
    public void unregisterTaskHandler(AbstractHandler taskHandler);
    
    /**
     *  Container object for inited handlers and the respective configuration
     */
    public static final class InitedHandlerInfo {
    	private AbstractHandler initedHandler;
    	private HandlerConfigInfo handlerConfigInfo;
    	public InitedHandlerInfo(AbstractHandler initedHandler, HandlerConfigInfo handlerConfigInfo) {
    		this.initedHandler = initedHandler;
    		this.handlerConfigInfo = handlerConfigInfo;
    	}
		public AbstractHandler getInitedHandler() {
			return initedHandler;
		}
		public HandlerConfigInfo getHandlerConfigInfo() {
			return handlerConfigInfo;
		}
    }

}
