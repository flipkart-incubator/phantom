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
package com.flipkart.phantom.runtime.impl.server.oio;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.trpr.platform.runtime.impl.config.FileLocator;

import com.flipkart.phantom.event.ServiceProxyEvent;
import com.flipkart.phantom.event.ServiceProxyEventProducer;
import com.flipkart.phantom.runtime.impl.server.AbstractNetworkServer;
import com.flipkart.phantom.runtime.impl.server.concurrent.NamedThreadFactory;
import com.flipkart.phantom.runtime.impl.server.netty.handler.command.CommandInterpreter;
import com.flipkart.phantom.task.impl.TaskHandler;
import com.flipkart.phantom.task.impl.TaskHandlerExecutor;
import com.flipkart.phantom.task.impl.collector.EventDispatchingSpanCollector;
import com.flipkart.phantom.task.impl.interceptor.ServerRequestInterceptor;
import com.flipkart.phantom.task.spi.RequestContext;
import com.flipkart.phantom.task.spi.TaskRequestWrapper;
import com.flipkart.phantom.task.spi.TaskResult;
import com.flipkart.phantom.task.spi.repository.ExecutorRepository;
import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.FixedSampleRateTraceFilter;
import com.github.kristofa.brave.ServerSpan;
import com.github.kristofa.brave.ServerTracer;
import com.github.kristofa.brave.TraceFilter;
import com.google.common.base.Optional;

/**
 * <code>UDSOIOServer</code> is a concrete implementation of the {@link AbstractNetworkServer}
 * for Unix Domain Sockets. Note that this server has to be initialized with a UDS socket file rather than a port no.
 *
 * @author Regunath B
 * @version 1.0, 25 Jun 2013
 */
@SuppressWarnings("rawtypes")
public class UDSOIOServer extends AbstractNetworkServer {

    /** Logger for this class*/
    private static final Logger LOGGER = LoggerFactory.getLogger(UDSOIOServer.class);

    /** The default counts (invalid one) for worker pool count*/
    private static final int INVALID_POOL_SIZE = -1;

    /** The default name of the server/service this channel handler is serving*/
    private static final String DEFAULT_SERVICE_NAME = "UDS OIO Server";
    
    /** Event Type for publishing all events which are generated here */
    private final static String COMMAND_HANDLER = "COMMAND_HANDLER";
    
    /** The default value for tracing frequency. This value indicates that tracing if OFF*/
    private static final TraceFilter NO_TRACING = new FixedSampleRateTraceFilter(-1);    
    
    /** The default timeout for client socket inactivity*/
    private int DEFAULT_CLIENT_TIMEOUT_MILLIS = 300;

    /** The default directory name containing junix native libraries*/
    private static final String DEFAULT_JUNIX_NATIVE_DIRECTORY = "uds-lib";

    /** The System property to be set with Junix native lib path*/
    private static final String JUNIX_LIB_SYSTEM_PROPERTY = "org.newsclub.net.unix.library.path";

	/** Default host name and port where this ChannelHandler is available */
	public static final String DEFAULT_HOST = "localhost"; // unresolved local host name
	public static final int DEFAULT_PORT = -1; // no valid port really
    
    /** The local host name value*/
    private static String hostName = DEFAULT_HOST;
    static {
    	try {
			hostName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			LOGGER.warn("Unable to resolve local host name. Will use default host name : " + DEFAULT_HOST);
		}
    }
    
    /** The name for the service/server*/
    private String serviceName = DEFAULT_SERVICE_NAME;
    
    /** The port where the server for this handler is listening on*/
    private int hostPort;
    
    /** The worker thread pool sizes*/
    private int workerPoolSize = INVALID_POOL_SIZE;

    /** The worker thread pool executor queue size*/
    private int executorQueueSize = Runtime.getRuntime().availableProcessors() * 12;

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
	private ExecutorRepository<TaskRequestWrapper,TaskResult, TaskHandler> repository;

    /** The publisher used to broadcast events to Service Proxy Subscribers */
    private ServiceProxyEventProducer eventProducer;

    /** The request tracing frequency for this channel handler*/
    private TraceFilter traceFilter = NO_TRACING;	
    
    /** The EventDispatchingSpanCollector instance used in tracing requests*/
    private EventDispatchingSpanCollector eventDispatchingSpanCollector;    
    
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
        Assert.notNull(this.eventDispatchingSpanCollector, "The 'eventDispatchingSpanCollector' may not be null");
        
        //Create the socket file
        this.socketFile = new File(new File(this.socketDir), this.socketName);

        //Create socket address
        LOGGER.info("Socket file: "+this.socketFile.getAbsolutePath());
        try {
            this.socketAddress = new AFUNIXSocketAddress(this.socketFile);
            this.socket = AFUNIXServerSocket.newInstance();
            this.socket.bind(this.socketAddress);
            this.hostPort = this.socketAddress.getPort();
        } catch (IOException e) {
            throw new RuntimeException("Error creating Socket Address. ",e);
        }
        if (this.getWorkerExecutors() == null) {  // no executors have been set for workers
            if (this.getWorkerPoolSize() != UDSOIOServer.INVALID_POOL_SIZE) { // thread pool size has  been set
                this.setWorkerExecutors(new ThreadPoolExecutor(this.getWorkerPoolSize(),
                        this.getWorkerPoolSize(),
                        60,
                        TimeUnit.SECONDS,
                        new LinkedBlockingQueue<Runnable>(this.getExecutorQueueSize()),
                        new NamedThreadFactory("UDSOIOServer-Worker"),
                        new ThreadPoolExecutor.CallerRunsPolicy()));
            }else { //
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
        return this.socketFile.toString();
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
                    workerExecutors.submit(new CommandProcessor(client));
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
            long receiveTime = System.currentTimeMillis();
            TaskHandlerExecutor<byte[]> executor = null;
            CommandInterpreter.ProxyCommand readCommand = null;
            Optional<RuntimeException> transportError = Optional.absent();
            TaskResult result = null;
            ServerRequestInterceptor<TaskRequestWrapper, TaskResult> serverRequestInterceptor = null;
            try {
                CommandInterpreter commandInterpreter = new CommandInterpreter();
                readCommand = commandInterpreter.readCommand(client.getInputStream());
                LOGGER.debug("Read Command : " + readCommand);
                String pool = readCommand.getCommandParams().get("pool");

                // Prepare the request Wrapper
                TaskRequestWrapper<byte[]> taskRequestWrapper = new TaskRequestWrapper<byte[]>();
                taskRequestWrapper.setCommandName(readCommand.getCommand());
                taskRequestWrapper.setData(readCommand.getCommandData());
                taskRequestWrapper.setParams(readCommand.getCommandParams());
                // set the service name for the request
                taskRequestWrapper.setServiceName(Optional.of(serviceName));

                // Create and process a Server request interceptor. This will initialize the server tracing
                serverRequestInterceptor = initializeServerTracing(taskRequestWrapper);

                /*Try to execute command using ThreadPool, if "pool" is found in the command, else the command name */
                if (pool != null) {
                    executor = (TaskHandlerExecutor) repository.getExecutor(readCommand.getCommand(), pool, taskRequestWrapper);
                } else {
                    executor = (TaskHandlerExecutor) repository.getExecutor(readCommand.getCommand(), readCommand.getCommand(), taskRequestWrapper);
                }
                /* execute */
                if (executor.getCallInvocationType() == TaskHandler.SYNC_CALL) {
                    result = executor.execute();
                } else {
                    /* dont wait for the result. send back a response that the call has been dispatched for async execution */
                    executor.queue();
                    result = new TaskResult(true, TaskHandlerExecutor.ASYNC_QUEUED);
                }
                LOGGER.debug("The output is: " + result);

                // write the results to the socket output
                commandInterpreter.writeCommandExecutionResponse(client.getOutputStream(), result);
            } catch (Exception e) {
            	RuntimeException runtimeException = new RuntimeException("Error in executing command : " + readCommand, e);
            	transportError = Optional.of(runtimeException);
                throw runtimeException;
            } finally {
            	// finally inform the server request tracer
            	if (serverRequestInterceptor != null) {
            		serverRequestInterceptor.process(result, transportError);
            	}
                if (eventProducer != null) {
                    // Publishes event both in case of success and failure.
                    final Map<String, String> params = readCommand.getCommandParams();
                    ServiceProxyEvent.Builder eventBuilder;
                    if(executor==null) {
                        eventBuilder = new ServiceProxyEvent.Builder(readCommand.getCommand(), COMMAND_HANDLER).withEventSource(getClass().getName());
                    } else {
                        eventBuilder = executor.getEventBuilder().withCommandData(executor).withEventSource(executor.getClass().getName());
                    }
                    eventBuilder.withRequestId(params.get("requestID")).withRequestReceiveTime(receiveTime);
                    if(params.containsKey("requestSentTime")) {
                        eventBuilder.withRequestSentTime(Long.valueOf(params.get("requestSentTime")));
                    }
                    eventProducer.publishEvent(eventBuilder.build());
                } else {
                    LOGGER.debug("eventProducer not set, not publishing event");
                }
                if (client != null) {
                    try {
                        client.close();
                    } catch (IOException e) {
                        LOGGER.error("Error closing client socket : " + e.getMessage(), e);
                    }
                }
            }
        }
    }

    /**
     * Initializes server tracing for the specified request
     * @param executorHttpRequest the Http request 
     * @return the initialized ServerRequestInterceptor
     */
    private ServerRequestInterceptor<TaskRequestWrapper, TaskResult> initializeServerTracing(TaskRequestWrapper executorRequest) {
		// set the server request context on the received request
    	ServerSpan serverSpan = Brave.getServerSpanThreadBinder().getCurrentServerSpan();
    	RequestContext serverRequestContext = new RequestContext();
    	serverRequestContext.setCurrentServerSpan(serverSpan);	
    	executorRequest.setRequestContext(Optional.of(serverRequestContext));
        ServerRequestInterceptor<TaskRequestWrapper, TaskResult> serverRequestInterceptor = new ServerRequestInterceptor<TaskRequestWrapper, TaskResult>();
    	List<TraceFilter> traceFilters = Arrays.<TraceFilter>asList(this.traceFilter);    
    	ServerTracer serverTracer = Brave.getServerTracer(this.eventDispatchingSpanCollector, traceFilters);
    	serverRequestInterceptor.setEndPointSubmitter(Brave.getEndPointSubmitter());
        serverRequestInterceptor.setServerTracer(serverTracer);
        serverRequestInterceptor.setServiceHost(UDSOIOServer.hostName);
        serverRequestInterceptor.setServicePort(this.hostPort);
        serverRequestInterceptor.setServiceName(this.serviceName);   
        // now process the request to initialize tracing
        serverRequestInterceptor.process(executorRequest); 
        return serverRequestInterceptor;
    }
    
    /** Start Getter/Setter methods */
    public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}	
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
    public ExecutorRepository getRepository() {
        return this.repository;
    }
    public void setRepository(ExecutorRepository<TaskRequestWrapper,TaskResult, TaskHandler> repository) {
        this.repository = repository;
    }
    public void setEventProducer(ServiceProxyEventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }
    public int getExecutorQueueSize() {
        return executorQueueSize;
    }
    public void setExecutorQueueSize(int executorQueueSize) {
        this.executorQueueSize = executorQueueSize;
    }
	public void setTraceFilter(TraceFilter traceFilter) {
		this.traceFilter = traceFilter;
	}
	public void setEventDispatchingSpanCollector(EventDispatchingSpanCollector eventDispatchingSpanCollector) {
		this.eventDispatchingSpanCollector = eventDispatchingSpanCollector;
	}    
    /** End Getter/Setter methods */

}
