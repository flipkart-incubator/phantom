/*
 * Copyright 2012-2015, Flipkart Internet Pvt Ltd. All rights reserved.
 * 
 * This software is the confidential and proprietary information of Flipkart Internet Pvt Ltd. ("Confidential Information").  
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of the license 
 * agreement you entered into with Flipkart.    
 * 
 */
package com.flipkart.phantom.netty.uds;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.SocketChannel;
import org.jboss.netty.util.internal.ExecutorUtil;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * A {@link ClientSocketChannelFactory} which creates a client-side blocking
 * I/O based {@link SocketChannel}.  It utilizes the good old blocking I/O API
 * which is known to yield better throughput and latency when there are
 * relatively small number of connections to serve.
 *
 * <h3>How threads work</h3>
 * <p>
 * There is only one type of threads in {@link OioClientSocketChannelFactory};
 * worker threads.
 *
 * <h4>Worker threads</h4>
 * <p>
 * Each connected {@link Channel} has a dedicated worker thread, just like a
 * traditional blocking I/O thread model.
 *
 * <h3>Life cycle of threads and graceful shutdown</h3>
 * <p>
 * Worker threads are acquired from the {@link Executor} which was specified
 * when a {@link OioClientSocketChannelFactory} was created (i.e. {@code workerExecutor}.)
 * Therefore, you should make sure the specified {@link Executor} is able to
 * lend the sufficient number of threads.
 * <p>
 * Worker threads are acquired lazily, and then released when there's nothing
 * left to process.  All the related resources are also released when the
 * worker threads are released.  Therefore, to shut down a service gracefully,
 * you should do the following:
 *
 * <ol>
 * <li>close all channels created by the factory usually using
 *     {@link ChannelGroup#close()}, and</li>
 * <li>call {@link #releaseExternalResources()}.</li>
 * </ol>
 *
 * Please make sure not to shut down the executor until all channels are
 * closed.  Otherwise, you will end up with a {@link RejectedExecutionException}
 * and the related resources might not be released properly.
 *
 * <h3>Limitation</h3>
 * <p>
 * A {@link SocketChannel} created by this factory does not support asynchronous
 * operations.  Any I/O requests such as {@code "connect"} and {@code "write"}
 * will be performed in a blocking manner.
 * 
 * @apiviz.landmark
 * 
 * Ported to com.flipkart.phantom.runtime.impl.server.netty.oio for package compatibility
 * OIO package modified to work for Unix Domain Sockets instead of ServerSocket.
 *
 * @author devashishshankar
 * @version 1.0, April 19th, 2013
 */
public class OioClientSocketChannelFactory implements ClientSocketChannelFactory {

    private final Executor workerExecutor;
    final OioClientSocketPipelineSink sink;

    /**
     * Creates a new instance.
     *
     * @param workerExecutor
     *        the {@link Executor} which will execute the I/O worker threads
     */
    public OioClientSocketChannelFactory(Executor workerExecutor) {
        if (workerExecutor == null) {
            throw new NullPointerException("workerExecutor");
        }
        this.workerExecutor = workerExecutor;
        sink = new OioClientSocketPipelineSink(workerExecutor);
    }

    public SocketChannel newChannel(ChannelPipeline pipeline) {
        return new OioClientSocketChannel(this, pipeline, sink);
    }

    public void releaseExternalResources() {
        ExecutorUtil.terminate(workerExecutor);
    }
}
