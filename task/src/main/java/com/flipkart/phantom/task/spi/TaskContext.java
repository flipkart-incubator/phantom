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

package com.flipkart.phantom.task.spi;

import com.flipkart.phantom.task.impl.TaskHandler;
import com.flipkart.phantom.task.impl.TaskResult;
import org.codehaus.jackson.map.ObjectMapper;

import java.util.Map;
import java.util.concurrent.Future;

/**
 * <code>TaskContext</code> provides methods for {@link com.flipkart.phantom.task.impl.TaskHandler} to communicate with it's Component Container - to execute tasks on other TaskHandler instances,
 * perform profiling operations and result serialization.
 *  
 * @author devashishshankar
 * @author regunath.balasubramanian
 * 
 * @version 1.0, 19th March, 2013
 * @version 2.0, 11th July, 2013
 */
public interface TaskContext {
	
	/**
	 * Gets the config from the ConfigTaskHandler.
	 * @param group group name of the object to be fetched
	 * @param key the primary key
	 * @return the config as string, empty string if not found/error
	 */
	public String getConfig(String group, String key, int count);

	/**
	 * Executes a thrift identified by the specified command name. This command executes synchronously
	 * @param commandName the command to execute
	 * @param data the command processing data
	 * @param params data parameters
	 * @return a TaskResult instance with the execution outcome
	 * @throws UnsupportedOperationException in case none of the registered TaskHandler instances support the specified command
	 */
	public TaskResult executeCommand(String commandName, byte[] data, Map<String,String> params) throws UnsupportedOperationException;

	/**
	 * Executes a command asynchronously and returns a {@link Future} to get the {@link TaskResult} from
	 * @see TaskContext#executeCommand(String, byte[], Map)
	 */
	public Future<TaskResult> executeAsyncCommand(String commandName, byte[] data, Map<String, String> params) throws UnsupportedOperationException;

	/**
	 * Profiles the thrift command. (For logging/metrics calculation)
	 * @param handler The thrift Handler which executed the command
	 * @param command The command name
	 * @param diff Time taken to execute the command
	 * @param tags Optional info
	 */
	public void profileCommand(TaskHandler handler, String command, Long diff, String tags);

	/** Gets the ObjectMapper instance for result serialization to JSON*/
	public ObjectMapper getObjectMapper();

    /** Gets Host Name of current server */
    public String getHostName();
}
