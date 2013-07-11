/* Copyright 2012-2015, Flipkart Internet Pvt Ltd. All rights reserved.
 * 
 * This software is the confidential and proprietary information of Flipkart Internet Pvt Ltd. ("Confidential Information").  
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of the license 
 * agreement you entered into with Flipkart.    
 * 
 */
package com.flipkart.sp.task.impl.task;

import org.springframework.beans.factory.FactoryBean;

import com.flipkart.sp.task.spi.task.TaskContext;

/**
 * Factory for creating {@link TaskContext} instance. This class returns a classloader scope singleton instance. Also implements the Spring {@link FactoryBean}
 * to enable injection into Spring ApplicationContext
 * 
 * @author Regunath B
 * @version 1.0, 8th May, 2013
 */
public class TaskContextFactory implements FactoryBean<TaskContext> {
	
	/**
	 * The TaskHandlerExecutorRepository instance for getting task handler executor instances
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
	}
	
}
