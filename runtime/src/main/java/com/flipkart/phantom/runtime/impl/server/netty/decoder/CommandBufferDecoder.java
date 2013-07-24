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
package com.flipkart.phantom.runtime.impl.server.netty.decoder;

import com.flipkart.phantom.runtime.impl.server.netty.handler.command.CommandInterpreter;
import com.flipkart.phantom.runtime.impl.server.netty.handler.command.CommandInterpreter.ReadFailure;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * <code>CommandBufferDecoder</code> is an extension of the Netty {@link FrameDecoder} that ensures that all Command protocol bytes have been received
 * before the {@link MessageEvent} is constructed for use by other upstream channel handlers.
 * The Command protocol does not indicate length of bytes relayed up front. Occurrence of a line feed ({@link CommandInterpreter#LINE_FEED}) or total byte size 
 * equal to or greater than {@link CommandInterpreter#MAX_COMMAND_INPUT} is treated as end of input bytes. The {@link CommandInterpreter} is used to interpret
 * the protocol bytes. Any Exception in reading the Command is caught
 * 
 * @author Regunath B
 * @version 1.0, 12 April, 2013
 */
public class CommandBufferDecoder extends FrameDecoder {

    /** Logger for this class*/
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandBufferDecoder.class);

	/**
	 * Overriden super class method. Uses the {@link CommandInterpreter#readCommand(ChannelBuffer)} to interpret the Command and thereby check if all bytes have
	 * been received, returns a null otherwise.
	 * @see org.jboss.netty.handler.codec.frame.FrameDecoder#decode(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.Channel, org.jboss.netty.buffer.ChannelBuffer)
	 */
	protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
		int beginIndex = buffer.readerIndex();
		buffer.markReaderIndex();
		CommandInterpreter.ProxyCommand proxyCommand = new CommandInterpreter().interpretCommand(buffer);
		if (proxyCommand.getReadFailure() != null && proxyCommand.getReadFailure() == ReadFailure.INSUFFICIENT_DATA) {
			LOGGER.debug("Frame decode failed due to insufficient data. Message is : " + proxyCommand.getReadFailureDescription());
            buffer.resetReaderIndex();
			return null; // we return null here and Netty will call this decoder again when more data is available
		}
	    int endIndex = buffer.readerIndex();
	    buffer.resetReaderIndex();
	    return buffer.readSlice(endIndex - beginIndex);		
	}

}
