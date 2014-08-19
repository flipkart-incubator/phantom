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
            if (this.getServerPoolSize() == TCPNettyServer.INVALID_POOL_SIZE) { // thread pool size has been set. create and use a fixed thread pool
                this.setServerPoolSize(Runtime.getRuntime().availableProcessors());
            }
            this.setServerExecutors(new ThreadPoolExecutor(this.getServerPoolSize(),
                    this.getServerPoolSize() * 4,
                    30,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>(this.getServerPoolSize() * 12),
                    new NamedThreadFactory("TCPServer-Listener"),
                    new ThreadPoolExecutor.CallerRunsPolicy()));
        }
        if (this.getWorkerExecutors() == null) {  // no executors have been set for workers
            if (this.getWorkerPoolSize() == TCPNettyServer.INVALID_POOL_SIZE) { // thread pool size has been set. create and use a fixed thread pool
                this.setWorkerPoolSize(Runtime.getRuntime().availableProcessors());
            }
            this.setWorkerExecutors(new ThreadPoolExecutor(this.getWorkerPoolSize(),
                    this.getWorkerPoolSize() * 4,
                    30,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>(this.getWorkerPoolSize() * 12),
                    new NamedThreadFactory("TCPServer-Worker"),
                    new ThreadPoolExecutor.CallerRunsPolicy()));
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
        return new ServerBootstrap(new NioServerSocketChannelFactory(this.getServerExecutors(), this.getWorkerExecutors()));
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
    /** End Getter/Setter methods */

}
