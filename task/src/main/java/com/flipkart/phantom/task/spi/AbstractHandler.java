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


import java.util.concurrent.atomic.AtomicInteger;

/**
 * The abstract handler class, which has functions necessary only for basic proxy functions
 * @author kartikbu
 * @version 1.0
 * @created 7/8/13 10:05 PM
 */
abstract public class AbstractHandler {

    /** The status showing the TaskHandler is inited and ready to use */
    public static int ACTIVE = 1;

    /** The status showing the TaskHandler is not inted/has been shutdown and should not be used */
    public static int INACTIVE = 0;

    /** The status of this ThriftProxy (active/inactive) */
    private AtomicInteger status = new AtomicInteger(INACTIVE);

    /**
     * Method which returns the name of the handler
     * @return String name
     */
    abstract public String getName();

    /**
     * Method which returns the type of handler
     * Used in dashboard
     * @return String type
     */
    abstract public String getType();

    /**
     * Method which returns a short description of the handler
     * Used in dashboard
     * @return String description
     */
    abstract public String getDetails();

    /**
     * Init lifecycle method
     * @param context context in which init is happening
     */
    abstract public void init(TaskContext context) throws Exception;

    /**
     * Shutdown lifecycle method
     * @param context context in which shutdown is happening
     */
    abstract public void shutdown(TaskContext context) throws Exception;

    /**
     * Set the handler status to Active
     */
    public final void activate() {
        this.status.set(AbstractHandler.ACTIVE);
    }

    /**
     * Set the handler status to Inactive
     */
    public final void deactivate() {
        this.status.set(AbstractHandler.INACTIVE);
    }

    /**
     * Check if the handler is active
     * @return boolean
     */
    public final boolean isActive() {
        return this.status.get() == AbstractHandler.ACTIVE;
    }


}
