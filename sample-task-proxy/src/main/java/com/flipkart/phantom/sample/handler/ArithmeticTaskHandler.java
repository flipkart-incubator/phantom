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
package com.flipkart.phantom.sample.handler;

import java.util.Map;

import com.flipkart.phantom.task.impl.HystrixTaskHandler;
import com.flipkart.phantom.task.spi.TaskContext;
import com.flipkart.phantom.task.spi.TaskResult;

/**
 * A simple task handler which does basic arithmetic operations.
 *
 * @author kartikbu
 * @version 1.0
 * @created 24/7/13 12:29 AM
 */
public class ArithmeticTaskHandler extends HystrixTaskHandler {

    public final String CMD_ADD = "add";
    public final String CMD_SUB = "subtract";
    public final String CMD_MUL = "multiply";
    public final String CMD_DIV = "divide";

    /**
     * Abstract method implementations.
     * @see com.flipkart.phantom.task.impl.TaskHandler#execute(com.flipkart.phantom.task.spi.TaskContext, String, java.util.Map, byte[])
     */
    @Override
    public TaskResult<byte[]> execute(TaskContext taskContext, String command, Map<String, String> params, byte[] data) throws RuntimeException {

        float num1 = Float.parseFloat(params.get("num1"));
        float num2 = Float.parseFloat(params.get("num2"));

        if (CMD_ADD.equals(command)) {
            return new TaskResult<byte[]>(true, Float.toString(num1+num2));
        } else if (CMD_SUB.equals(command)) {
            return new TaskResult<byte[]>(true, Float.toString(num1+num2));
        } else if (CMD_MUL.equals(command)) {
            return new TaskResult<byte[]>(true, Float.toString(num1+num2));
        } else if (CMD_DIV.equals(command)) {
            return new TaskResult<byte[]>(true, Float.toString(num1+num2));
        } else {
            return null;
        }

    }

    /**
     * Abstract method implementations.
     * @see com.flipkart.phantom.task.impl.TaskHandler#getName()
     */
    @Override
    public String getName() {
        return "Arithmetic";
    }

    /**
     * Abstract method implementations.
     * @see com.flipkart.phantom.task.impl.TaskHandler#shutdown(com.flipkart.phantom.task.spi.TaskContext)
     */
    @Override
    public void shutdown(TaskContext taskContext) throws Exception {

    }

    /**
     * Abstract method implementations.
     * @see com.flipkart.phantom.task.impl.TaskHandler#getCommands()
     */
    @Override
    public String[] getCommands() {
        return new String[]{CMD_ADD,CMD_SUB,CMD_MUL,CMD_DIV};  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Abstract method implementations.
     * @see com.flipkart.phantom.task.impl.HystrixTaskHandler#getFallBack(com.flipkart.phantom.task.spi.TaskContext, String, java.util.Map, byte[])
     */
    @Override
    public TaskResult<byte[]> getFallBack(TaskContext taskContext, String command, Map<String, String> params, byte[] data) {
        return null;
    }

}
