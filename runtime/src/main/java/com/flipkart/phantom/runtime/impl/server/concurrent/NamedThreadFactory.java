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

package com.flipkart.phantom.runtime.impl.server.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <code>NamedThreadFactory</code> is a convenience implementation of {@link ThreadFactory} that may be used to create meaningful thread names
 * 
 * @author Regunath B
 * @version 1.0, 15 Mar 2013
 */

public class NamedThreadFactory implements ThreadFactory {
	
	/** Default thread name prefix*/
	private static final String DEFAULT_THREAD_NAME_PREFIX = "ServiceProxy-Runtime-Thread";
	
	/** Counter used in thread names and incremented each time a Thread is created*/
	private static AtomicInteger threadCounter = new AtomicInteger(1); 
	
	/** The name prefix for all threads created by this factory */
    private String name = DEFAULT_THREAD_NAME_PREFIX; 
    
    /** Daemon and priority settings for all threads created by this factory*/
    private boolean daemon; 
    private int priority; 

    /**
     * Constructor for this class. Creates threads with the specified name that are not daemons and using the priority of the invoking thread
     * @param name the name prefix for threads created by this factory
     */
    public NamedThreadFactory(String name) { 
        this(name, false, Thread.currentThread().getPriority()); 
    } 

    /**
     * Constructor for this class. Creates threads with the specified name and daemon settings and using the priority of the invoking thread
     * @param name the name prefix for threads created by this factory
     * @param daemon the daemon indicator for created threads
     */
    public NamedThreadFactory(String name, boolean daemon) { 
        this(name, daemon, Thread.currentThread().getPriority()); 
    } 

    /**
     * Constructor for this class. Creates threads with the specified thread properties
     * @param name the name prefix for threads created by this factory
     * @param daemon the daemon indicator for created threads
     * @param priority the priority for created threads
     */
    public NamedThreadFactory(String name, boolean daemon, int priority) { 
        this.name = name; 
        this.daemon = daemon; 
        this.priority = priority; 
    } 

    /**
     * Interface method implementation. Creates threads using the properties set on this factory
     * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
     */
    public Thread newThread(Runnable runnable) { 
        Thread thread = new Thread(runnable, this.name + "-" + NamedThreadFactory.threadCounter.getAndIncrement()); 
        thread.setDaemon(this.daemon); 
        thread.setPriority(this.priority); 
        return thread; 
    }
}