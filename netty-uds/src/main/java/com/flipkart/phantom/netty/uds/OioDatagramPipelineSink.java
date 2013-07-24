/*
 * Copyright 2012-2015, Flipkart Internet Pvt Ltd. All rights reserved.
 * 
 * This software is the confidential and proprietary information of Flipkart Internet Pvt Ltd. ("Confidential Information").  
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of the license 
 * agreement you entered into with Flipkart.    
 * 
 */
package com.flipkart.phantom.netty.uds;

import org.jboss.netty.channel.*;
import org.jboss.netty.util.ThreadRenamingRunnable;
import org.jboss.netty.util.internal.DeadLockProofWorker;

import java.net.SocketAddress;
import java.util.concurrent.Executor;

import static org.jboss.netty.channel.Channels.*;

/**
 * Based on: org.jboss.netty.channel.socket.oio.OioDatagramPipelineSink
 * OIO package modified to work for Unix Domain Sockets instead of ServerSocket.
 * 
 * @author devashishshankar
 * @version 1.0, 19th April 2013
 */
class OioDatagramPipelineSink extends AbstractChannelSink {

    private final Executor workerExecutor;

    OioDatagramPipelineSink(Executor workerExecutor) {
        this.workerExecutor = workerExecutor;
    }

    public void eventSunk(
            ChannelPipeline pipeline, ChannelEvent e) throws Exception {
        OioDatagramChannel channel = (OioDatagramChannel) e.getChannel();
        ChannelFuture future = e.getFuture();
        if (e instanceof ChannelStateEvent) {
            ChannelStateEvent stateEvent = (ChannelStateEvent) e;
            ChannelState state = stateEvent.getState();
            Object value = stateEvent.getValue();
            switch (state) {
            case OPEN:
                if (Boolean.FALSE.equals(value)) {
                    OioDatagramWorker.close(channel, future);
                }
                break;
            case BOUND:
                if (value != null) {
                    bind(channel, future, (SocketAddress) value);
                } else {
                    OioDatagramWorker.close(channel, future);
                }
                break;
            case CONNECTED:
                if (value != null) {
                    connect(channel, future, (SocketAddress) value);
                } else {
                    OioDatagramWorker.disconnect(channel, future);
                }
                break;
            case INTEREST_OPS:
                OioDatagramWorker.setInterestOps(channel, future, ((Integer) value).intValue());
                break;
            }
        } else if (e instanceof MessageEvent) {
            MessageEvent evt = (MessageEvent) e;
            OioDatagramWorker.write(
                    channel, future, evt.getMessage(), evt.getRemoteAddress());
        }
    }

    private void bind(
            OioDatagramChannel channel, ChannelFuture future,
            SocketAddress localAddress) {
        boolean bound = false;
        boolean workerStarted = false;
        try {
            channel.socket.bind(localAddress);
            bound = true;

            // Fire events
            future.setSuccess();
            fireChannelBound(channel, channel.getLocalAddress());

            // Start the business.
            DeadLockProofWorker.start(
                    workerExecutor,
                    new ThreadRenamingRunnable(
                            new OioDatagramWorker(channel),
                            "Old I/O datagram worker (" + channel + ')'));
            workerStarted = true;
        } catch (Throwable t) {
            future.setFailure(t);
            fireExceptionCaught(channel, t);
        } finally {
            if (bound && !workerStarted) {
                OioDatagramWorker.close(channel, future);
            }
        }
    }

    private void connect(
            OioDatagramChannel channel, ChannelFuture future,
            SocketAddress remoteAddress) {

        boolean bound = channel.isBound();
        boolean connected = false;
        boolean workerStarted = false;

        future.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);

        // Clear the cached address so that the next getRemoteAddress() call
        // updates the cache.
        channel.remoteAddress = null;

        try {
            channel.socket.connect(remoteAddress);
            connected = true;

            // Fire events.
            future.setSuccess();
            if (!bound) {
                fireChannelBound(channel, channel.getLocalAddress());
            }
            fireChannelConnected(channel, channel.getRemoteAddress());

            String threadName = "Old I/O datagram worker (" + channel + ')';
            if (!bound) {
                // Start the business.
                DeadLockProofWorker.start(
                        workerExecutor,
                        new ThreadRenamingRunnable(
                                new OioDatagramWorker(channel), threadName));
            } else {
                // Worker started by bind() - just rename.
                Thread workerThread = channel.workerThread;
                if (workerThread != null) {
                    try {
                        workerThread.setName(threadName);
                    } catch (SecurityException e) {
                        // Ignore.
                    }
                }
            }

            workerStarted = true;
        } catch (Throwable t) {
            future.setFailure(t);
            fireExceptionCaught(channel, t);
        } finally {
            if (connected && !workerStarted) {
                OioDatagramWorker.close(channel, future);
            }
        }
    }
}
