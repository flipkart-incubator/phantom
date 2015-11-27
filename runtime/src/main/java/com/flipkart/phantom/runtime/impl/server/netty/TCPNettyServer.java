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
package com.flipkart.phantom.runtime.impl.server.netty;

import com.flipkart.phantom.runtime.impl.server.concurrent.NamedThreadFactory;
import org.jboss.netty.bootstrap.Bootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import java.util.concurrent.*;

/**
 * <code>TCPNettyServer</code> is a concrete implementation of the {@link AbstractNettyNetworkServer} for {@link TRANSMISSION_PROTOCOL#TCP}
 *
 * @author Regunath B
 * @version 1.0, 15 Mar 2013
 */
public class TCPNettyServer extends AbstractNettyNetworkServer {

    /** The default counts (invalid one) for server and worker pool counts*/
    private static final int INVALID_POOL_SIZE = -1;

    /** The server and worker thread pool sizes*/
    private int serverPoolSize = INVALID_POOL_SIZE;
    private int workerPoolSize = INVALID_POOL_SIZE;
    private int executorQueueSize = Runtime.getRuntime().availableProcessors() * 12;

    /** The server and worker ExecutorService instances*/
    private ExecutorService serverExecutors;
    private ExecutorService workerExecutors;

    /** Server Type */
    private String serverType = "TCP Netty Server";


    /**
     * Interface method implementation. Returns {@link TRANSMISSION_PROTOCOL#TCP}
     * @see com.flipkart.phantom.runtime.spi.server.NetworkServer#getTransmissionProtocol()
     */
    public TransmissionProtocol getTransmissionProtocol() {
        return TRANSMISSION_PROTOCOL.TCP;
    }

    /**
     * Interface method implementation. Creates server and worker thread pools if required and then calls {@link #afterPropertiesSet()} on the super class
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        if (this.getServerExecutors() == null) { // no executors have been set for server listener
            if (this.getServerPoolSize() != TCPNettyServer.INVALID_POOL_SIZE) { // thread pool size has ot been set.
               this.setServerExecutors(new ThreadPoolExecutor(this.getServerPoolSize(),
                    this.getServerPoolSize(),
                    60,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>(this.getExecutorQueueSize()),
                    new NamedThreadFactory("TCPServer-Listener"),
                    new ThreadPoolExecutor.CallerRunsPolicy()));
            }
            else { // default behavior of creating and using a cached thread pool
				this.setServerExecutors(Executors.newCachedThreadPool(new NamedThreadFactory("TCPServer-Listener")));
			}
        }
        if (this.getWorkerExecutors() == null) {  // no executors have been set for workers
            if (this.getWorkerPoolSize() != TCPNettyServer.INVALID_POOL_SIZE) { // thread pool size has not been set.
                this.setWorkerExecutors(new ThreadPoolExecutor(this.getWorkerPoolSize(),
                        this.getWorkerPoolSize(),
                        60,
                        TimeUnit.SECONDS,
                        new LinkedBlockingQueue<Runnable>(this.getExecutorQueueSize()),
                        new NamedThreadFactory("TCPServer-Worker"),
                        new ThreadPoolExecutor.CallerRunsPolicy()));
            } else { // default behavior of creating and using a cached thread pool
                this.setWorkerExecutors(Executors.newCachedThreadPool(new NamedThreadFactory("TCPServer-Worker")));
            }
        }
        super.afterPropertiesSet();
    }

    /**
     * Overriden super class method. Returns a readable string for this TCPNettyServer
     * @see java.lang.Object#toString()
     */
    public String toString(){
        return "TCPNettyServer [socketAddress=" + socketAddress + ", portNumber=" + portNumber + "] " + this.getPipelineFactory();
    }

    /**
     * Interface method implementation. Creates and returns a Netty ServerBootstrap instance
     * @see com.flipkart.phantom.runtime.impl.server.netty.AbstractNettyNetworkServer#createServerBootstrap()
     */
    protected Bootstrap createServerBootstrap() throws RuntimeException {
    	if (this.getWorkerPoolSize() != TCPNettyServer.INVALID_POOL_SIZE) { // specify the worker count if it has been set, else use defaults (Netty uses 2 * no. of cores)
    		return new ServerBootstrap(new NioServerSocketChannelFactory(this.getServerExecutors(), this.getWorkerExecutors(), this.getWorkerPoolSize()));    		
    	} else {
    		return new ServerBootstrap(new NioServerSocketChannelFactory(this.getServerExecutors(), this.getWorkerExecutors()));
    	}
    }

    /**
     * Abstract method implementation. Creates and returns a Netty Channel from the ServerBootstrap that was
     * previously created in {@link TCPNettyServer#createServerBootstrap()}
     * @see com.flipkart.phantom.runtime.impl.server.netty.AbstractNettyNetworkServer#createChannel()
     */
    protected Channel createChannel() throws RuntimeException {
        if (this.getServerBootstrap() == null) {
            throw new RuntimeException("Error creating Channel. Bootstrap instance cannot be null. See TCPNettyServer#createServerBootstrap()");
        }
        return ((ServerBootstrap)this.serverBootstrap).bind(this.socketAddress);
    }

    /**
     * Abstract method implementation. Returns server type as string.
     */
    public String getServerType() {
        return  this.serverType;
    }

    public void setServerType(String serverType) {
        this.serverType = serverType;
    }

    /**
     * Abstract method implementation. Returns server endpoint as string.
     */
    public String getServerEndpoint() {
        return ""+this.portNumber;
    }

    /** Start Getter/Setter methods */
    public int getServerPoolSize() {
        return this.serverPoolSize;
    }
    public void setServerPoolSize(int serverPoolSize) {
        this.serverPoolSize = serverPoolSize;
    }
    public int getWorkerPoolSize() {
        return this.workerPoolSize;
    }
    public void setWorkerPoolSize(int workerPoolSize) {
        this.workerPoolSize = workerPoolSize;
    }
    public ExecutorService getServerExecutors() {
        return this.serverExecutors;
    }
    public void setServerExecutors(ExecutorService serverExecutors) {
        this.serverExecutors = serverExecutors;
    }
    public ExecutorService getWorkerExecutors() {
        return this.workerExecutors;
    }
    public void setWorkerExecutors(ExecutorService workerExecutors) {
        this.workerExecutors = workerExecutors;
    }
    public int getExecutorQueueSize() {
        return executorQueueSize;
    }
    public void setExecutorQueueSize(int executorQueueSize) {
        this.executorQueueSize = executorQueueSize;
    }
    /** End Getter/Setter methods */
}
