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

package com.flipkart.phantom.task.spi.repository;

import com.flipkart.phantom.task.spi.Executor;
import com.flipkart.phantom.task.spi.RequestWrapper;
import com.flipkart.phantom.task.spi.TaskContext;
import com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry;

/**
 *  <code>ExecutorRepository</code> is a general purpose repository  which provides the {@link Executor} object and
 *  makes sure that classes implementing <code>ExecutorRepository</code> provides setters and getters of
 *  {@link AbstractHandlerRegistry} and {@link TaskContext}
 * This interface provides a way of decoupling task submission from the mechanics
 * of how each task will be run
 *
 * @author : arya.ketan
 * @version : 1.0
 * @date : 28/10/13
 */
public interface ExecutorRepository<T>{

    /**
     *  Getter for the registry holding the names of the Handlers
     * @return   {@link com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry}
     */
    public AbstractHandlerRegistry getRegistry();

    /**
     *  Setter for the  registry holding the names of the TaskHandler
     * @param  registry
     */
    public void setRegistry(AbstractHandlerRegistry registry);

    /**
     *  Getter for the <code>TaskContext</code>, that provides methods
     *  for {@link com.flipkart.phantom.task.impl.TaskHandler} to communicate with it's Component Container.
     *
     * @return {@link com.flipkart.phantom.task.spi.TaskContext}
     */
    public TaskContext getTaskContext();

    /**
     *  Setter for the <code>TaskContext</code>, that provides methods
     *  for {@link com.flipkart.phantom.task.impl.TaskHandler} to communicate with it's Component Container.
     *
     * @param taskContext
     */
    public void setTaskContext(TaskContext taskContext);


    /**
     * This method provides the <code>Executor</code> class after instantiating it from the command,proxy and requestWrapper.
     *
     * @param commandName the command name/String for which the Executor is needed
     * @param proxyName the name of the proxy for which command has to be processed
     * @param requestWrapper  the requestWrapper passed to the executor which process it to get the response
     * @return  {@link Executor}
     */
    public Executor<T> getExecutor(String commandName, String proxyName, RequestWrapper requestWrapper);

}
