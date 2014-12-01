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

package com.flipkart.phantom.task.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.phantom.task.spi.Decoder;
import com.flipkart.phantom.task.spi.RequestContext;
import com.flipkart.phantom.task.spi.TaskContext;
import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.ServerSpan;
import com.google.common.base.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Default implementation of {@link TaskContext}
 *
 * @author devashishshankar
 * @version 1.0, 20th March, 2013
 */
@SuppressWarnings("rawtypes")
public class TaskContextImpl implements TaskContext {

    /** Logger for this class */
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskContextImpl.class);

    /** The default command to get Config  */
    private static final String GET_CONFIG_COMMAND = "getConfig";

    /** Host name for this TaskContext */
    private static String hostName;

    /** ObjectMapper instance */
    private ObjectMapper objectMapper = new ObjectMapper();

    /** The TaskHandlerExecutorRepository instance for getting thrift handler executor instances */
    private TaskHandlerExecutorRepository executorRepository;

    static {
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            LOGGER.error("Unable to find host name", e);
        }
    }

    /**
     * Gets the config from the ConfigTaskHandler (@link{GET_CONFIG_COMMAND}).
     * @param group group name of the object to be fetched
     * @param key the primary key
     * @return the config as string, empty string if not found/error
     */
	public String getConfig(String group, String key, int count) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("group", group);
        params.put("key", key);
        params.put("count", Integer.toString(count));
        TaskResult result = this.executeCommand(GET_CONFIG_COMMAND, null, params);
        if (result == null) {
            return "";
        }
        return new String((byte[])result.getData());
    }

    /**
     * Executes a command
     */
    public TaskResult executeCommand(String commandName, byte[] data, Map<String, String> params) throws UnsupportedOperationException {
        return this.executorRepository.executeCommand(commandName, this.createRequestFromParams(commandName, data, params));
    }

    @Override
    public <T> TaskResult<T> executeCommand(String commandName, TaskRequestWrapper taskRequestWrapper, Decoder<T> decoder) throws UnsupportedOperationException {
        return this.executorRepository.executeCommand(commandName, taskRequestWrapper,decoder);
    }

    @Override
    public <T> TaskResult<T> executeCommand(String commandName, byte[] data, Map<String, String> params, Decoder<T> decoder) throws UnsupportedOperationException {
        return this.executorRepository.executeCommand(commandName, this.createRequestFromParams(commandName, data, params),decoder);
    }

    @Override
    public Future<TaskResult> executeAsyncCommand(String commandName, byte[] data, Map<String, String> params, Decoder decoder) throws UnsupportedOperationException {
        return this.executorRepository.executeAsyncCommand(commandName, this.createRequestFromParams(commandName, data, params),decoder);
    }

    /**
     * Executes a command asynchronously
     */
    public Future<TaskResult> executeAsyncCommand(String commandName, byte[] data, Map<String, String> params) throws UnsupportedOperationException {
        return this.executorRepository.executeAsyncCommand(commandName, this.createRequestFromParams(commandName, data, params));
    }
    
    /** Creates a TaskRequestWrapper from passed in params and sets the current server span on it*/
    private TaskRequestWrapper createRequestFromParams(String commandName, byte[] data, Map<String, String> params) {
        TaskRequestWrapper taskRequestWrapper = new TaskRequestWrapper();
        taskRequestWrapper.setCommandName(commandName);
        taskRequestWrapper.setData(data);
        taskRequestWrapper.setParams(params);
		// set the server request context on the received request
    	ServerSpan serverSpan = Brave.getServerSpanThreadBinder().getCurrentServerSpan();
    	RequestContext serverRequestContext = new RequestContext();
    	serverRequestContext.setCurrentServerSpan(serverSpan);	
    	taskRequestWrapper.setRequestContext(Optional.of(serverRequestContext));
    	return taskRequestWrapper;
    }

    /** Getter/Setter methods */
    public TaskHandlerExecutorRepository getExecutorRepository() {
        return this.executorRepository;
    }
    public void setExecutorRepository(TaskHandlerExecutorRepository executorRepository) {
        this.executorRepository = executorRepository;
    }
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    @Override
    public String getHostName() {
        return hostName;
    }
    /** End Getter/Setter methods */
}
