/*
 * Copyright 2012-2015, Flipkart Internet Pvt Ltd. All rights reserved.
 * 
 * This software is the confidential and proprietary information of Flipkart Internet Pvt Ltd. ("Confidential Information").  
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of the license 
 * agreement you entered into with Flipkart.    
 * 
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
