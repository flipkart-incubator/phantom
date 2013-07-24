package com.flipkart.phantom.sample.handler;

import com.flipkart.phantom.task.impl.TaskResult;
import com.flipkart.phantom.task.spi.HystrixTaskHandler;
import com.flipkart.phantom.task.spi.TaskContext;

import java.util.Map;

/**
 * @author kartikbu
 * @version 1.0
 * @created 24/7/13 12:29 AM
 */
public class ArithmeticTaskHandler extends HystrixTaskHandler {

    public final String CMD_ADD = "add";
    public final String CMD_SUB = "substract";
    public final String CMD_MUL = "multiply";
    public final String CMD_DIV = "divide";

    @Override
    public TaskResult execute(TaskContext taskContext, String command, Map<String, String> params, byte[] data) throws RuntimeException {

        float num1 = Float.parseFloat(params.get("num1"));
        float num2 = Float.parseFloat(params.get("num2"));

        if (CMD_ADD.equals(command)) {
            return new TaskResult(true, new Float(num1+num2).toString());
        } else if (CMD_SUB.equals(command)) {
            return new TaskResult(true, new Float(num1-num2).toString());
        } else if (CMD_MUL.equals(command)) {
            return new TaskResult(true, new Float(num1*num2).toString());
        } else if (CMD_DIV.equals(command)) {
            return new TaskResult(true, new Float(num1/num2).toString());
        } else {
            return null;
        }

    }

    @Override
    public String getName() {
        return "Arithmetic";
    }

    @Override
    public void shutdown(TaskContext taskContext) throws Exception {

    }

    @Override
    public String[] getCommands() {
        return new String[]{CMD_ADD,CMD_SUB,CMD_MUL,CMD_DIV};  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public TaskResult getFallBack(TaskContext taskContext, String command, Map<String, String> params, byte[] data) {
        return null;
    }

}
