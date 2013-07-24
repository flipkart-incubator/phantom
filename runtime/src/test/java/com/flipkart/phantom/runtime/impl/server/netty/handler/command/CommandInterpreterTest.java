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
package com.flipkart.phantom.runtime.impl.server.netty.handler.command;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * User: kartikbu
 * Date: 20/6/13
 * Time: 2:46 PM
 */
public class CommandInterpreterTest {

    private CommandInterpreter commandInterpreter = new CommandInterpreter();

    @Before
    public void setUp() {

    }

    @After
    public void tearDown() {

    }

    @Test
    public void testInterpretCommand() {

        CommandInterpreter.ProxyCommand command;

        // just command
        command = getCommand("testCommand\n");
        assertTrue("Command parsing failed",command != null && command.getReadFailure() == null);
        assertTrue("Parsed command mismatch: "+command.getCommand(), "testCommand".equals(command.getCommand()));
        assertTrue("Command parameters not empty", command.getCommandParams().isEmpty());
        assertTrue("Command data not empty", command.getCommandData() == null);

        // command + params
        command = getCommand("testCommand p1=v1 p2=v2\n");
        assertTrue("Command parsing failed",command != null && command.getReadFailure() == null);
        assertTrue("Parsed command mismatch: "+command.getCommand(), "testCommand".equals(command.getCommand()));
        assertTrue("Command parameters mismatch: "+command.getCommandParams().size(), command.getCommandParams().size() == 2);
        assertTrue("Command data not empty", command.getCommandData() == null);

        // command + params + different delim
        command = getCommand("#testCommand#p1=v1#p2=v2\n");
        assertTrue("Command parsing failed",command != null && command.getReadFailure() == null);
        assertTrue("Parsed command mismatch: "+command.getCommand(), "testCommand".equals(command.getCommand()));
        assertTrue("Command parameters mismatch: "+command.getCommandParams().size(), command.getCommandParams().size() == 2);
        assertTrue("Command data not empty", command.getCommandData() == null);

        // command + params + data
        command = getCommand("testCommand p1=v1 p2=v2 8\ntestData");
        assertTrue("Command parsing failed",command != null && command.getReadFailure() == null);
        assertTrue("Parsed command mismatch: "+command.getCommand(), "testCommand".equals(command.getCommand()));
        assertTrue("Command parameters mismatch: "+command.getCommandParams().size(), command.getCommandParams().size() == 2);
        assertTrue("Command data mismatch: "+command.getCommandData(), "testData".equals(new String(command.getCommandData())));

        // incorrect command - no newline
        command = getCommand("testCommand p1=v1 p2=v2");
        assertTrue("Command parsing passed (which should not)", command != null && command.getReadFailure() != null);

        // incorrect command - data size mismatch
        command = getCommand("testCommand p1=v1 p2=v2 10\ntestData");
        assertTrue("Command parsing passed (which should not)", command != null && command.getReadFailure() != null);

    }

    private CommandInterpreter.ProxyCommand getCommand(String command) {
        ChannelBuffer buffer = ChannelBuffers.copiedBuffer(command.getBytes());
        try {
            return commandInterpreter.interpretCommand(buffer);
        } catch (Exception e) {
            return null;
        }
    }

}
