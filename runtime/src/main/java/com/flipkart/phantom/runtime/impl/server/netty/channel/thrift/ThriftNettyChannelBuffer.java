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
package com.flipkart.phantom.runtime.impl.server.netty.channel.thrift;

import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.jboss.netty.buffer.ChannelBuffer;

/**
 * <code>ThriftNettyChannelBuffer</code> is a Thrift transport based on JBoss Netty's ChannelBuffers
 * 
 * @author Regunath B
 * @version 1.0, 26 Mar 2013
 */
public class ThriftNettyChannelBuffer extends TTransport {
	
	/** The ChannelBuffer instances for input and output*/
    private ChannelBuffer inputBuffer;
    private ChannelBuffer outputBuffer;

    /**
     * Constructor for this class
     * @param input the input ChannelBuffer
     * @param output the output ChannelBuffer
     */
    public ThriftNettyChannelBuffer(ChannelBuffer input, ChannelBuffer output) {
        this.inputBuffer = input;
        this.outputBuffer = output;
    }

    /**
     * Overriden superclass method. Returns 'true' always
     * @see org.apache.thrift.transport.TTransport#isOpen()
     */
    public boolean isOpen() {
        // Buffer is always open
        return true;
    }

    /**
     * Overriden superclass method. Does nothing as the ChannelBuffer is already open
     * @see org.apache.thrift.transport.TTransport#open()
     */
    public void open() throws TTransportException {
        // do nothing as ChannelBuffer is open always
    }

    /**
     * Overriden superclass method. Does nothing as the ChannelBuffer is always open
     * @see org.apache.thrift.transport.TTransport#close()
     */
    public void close() {
        // do nothing as ChannelBuffer is always open
    }

    /**
     * Overriden superclass method. Reads all readable bytes into the input ChannelBuffer
     * @see org.apache.thrift.transport.TTransport#read(byte[], int, int)
     */
    public int read(byte[] buffer, int offset, int length) throws TTransportException {
        int readableBytes = this.inputBuffer.readableBytes();
        int bytesToRead = length > readableBytes ? readableBytes : length;
        this.inputBuffer.readBytes(buffer, offset, bytesToRead);
        return bytesToRead;
    }

    /**
     * Overriden superclass method. Writes all the output bytes into the output ChannelBuffer
     * @see org.apache.thrift.transport.TTransport#write(byte[], int, int)
     */
    public void write(byte[] buffer, int offset, int length) throws TTransportException {
        this.outputBuffer.writeBytes(buffer, offset, length);
    }

    /** Start getter methods */
    public ChannelBuffer getInputBuffer() {
        return this.inputBuffer;
    }
    public ChannelBuffer getOutputBuffer() {
        return this.outputBuffer;
    }
    /** End getter methods */
}