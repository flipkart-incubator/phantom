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
package com.flipkart.phantom.thrift.impl;

import org.apache.thrift.TApplicationException;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TProtocol;
import org.trpr.platform.core.impl.logging.LogFactory;
import org.trpr.platform.core.spi.logging.Logger;

/**
 * <code>ProxyServiceClient</code> is a sub-type of the Thrift {@link org.apache.thrift.TServiceClient} that retains the sequence ID of the calling message when
 * invoking the target thrift service. It additionally writes the TBase result from a call invocation into the invoking client's {@link org.apache.thrift.protocol.TProtocol} instance
 * i.e. relays the service response to the client as is in the Thrift protocol format.
 *
 * @author devashishshankar
 * @author Regunath B
 * @version 1.0, 28 Mar 2013
 */
public class ProxyServiceClient extends TServiceClient {

	/** Logger for this class*/
	private static final Logger LOGGER = LogFactory.getLogger(ProxyServiceClient.class);

	/** The client's TProtocol instance to relay the response to*/
	private TProtocol clientProtocol;

	/**
	 * Constructor for this class
	 * @param clientProtocol the client's TProtocol instance to relay the service response to
	 * @param iprot the incoming TProtocol from the service
	 * @param oprot the outgoing TProtocol to the service
	 */
	public ProxyServiceClient(TProtocol clientProtocol, TProtocol iprot, TProtocol oprot) {
		super(iprot, oprot);
		this.clientProtocol = clientProtocol;
	}

	/**
	 * Overriden super class method. Simply delegates the call to super type implementation
	 * @see org.apache.thrift.TServiceClient#sendBase(String, org.apache.thrift.TBase)
	 */
	public void sendBase(String methodName, TBase args, int sequenceId) throws TException {
		this.seqid_ = sequenceId;
	    oprot_.writeMessageBegin(new TMessage(methodName, TMessageType.CALL, this.seqid_));
	    args.write(oprot_);
	    oprot_.writeMessageEnd();
	    oprot_.getTransport().flush();
	}

	/**
	 * Overriden super class method. Receives the service response and relays it back to the client
	 * @see org.apache.thrift.TServiceClient#receiveBase(org.apache.thrift.TBase, String)
	 */
	public void receiveBase(TBase result, String methodName) throws TException {
		// Read the service response - same as in TServiceClient#receiveBase
	    TMessage msg = iprot_.readMessageBegin();
	    if (msg.type == TMessageType.EXCEPTION) {
	      TApplicationException x = TApplicationException.read(iprot_);
	      iprot_.readMessageEnd();
	      throw x;
	    }
	    if (msg.seqid != this.seqid_) {
	      throw new TApplicationException(TApplicationException.BAD_SEQUENCE_ID, methodName + " failed: out of sequence response");
	    }
	    result.read(iprot_);
	    iprot_.readMessageEnd();	
	    
	    // now relay the response to the client
	    clientProtocol.writeMessageBegin(msg);
	    result.write(clientProtocol);
	    clientProtocol.writeMessageEnd();
	    clientProtocol.getTransport().flush();
	    LOGGER.debug("Relayed thrift response to client. Seq Id : " + msg.seqid + ", Method : " +msg.name + ", value : " + result);	    
	}
		 
}
