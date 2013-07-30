package com.flipkart.phantom.task.spi.registry;

import com.flipkart.phantom.task.spi.TaskContext;

/**
 * @author kartikbu
 * @version 1.0
 * @created 30/7/13 12:43 AM
 */
abstract public class AbstractHandlerRegistry {

    public abstract void init(TaskContext taskContext) throws Exception;

    public abstract void shutdown(TaskContext taskContext) throws Exception;

}
