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

import com.flipkart.phantom.runtime.impl.server.netty.channel.thrift.ThriftNettyChannelBuffer;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.TTransportException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;
import org.jboss.netty.handler.codec.replay.VoidEnum;

/**
 * <code>ThriftBufferDecoder</code> is an extension of the Netty {@link ReplayingDecoder} that ensures that all Thrift protocol bytes have been received
 * before the {@link MessageEvent} is constructed for use by other upstream channel handlers.
 * The Thrift protocol doesnot indicate in anyway (such as header bytes denoting length of byte stream ) the data size of protocol messages. This decoder
 * therefore attempts to read the Thrift message from the transport using the protocol. An unsuccessful read indicates that the bytes have not been fully 
 * received. This decoder returns a null object in {@link #decode(ChannelHandlerContext, Channel, ChannelBuffer, VoidEnum)} in such scenarios and the Netty
 * framework would then call it again when more bytes are received, eventually resulting in all required bytes becoming available. This decoder resets the
 * reader index on the input {@link ChannelBuffer} at the end of each {@link #decode(ChannelHandlerContext, Channel, ChannelBuffer, VoidEnum)} call to permit
 * byte consumption in upstream handlers. 
 * 
 * @author Regunath B
 * @version 1.0, 3 April, 2013
 */

public class ThriftBufferDecoder extends ReplayingDecoder<VoidEnum> {

	/** The Thrift binary protocol factory*/
	private TProtocolFactory protocolFactory =  new TBinaryProtocol.Factory();

	/**
	 * Interface method implementation. Tries to read the Thrift protocol message. Returns null if unsuccessful, else returns the read byte array. Also
	 * resets the {@link ChannelBuffer} reader index to nullify the effect of the read operation.
	 * @see org.jboss.netty.handler.codec.replay.ReplayingDecoder#decode(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.Channel, org.jboss.netty.buffer.ChannelBuffer, java.lang.Enum)
	 */
	protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer, VoidEnum voidEnum) throws Exception {
		ThriftNettyChannelBuffer ttransport = new ThriftNettyChannelBuffer(buffer, null); // we dont use the output buffer, so null is fine
		TProtocol iprot = this.protocolFactory.getProtocol(ttransport);
		int beginIndex = buffer.readerIndex();
		buffer.markReaderIndex();

	    iprot.readMessageBegin();
	    TProtocolUtil.skip(iprot, TType.STRUCT);
	    iprot.readMessageEnd();

	    int endIndex = buffer.readerIndex();
	    buffer.resetReaderIndex();

	    return buffer.readSlice(endIndex - beginIndex);		
	}
	
	/**
	 * Overriden superclass method. Returns the result of calling {@link #decode(ChannelHandlerContext, Channel, ChannelBuffer, VoidEnum)} or null
	 * in case of {@link TTransportException} indicating that expected bytes have been received yet.
	 * @see org.jboss.netty.handler.codec.replay.ReplayingDecoder#decodeLast(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.Channel, org.jboss.netty.buffer.ChannelBuffer, java.lang.Enum)
	 */
	protected java.lang.Object decodeLast(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer, VoidEnum voidEnum) throws Exception {
		try {
		    return decode(ctx, channel, buffer, voidEnum);
		} catch (TTransportException te) {
			return null; // return null to indicate that not all expected bytes have been received yet
		}
	}

}
