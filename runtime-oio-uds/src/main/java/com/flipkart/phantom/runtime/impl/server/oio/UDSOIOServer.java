/*
 * Copyright 2012-2015, Flipkart Internet Pvt Ltd. All rights reserved.
 * 
 * This software is the confidential and proprietary information of Flipkart Internet Pvt Ltd. ("Confidential Information").  
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of the license 
 * agreement you entered into with Flipkart.    
 * 
 */
package com.flipkart.phantom.runtime.impl.server.oio;

import com.flipkart.phantom.runtime.impl.server.AbstractNetworkServer;
import com.flipkart.phantom.runtime.impl.server.concurrent.NamedThreadFactory;
import com.flipkart.phantom.runtime.impl.server.netty.handler.command.CommandInterpreter;
import com.flipkart.phantom.task.impl.TaskHandlerExecutor;
import com.flipkart.phantom.task.impl.TaskHandlerExecutorRepository;
import com.flipkart.phantom.task.impl.TaskResult;
import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.springframework.util.Assert;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.trpr.platform.runtime.impl.config.FileLocator;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <code>UDSOIOServer</code> is a concrete implementation of the {@link AbstractNetworkServer} 
 * for Unix Domain Sockets. Note that this server has to be initialized with a UDS socket file rather than a port no.
 * 
 * @author Regunath B
 * @version 1.0, 25 Jun 2013
 */
public class UDSOIOServer extends AbstractNetworkServer {
	
	/** Logger for this class*/
	private static final Logger LOGGER = LoggerFactory.getLogger(UDSOIOServer.class);

	/** The default counts (invalid one) for worker pool count*/
	private static final int INVALID_POOL_SIZE = -1;
	
	/** The default timeout for client socket inactivity*/
	private int DEFAULT_CLIENT_TIMEOUT_MILLIS = 300;

	/** The default directory name containing junix native libraries*/
	private static final String DEFAULT_JUNIX_NATIVE_DIRECTORY = "uds-lib";

	/** The System property to be set with Junix native lib path*/
	private static final String JUNIX_LIB_SYSTEM_PROPERTY = "org.newsclub.net.unix.library.path";

	/** The worker thread pool sizes*/
	private int workerPoolSize = INVALID_POOL_SIZE;
	
	/** The client socket inactivity timeout in millis*/
	private int clientSocketTimeoutMillis = DEFAULT_CLIENT_TIMEOUT_MILLIS;

	/** The worker ExecutorService instances*/
	private ExecutorService workerExecutors;

	/** The directory name containing junix native libraries*/
	private String junixNativeLibDirectoryName = DEFAULT_JUNIX_NATIVE_DIRECTORY;
	
	/** The name of the socket file for this server (UDS) */
	private String socketName;
	
	/** The directory containing the socket file */
	private String socketDir;

	/** The socket file */
	private File socketFile;
	
    /** The UNIX domain socket instance */
	public AFUNIXServerSocket socket;	
	
	/** The TaskRepository to lookup TaskHandlerExecutors from */
	private TaskHandlerExecutorRepository repository;	
	
	/**
	 * Interface method implementation. Returns {@link TRANSMISSION_PROTOCOL#UDS} (Unix domain Sockets)
	 * @see com.flipkart.phantom.runtime.spi.server.NetworkServer#getTransmissionProtocol()
	 */
	public TransmissionProtocol getTransmissionProtocol() {
		return TRANSMISSION_PROTOCOL.UDS;
	}

	/**
	 * Interface method implementation. Creates worker thread pool if required and then calls {@link #afterPropertiesSet()} on the super class
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {	
		File[] junixDirectories = FileLocator.findDirectories(this.junixNativeLibDirectoryName, null);
		if(junixDirectories==null || junixDirectories.length==0) {
			throw new RuntimeException("Did not find junixDirectory: "+junixNativeLibDirectoryName);
		}
		LOGGER.info("Found junixDirectory: "+junixDirectories[0].getAbsolutePath());
        System.setProperty(JUNIX_LIB_SYSTEM_PROPERTY,junixDirectories[0].getAbsolutePath());
        //Required properties
		Assert.notNull(this.socketDir, "socketDir is a required property for UDSNetworkServer");
		Assert.notNull(this.socketName, "socketName is a required property for UDSNetworkServer");
		//Create the socket file
		this.socketFile = new File(new File(this.socketDir), this.socketName);
		
		//Create socket address
		LOGGER.info("Socket file: "+this.socketFile.getAbsolutePath());
		try {
			this.socketAddress = new AFUNIXSocketAddress(this.socketFile);
			this.socket = AFUNIXServerSocket.newInstance();
			this.socket.bind(this.socketAddress);
		} catch (IOException e) {
			throw new RuntimeException("Error creating Socket Address. ",e);
		}
		if (this.getWorkerExecutors() == null) {  // no executors have been set for workers
			if (this.getWorkerPoolSize() != UDSOIOServer.INVALID_POOL_SIZE) { // thread pool size has been set. create and use a fixed thread pool
				this.setWorkerExecutors(Executors.newFixedThreadPool(this.getWorkerPoolSize(), new NamedThreadFactory("UDSOIOServer-Worker")));
			}else { // default behavior of creating and using a cached thread pool
				this.setWorkerExecutors(Executors.newCachedThreadPool(new NamedThreadFactory("UDSOIOServer-Worker")));
			}
		}
		super.afterPropertiesSet();
		LOGGER.info("UDS Server startup complete");
	}

	/**
	 * Overriden super class method. Returns a readable string for this UDSNetworkServer
	 * @see java.lang.Object#toString()
	 */
	public String toString(){
		return "UDSOIONetworkServer [socketFile=" + socketFile.getAbsolutePath() + "] ";
	}

	/**
	 * Overriden superclass method. Starts up a ServerSocket for listening to client connection requests
	 * @see com.flipkart.phantom.runtime.impl.server.AbstractNetworkServer#doStartServer()
	 */
	protected void doStartServer() throws RuntimeException {
		new SocketListener();
	}

	/**
	 * Overriden superclass method. Shuts down the ServerSocket and stops accepting any new client connection requests
	 * @see com.flipkart.phantom.runtime.impl.server.AbstractNetworkServer#doStopServer()
	 */
	protected void doStopServer() throws RuntimeException {		
		try {
			this.socket.close();
			this.workerExecutors.shutdown();
		} catch (IOException e) {
			throw new RuntimeException("Error shutting down UDS server : " + this.toString(), e);
		}
	}

    @Override
    public String getServerType() {
        return "UDS OIO Server";
    }

    @Override
    public String getServerEndpoint() {
        return this.socketDir + this.socketFile;
    }

    /**
	 * The Socket listener thread
	 */
	class SocketListener extends Thread {
		SocketListener() {
			this.setName("UDSOIO_Listener");
			this.start();
		}
		public void run() {
			while(true) {
				Socket client = null;
				try {
					client = socket.accept();
					client.setSoTimeout(getClientSocketTimeoutMillis()); // set this timeout to protect server from clients that become inactive
					workerExecutors.execute(new CommandProcessor(client));
				} catch (IOException e) {
					throw new RuntimeException("Error accepting client socket connections : " + e.getMessage(), e);
				} 
			}			
		}
	}
	
	/**
	 * Helper class that reads and processes Commands from a client Socket. This runs inside a Worker thread.
	 */
	class CommandProcessor implements Runnable {
		Socket client;
		CommandProcessor(Socket client) {
			this.client = client;
		}
		public void run() {
			try {
				CommandInterpreter commandInterpreter = new CommandInterpreter();
				CommandInterpreter.ProxyCommand readCommand = commandInterpreter.readCommand(client.getInputStream());	
				LOGGER.debug("Read Command : " + readCommand);
				String pool = readCommand.getCommandParams().get("pool");
				TaskHandlerExecutor executor;
				//Try to execute command using ThreadPool, if "pool" is found in the command, else the command name
				if(pool!=null) {
					executor = repository.get(readCommand.getCommand(),pool);
				} else {
					executor = repository.get(readCommand.getCommand(),readCommand.getCommand());
				}
				executor.setParams(readCommand.getCommandParams());
				executor.setData(readCommand.getCommandData());
				TaskResult result = executor.execute();
				LOGGER.debug("The output is: "+ result);
				// write the results to the socket output
				commandInterpreter.writeCommandExecutionResponse(client.getOutputStream(), result);				            
			} catch(Exception e) {
				LOGGER.error("Error in processing command : " + e.getMessage(), e);
				throw new RuntimeException("Error in processing command : " + e.getMessage(), e);
			} finally {
				if (client !=null) {
					try {
						client.close();
					} catch (IOException e) {
						LOGGER.error("Error closing client socket : " + e.getMessage(), e);
					}					
				}
			}
		}		
	}

	/** Start Getter/Setter methods */
	public int getWorkerPoolSize() {
		return this.workerPoolSize;
	}
	public void setWorkerPoolSize(int workerPoolSize) {
		this.workerPoolSize = workerPoolSize;
	}	
	public int getClientSocketTimeoutMillis() {
		return this.clientSocketTimeoutMillis;
	}
	public void setClientSocketTimeoutMillis(int clientSocketTimeoutMillis) {
		this.clientSocketTimeoutMillis = clientSocketTimeoutMillis;
	}
	public ExecutorService getWorkerExecutors() {
		return this.workerExecutors;
	}
	public void setWorkerExecutors(ExecutorService workerExecutors) {
		this.workerExecutors = workerExecutors;
	}	
	public String getSocketDir() {
		return socketDir;
	}
	public void setSocketDir(String socketDir) {
		this.socketDir = socketDir;
	}
	public String getSocketName() {
		return socketName;
	}
	public void setSocketName(String socketName) {
		this.socketName = socketName;
	}
	public String getJunixNativeLibDirectoryName() {
		return junixNativeLibDirectoryName;
	}
	public void setJunixNativeLibDirectoryName(String junixNativeLibDirectoryName) {
		this.junixNativeLibDirectoryName = junixNativeLibDirectoryName;
	}
	public TaskHandlerExecutorRepository getRepository() {
		return this.repository;
	}
	public void setRepository(TaskHandlerExecutorRepository repository) {
		this.repository = repository;
	}	
	/** End Getter/Setter methods */
	
}
