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

import com.flipkart.phantom.task.spi.TaskContext;
import com.netflix.hystrix.*;

import org.apache.thrift.ProcessFunction;
import org.apache.thrift.TBase;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * <code>ThriftProxyExecutor</code> is an extension of {@link com.netflix.hystrix.HystrixCommand}. It is essentially a
 * wrapper around {@link ThriftProxy}, providing a means for the ThriftProxy to to be called using
 * a Hystrix Command.
 *
 * @author Regunath B
 * @version 1.0, 28 March, 2013
 */
public class ThriftProxyExecutor extends HystrixCommand<TTransport> {

	/** Thrift transport errors */
	private static final Map<Integer, String> THRIFT_ERRORS = new HashMap<Integer, String>();
	static {
		THRIFT_ERRORS.put(TTransportException.UNKNOWN,"Unknown exception");
		THRIFT_ERRORS.put(TTransportException.NOT_OPEN,"Transport not open");
		THRIFT_ERRORS.put(TTransportException.ALREADY_OPEN,"Transport already open");
		THRIFT_ERRORS.put(TTransportException.TIMED_OUT,"Thrift timed out");
		THRIFT_ERRORS.put(TTransportException.END_OF_FILE,"Reached end of file");
	}

	/** Logger for this class*/
	private static final Logger LOGGER = LoggerFactory.getLogger(ThriftProxyExecutor.class);

	/** The default Hystrix group to which the command belongs, unless otherwise mentioned*/
	public static final String DEFAULT_HYSTRIX_GROUP = "defaultThriftGroup";

	/** The default Hystrix Thread pool to which the command belongs, unless otherwise mentioned */
	public static final String DEFAULT_HYSTRIX_THREAD_POOL = "defaultThriftThreadPool";
	
	/** The default Hystrix Thread pool to which this command belongs, unless otherwise mentioned */
	public static final int DEFAULT_HYSTRIX_THREAD_POOL_SIZE = 20;	

	/** The default Thrift call result class name */
	private static final String DEFAULT_RESULT_CLASS_NAME="_result";

	/** The {@link ThriftProxy} or {@link ThriftProxy} instance which this Command wraps around */
	protected ThriftProxy thriftProxy;

	/** The TaskContext instance*/
	protected TaskContext taskContext;

	/** The client's TTransport*/
	protected TTransport clientTransport;

	/** The Thrift binary protocol factory*/
	private TProtocolFactory protocolFactory =  new TBinaryProtocol.Factory();

	/**
	 * Constructor for this class.
	 * @param hystrixThriftProxy the HystrixThriftProxy that must be wrapped by Hystrix
	 * @param taskContext the TaskContext instance that manages the proxies
	 * @param commandName the Hystrix command name.
	 */
	protected ThriftProxyExecutor(HystrixThriftProxy hystrixThriftProxy, TaskContext taskContext, String commandName) {
		super(constructHystrixSetter(hystrixThriftProxy,commandName));
		this.thriftProxy = hystrixThriftProxy;
		this.taskContext = taskContext;
	}

	/**
	 * Interface method implementation. @see HystrixCommand#run()
	 */
	@SuppressWarnings("rawtypes")
	@Override
	protected TTransport run() {
		// not using Thrift pooled sockets
		//TSocket serviceSocket = this.thriftProxy.getPooledSocket();
		//boolean isConnectionValid = true;

		TSocket serviceSocket = null;
        try {

			//Get Protocol from transport
			TProtocol clientProtocol = this.protocolFactory.getProtocol(clientTransport);
			TMessage message = clientProtocol.readMessageBegin();
			//Arguments
			ProcessFunction invokedProcessFunction = this.thriftProxy.getProcessMap().get(message.name);
			if (invokedProcessFunction == null) {
				throw new RuntimeException("Unable to find a matching ProcessFunction for invoked method : " + message.name);
			}
			TBase args = invokedProcessFunction.getEmptyArgsInstance(); // get the empty args. The values will then be read from the client's TProtocol
			//Read the argument values from the client's TProtocol
			args.read(clientProtocol);
			clientProtocol.readMessageEnd();

			// Instantiate the call result object using the Thrift naming convention used for classes
			TBase result = (TBase) Class.forName( this.thriftProxy.getThriftServiceClass() + "$" + message.name + DEFAULT_RESULT_CLASS_NAME).newInstance();
			
        	serviceSocket = new TSocket(this.thriftProxy.getThriftServer(), this.thriftProxy.getThriftPort(), this.thriftProxy.getThriftTimeoutMillis());
	        TProtocol serviceProtocol = new TBinaryProtocol(serviceSocket);
    		serviceSocket.open();

			//Send the arguments to the server and relay the response back
			//Create the custom TServiceClient client which sends request to actual Thrift servers and relays the response back to the client
			ProxyServiceClient proxyClient = new ProxyServiceClient(clientProtocol,serviceProtocol,serviceProtocol);

			//Send the request
			proxyClient.sendBase(message.name, args, message.seqid);
			//Get the response back (it is written to client's TProtocol)
			proxyClient.receiveBase(result, message.name);

			LOGGER.debug("Processed message : " + this.thriftProxy.getThriftServiceClass() + "." + message.name);

		} catch (Exception e) {
			if (e.getClass().isAssignableFrom(TTransportException.class)) {
				//isConnectionValid = false;
				throw new RuntimeException("Thrift transport exception executing the proxy service call : " + THRIFT_ERRORS.get(((TTransportException)e).getType()), e);
			} else {
				throw new RuntimeException("Exception executing the proxy service call : " + e.getMessage(), e);
			}
		} finally {
			if (serviceSocket != null) {			
	            //this.thriftProxy.returnPooledSocket(serviceSocket, isConnectionValid);
				serviceSocket.close();
			}
		}
		return this.clientTransport;
	}

	/**
	 * Interface method implementation. @see HystrixCommand#getFallback()
	 */
	@Override
	protected TTransport getFallback() {
		if(this.thriftProxy instanceof HystrixThriftProxy) {
			HystrixThriftProxy hystrixThriftProxy = (HystrixThriftProxy) this.thriftProxy;
			hystrixThriftProxy.fallbackThriftRequest(this.clientTransport,this.taskContext);
			return this.clientTransport;
		}
		return null;
	}

	/**
	 * Helper method that constructs the {@link com.netflix.hystrix.HystrixCommand.Setter} according to the properties set in the {@link ThriftProxy}.
	 * Falls back to default properties, if any of them has not been set
	 * 
	 * @param hystrixThriftProxy The {@link ThriftProxy} for whom properties have to be set
	 * @return Setter
	 */
	private static Setter constructHystrixSetter(HystrixThriftProxy hystrixThriftProxy, String commandName) {
		Setter setter = null;
		if((hystrixThriftProxy.getGroupName()!=null) || !hystrixThriftProxy.getGroupName().equals("")) {
			setter = Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(hystrixThriftProxy.getGroupName()));
		} else {
			setter = Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(hystrixThriftProxy.getName()));
		}
		setter = setter.andCommandKey(HystrixCommandKey.Factory.asKey(commandName));
		if((hystrixThriftProxy.getThreadPoolName()!=null) || !hystrixThriftProxy.getThreadPoolName().equals("")) {
			setter = setter.andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(hystrixThriftProxy.getThreadPoolName()));
		} else {
			setter = setter.andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(DEFAULT_HYSTRIX_THREAD_POOL));
		}
		Integer threadPoolSize = hystrixThriftProxy.getProxyThreadPoolSize() == null ? ThriftProxyExecutor.DEFAULT_HYSTRIX_THREAD_POOL_SIZE :  hystrixThriftProxy.getProxyThreadPoolSize();
		setter = setter.andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter().withCoreSize(threadPoolSize));
		setter = setter.andCommandPropertiesDefaults(HystrixCommandProperties.Setter().withExecutionIsolationThreadTimeoutInMilliseconds(hystrixThriftProxy.getExecutorTimeout(commandName)));
		if((hystrixThriftProxy.getHystrixProperties()!=null)) {
			setter = setter.andCommandPropertiesDefaults(hystrixThriftProxy.getHystrixProperties());
		}
		return setter;
	}

	/**Getter/Setter methods */	
	public TTransport getClientTransport() {
		return this.clientTransport;
	}
	public void setClientTransport(TTransport clientTransport) {
		this.clientTransport = clientTransport;
	}	
	/** End Getter/Setter methods */	

}
