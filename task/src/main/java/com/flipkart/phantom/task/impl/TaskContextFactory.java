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

import com.flipkart.phantom.task.spi.TaskContext;
import org.springframework.beans.factory.FactoryBean;

/**
 * Factory for creating {@link TaskContext} instance. This class returns a classloader scope singleton instance. Also implements the Spring {@link FactoryBean}
 * to enable injection into Spring ApplicationContext
 * 
 * @author Regunath B
 * @version 1.0, 8th May, 2013
 */
public class TaskContextFactory implements FactoryBean<TaskContext> {
	
	/**
	 * The TaskHandlerExecutorRepository instance for getting thrift handler executor instances
	 */
	private TaskHandlerExecutorRepository executorRepository;

	/** The singleton instance of the TaskContext */
	private static TaskContext singleton;
	
	/**
	 * Static accessor method to return the classloader scope singleton instance of the TaskContext.
	 * WARN : the singleton instance may be null i.e. uninitialized if this FactoryBean has not been declared in a Spring bean XML file
	 * @return null or the initialized TaskContext instance
	 */
	public static TaskContext getTaskContext() {
		return TaskContextFactory.singleton;
	}
	
	/**
	 * Interface method implementation. Returns the TaskContext type
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 */
	public Class<TaskContext> getObjectType() {
		return TaskContext.class;
	}

	/**
	 * Interface method implementation. Returns true
	 * @see org.springframework.beans.factory.FactoryBean#isSingleton()
	 */
	public boolean isSingleton() {
		return true;
	}

	/**
	 * Interface method implementation. Creates and returns a TaskContext instance
	 * @see org.springframework.beans.factory.FactoryBean#getObject()
	 */
	public TaskContext getObject() throws Exception {
		TaskContextFactory.singleton = new TaskContextImpl();
		((TaskContextImpl)TaskContextFactory.singleton).setExecutorRepository(this.executorRepository);
		return TaskContextFactory.singleton;
	}

	/** Getter/Setter methods */
	public TaskHandlerExecutorRepository getExecutorRepository() {
		return this.executorRepository;
	}
	public void setExecutorRepository(TaskHandlerExecutorRepository executorRepository) {
		this.executorRepository = executorRepository;
		// make sure that the TaskHandlerExecutorRepository is set on the singleton instance created by this factory
		((TaskContextImpl)TaskContextFactory.singleton).setExecutorRepository(this.executorRepository);
	}
	
}
