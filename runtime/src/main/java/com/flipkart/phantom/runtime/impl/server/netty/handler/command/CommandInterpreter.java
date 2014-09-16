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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.phantom.task.impl.TaskResult;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.springframework.util.SerializationUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * <code>CommandInterpreter</code> interprets a Command from the Netty {@link MessageEvent}
 * The command protocol is defined as follows:
 * 
 * <pre>
 * Command is described as below
 * +------------+---------+------------+--------------+---+---------------+------------+--------------+---+---------------+----+
 * | delim char | command | delim char | param name 1 | = | param value 1 | delim char | param name n | = | param value n | \n |
 * +------------+---------+------------+--------------+---+---------------+------------+--------------+---+---------------+----+
 * +------------+
 * | data bytes |
 * +------------+
 * 
 * where
 * <ul>
 *  <li>Command and params appear on a single line terminating in '\n' char</li>
 *	<li>'delim char' is any non-ascii character</li>
 * 	<li>'command' is an arbitrary sequence of characters</li>
 * 	<li>'param name'='param value' can repeat any number of times. Are of type : arbitrary sequence of characters</li>
 *	<li>'data' is an arbitrary sequence of bytes</li>
 * </ul>
 * 
 * Response from Command execution is described as below
 * 
 * +--------+----+
 * | status | \n |
 * +--------+----+
 *  (or)
 * +--------+-------------+-------------+----+
 * | status | white space | data length | \n |
 * +--------+-------------+-------------+----+
 * +------------+
 * | data bytes |
 * +------------+
 * 
 * <pre>
 * 
 * Command protocol interpretation code is based on the implementation in com.flipkart.w3.agent.W3Agent
 * 
 * @author Regunath B
 * @version 1.0, 22 Mar 2013
 */

@SuppressWarnings("rawtypes")
public class CommandInterpreter {

	/** Constant for max command input size*/
	public static final int MAX_COMMAND_INPUT = 20480;

	/** Constants for characters that have special meaning in the command protocol*/
	public static final char LINE_FEED = '\n';

	private static final char CARRIAGE_RETURN = '\r'; 
	private static final char DEFAULT_DELIM = ' '; 
	private static final char PARAM_VALUE_SEP = '='; 
	private static final char[] ASCII_LOW = {'a','z'};
	private static final char[] ASCII_HIGH = {'A','Z'};

	private static final String SUCCESS = "SUCCESS";
	private static final String ERROR = "ERROR";
	private static final String NULL_STRING = ""; 

	/** Default param value, when none is specified*/
	private static final String DEFAULT_PARAM_VALUE = "true";

	/** The Jackson ObjectMapper for writing output as JSON*/
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(); // using an instance variable as this class is deemed to be thread-safe

	/** Enumeration of read failure reasons */
	public enum ReadFailure {
		INSUFFICIENT_DATA,
	}

	/**
	 * Helper method to read and return a ProxyCommand from an {@link InputStream}. Throws Exception for all data read errors including partial
	 * reads arising from insufficient data
	 * @param inputStream the InputStream instance
	 * @return the read ProxyCommand
	 * @throws Exception in case of errors
	 */
	public ProxyCommand readCommand(InputStream inputStream) throws Exception {
		return this.interpretCommand(inputStream, true);
	}
	
	/**
	 * Helper method to read and return a ProxyCommand from an input Channel {@link MessageEvent}. Throws Exception for all data read errors including partial
	 * reads arising from insufficient data
	 * @param event the MessageEvent instance
	 * @return the read ProxyCommand
	 * @throws Exception in case of errors
	 */
	public ProxyCommand readCommand(MessageEvent event) throws Exception {
		return this.interpretCommand(new ChannelBufferInputStream((ChannelBuffer)event.getMessage()), true);
	}

	/**
	 * Helper method to read and return a ProxyCommand from a ChannelBuffer {@link ChannelBuffer}. Returns a ProxyCommand for partial read errors and throws
	 * Exception only for irrecoverable errors. Useful method to decode data frames from the raw input channel buffer.  
	 * @param buffer the input buffer
	 * @return the read ProxyCommand
	 * @throws Exception in case of errors
	 */
	public ProxyCommand interpretCommand(ChannelBuffer buffer) throws Exception {
		return this.interpretCommand(new ChannelBufferInputStream(buffer), false);
	}

	/**
	 * Writes the specified TaskResult data to the channel output following the Command protocol
	 * @param ctx the ChannelHandlerContext
	 * @param event the ChannelEvent
	 * @param result the TaskResult data written to the channel response
	 * @throws Exception in case of any errors
	 */
	public void writeCommandExecutionResponse(ChannelHandlerContext ctx, ChannelEvent event, TaskResult result) throws Exception {
		ChannelBuffer writeBuffer = ChannelBuffers.dynamicBuffer();
		this.writeCommandExecutionResponse(new ChannelBufferOutputStream(writeBuffer), result);
		Channels.write(ctx, event.getFuture(), writeBuffer);    	
	}
	
	/**
	 *  Writes the specified TaskResult data to the Outputstream following the Command protocol
	 * @param outputStream the Outputstream to write result data to
	 * @param result the TaskResult to write
	 * @throws Exception in case of any errors
	 */
	public void writeCommandExecutionResponse(OutputStream outputStream, TaskResult result) throws Exception {
		//Don't write anything if the result is null
		if(result==null) {
			return;
		}
		String message = result.getMessage();
		boolean success = result.isSuccess();
		int resultDatalength = result.getLength();
		String metaContents = (message==null ? (success ? SUCCESS : ERROR) : message);
		metaContents += (resultDatalength==0 ? LINE_FEED : (DEFAULT_DELIM + NULL_STRING + resultDatalength + NULL_STRING + LINE_FEED));

		// write the meta contents
		outputStream.write(metaContents.getBytes());

		// now write the result data
		if(result.isDataArray()) {
			for(Object object : result.getDataArray()) {
				if(object!=null) {
					if(object instanceof byte[]) {
						outputStream.write((byte[]) object);
					} else {
						outputStream.write(SerializationUtils.serialize(object));
					}
				}
			}
		} else {
			Object data = result.getData();
			if(data!=null) {
				if(data instanceof byte[]) {
					byte[] byteData = (byte[]) data;
					if(byteData.length>0)  {
						outputStream.write(byteData);
					}
				} else {
					outputStream.write(SerializationUtils.serialize(data));
				}
			}
		}					
	}

	/**
	 * Helper method to read and return a ProxyCommand from an input {@link InputStream}
	 * @param inputStream the InputStream instance
	 * @param isFramedTransport boolean indicator that defines mechanism for reporting errors - Exceptions vs a ProxyCommand with error description
	 * @return the read ProxyCommand
	 * @throws Exception in case of errors
	 */    
	private ProxyCommand interpretCommand(InputStream inputStream, boolean isFramedTransport) throws Exception {
        ProxyCommand readCommand = null;
		byte[] readBytes = new byte[MAX_COMMAND_INPUT];

		int byteReadIndex=0, commandEndIndex=0, dataStartIndex=0, dataLength=0;
		while(byteReadIndex < MAX_COMMAND_INPUT) {
			int bytesRead = inputStream.read(readBytes, byteReadIndex, MAX_COMMAND_INPUT-byteReadIndex); // try to read as much as is available into the byte array
			if(bytesRead <= 0){ // check if no data was read at all. Throw an IllegalArgumentException to indicate unexpected end of stream
				if (isFramedTransport) {
					throw new IllegalArgumentException("Invalid read. Encountered end of stream before reading a single byte");
				} else {
					return new ProxyCommand(ReadFailure.INSUFFICIENT_DATA, "Invalid read. Encountered end of stream before reading a single byte");
				}
			}
			for(int i=0; i<bytesRead; i++) { // look for the NEW_LINE character that signals end of command and params input
				if(readBytes[byteReadIndex+i ]== LINE_FEED) {
					commandEndIndex = byteReadIndex+i;
					break;
				}
			}
			if (bytesRead > 0) {
				byteReadIndex += bytesRead; // skip the read bytes by moving the index for next read
			}
			if(commandEndIndex > 0 || bytesRead <= 0) { // break the read loop if end of command line is reached (or) EOS (end of stream is reached)                 											
				break;									// i.e. no more data available for read
			}
		}

		if(commandEndIndex==0) { // report a suitable error if NEW_LINE was not encountered at all (or) if bytes read has exceeded MAX_COMMAND_INPUT
			if(byteReadIndex < MAX_COMMAND_INPUT){
				if (isFramedTransport) {
					throw new IllegalArgumentException("Stream ended before encountering a \\n: " + new String(readBytes,0,byteReadIndex)); 
				} else {
					return new ProxyCommand(ReadFailure.INSUFFICIENT_DATA, "Stream ended before encountering a \\n: " + new String(readBytes,0,byteReadIndex));
				}
			} else {
				throw new IllegalArgumentException("Maximum command line size allowed: " + MAX_COMMAND_INPUT +" Command : "+ new String(readBytes,0,byteReadIndex));
			}
		}

		// The input data appears to adhere to the command protocol. Proceed to read the command, params and data
		dataStartIndex = commandEndIndex+1;
		if (readBytes[commandEndIndex-1] == CARRIAGE_RETURN) {
			commandEndIndex--;	// handle the CR for people who still haven't moved on from telnet to netcat
		}

		byte delimiter = DEFAULT_DELIM;
		int fragmentStart = 0;
		if(!(readBytes[0]>=ASCII_LOW[0] && readBytes[0]<=ASCII_LOW[1]) && !(readBytes[0]>=ASCII_HIGH[0] && readBytes[0]<=ASCII_HIGH[1])) {
			delimiter = readBytes[0]; // the delimiter is not DEFAULT_DELIM but the non-ascii character appearing as the first byte
			fragmentStart=1;
		}
		int fragmentIndex = this.getNextCommandFragmentPosition(readBytes, fragmentStart, commandEndIndex, delimiter);
		readCommand = new ProxyCommand(new String(readBytes, fragmentStart, fragmentIndex-fragmentStart));

		Map<String,String> commandParams = new HashMap<String, String>();
		// gather params
		while(fragmentIndex < commandEndIndex) {
			// skip initial delims
			while(fragmentIndex < commandEndIndex && readBytes[fragmentIndex] == delimiter) {
				fragmentIndex++;
			}
			if (fragmentIndex == commandEndIndex) { 
				break;
			}
			// read first char
			if(Character.isDigit((char)readBytes[fragmentIndex])) {
				// this is the datalen
				try {
					dataLength = Integer.parseInt(new String(readBytes, fragmentIndex, commandEndIndex-fragmentIndex));
					break;
				} catch (Exception e) {
					throw new IllegalArgumentException("Invalid syntax in command: "+new String(readBytes), e);
				}
			} else {
				fragmentStart = fragmentIndex;
				fragmentIndex = getNextCommandFragmentPosition(readBytes, fragmentIndex+1, commandEndIndex, delimiter);
				int paramValueSepIndex = 0;
				for(int i=fragmentStart; i<fragmentIndex; i++) {
					if (readBytes[i] == PARAM_VALUE_SEP) {
						paramValueSepIndex = i;
						break;
					}
				}
				if (paramValueSepIndex > 0) {
					commandParams.put(new String(readBytes, fragmentStart, paramValueSepIndex-fragmentStart), 
							new String(readBytes, paramValueSepIndex+1, fragmentIndex-paramValueSepIndex-1));
				} else {
					commandParams.put(new String(readBytes, fragmentStart, fragmentIndex-fragmentStart), DEFAULT_PARAM_VALUE); // initialize with default value if none specified
				}
				// set the params on the ProxyCommand object
				readCommand.setCommandParams(commandParams);
			}        	
		}

		if(dataLength > 0) {
			byte[] commandData = new byte[dataLength];
			int dataByteReadIndex = byteReadIndex-dataStartIndex;
			if(dataStartIndex < byteReadIndex){
				System.arraycopy(readBytes, dataStartIndex, commandData, 0, dataByteReadIndex);
			}
			while(dataByteReadIndex<dataLength){
				if (inputStream.available() < (dataLength-dataByteReadIndex)) { 
					if (!isFramedTransport) { // check if all data bytes have been received. Return immediately for non framed transports
						return new ProxyCommand(ReadFailure.INSUFFICIENT_DATA, "Stream ended before all data was read. Length of data bytes needed : " + (dataLength-dataByteReadIndex));
					}
				}
				int actualBytesRead = inputStream.read(commandData, dataByteReadIndex, dataLength-dataByteReadIndex);
				if (actualBytesRead <= 0) { // 0 bytes not possible because dataLength-dataByteReadIndex is non-zero, -1 is returned if no byte is available because the stream is at end of file (as per Javadocs)
					throw new IllegalArgumentException("Insufficient bytes read for command : " + readCommand.getCommand() + ". Expected : " + (dataLength-dataByteReadIndex) + " but read : " + actualBytesRead);
				}
				dataByteReadIndex += actualBytesRead;					
			}
			// set the command data on the ProxyCommand object
			readCommand.setCommandData(commandData);
		}
		return readCommand;
	}
	
	/**
	 * Helper method to return the next command fragment position in the input byte array. Considers the start index to skip bytes and the delim char to
	 * identify the next fragment
	 * @return the start position of the next command fragment
	 */
	private int getNextCommandFragmentPosition(byte[] arr, int fragmentStart, int lastPos, byte delim) {
		for(; fragmentStart<lastPos; fragmentStart++) {
			if(arr[fragmentStart]==delim) {
				return fragmentStart;
			}
		}
		return fragmentStart;
	}

	/**
	 * Helper class to store command protocol objects
	 */
	public class ProxyCommand {
		
		/** The command String*/
		private String command;
		
		/** The read failure reason and message*/
		private ReadFailure readFailure;
		private String readFailureDescription;

		/** The command parameters*/
		private Map<String, String> commandParams = new HashMap<String, String>();

		/** The command data*/
		private byte[] commandData;

		/**
		 * Constructor for this class
		 * @param command the command string
		 */
		public ProxyCommand(String command) {
			this.command = command;
		}

		/**
		 * Constructor for this class
		 * @param readFailure the ReadFailure reason
		 * @param readFailureDescription the error description
		 */
		public ProxyCommand(ReadFailure readFailure, String readFailureDescription) {
			this.readFailure = readFailure;
			this.readFailureDescription = readFailureDescription;
		}

		/**
		 * Overriden super class method. Returns a string representation of this ProxyCommand
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			try {
				return String.format("ProxyCommand[Command = %s, Read Error = %s, Params = %s" + "]",this.getCommand(), this.getReadFailureDescription(), 
						commandParams != null ? OBJECT_MAPPER.writeValueAsString(this.getCommandParams()) : "");
			} catch (Exception e) {
				// ignore JSON formating errors and return just the command string
				return "ProxyCommand[Command = " + command +  ". Read Error = " + readFailureDescription + "]"; 
			}
		}

		/** Start setter/getter methods*/
		public String getCommand() {
			return command;
		}		
		public ReadFailure getReadFailure() {
			return readFailure;
		}
		public String getReadFailureDescription() {
			return readFailureDescription;
		}
		public Map<String, String> getCommandParams() {
			return commandParams;
		}
		public void setCommandParams(Map<String, String> commandParams) {
			this.commandParams = commandParams;
		}
		public byte[] getCommandData() {
			return this.commandData;
		}
		public void setCommandData(byte[] commandData) {
			this.commandData = commandData;
		}    	
		/** End setter/getter methods*/

	}

}
