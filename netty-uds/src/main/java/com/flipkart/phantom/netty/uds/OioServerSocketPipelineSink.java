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
import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.util.ThreadRenamingRunnable;
import org.jboss.netty.util.internal.DeadLockProofWorker;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.newsclub.net.unix.AFUNIXSocketException;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.Executor;

import static org.jboss.netty.channel.Channels.*;

/**
 * Based on: org.jboss.netty.channel.socket.oio.OioServerSocketPipelineSink
 * OIO package modified to work for Unix Domain Sockets instead of ServerSocket. 
 * 
 * @author devashishshankar
 * @version 1.0, 19th April 2013
 */
class OioServerSocketPipelineSink extends AbstractChannelSink {

	/** Log instance for this class */
	static final InternalLogger logger = InternalLoggerFactory.getInstance(OioServerSocketPipelineSink.class);

	/** Executor for running the Worker Threads */
	final Executor workerExecutor;

	/** Socket file, for sending a dummy request to the channel  */
	private File socketFile;

	/** Handle to the Boss instance */
	private Boss bossInstance;

	/** Default Constructor */
	OioServerSocketPipelineSink(Executor workerExecutor) {
		this.workerExecutor = workerExecutor;
	}

	public void eventSunk(
			ChannelPipeline pipeline, ChannelEvent e) throws Exception {
		Channel channel = e.getChannel();
		if (channel instanceof OioServerSocketChannel) {
			handleServerSocket(e);
		} else if (channel instanceof OioAcceptedSocketChannel) {
			handleAcceptedSocket(e);
		}
	}

	private void handleServerSocket(ChannelEvent e) {
		if (!(e instanceof ChannelStateEvent)) {
			return;
		}

		ChannelStateEvent event = (ChannelStateEvent) e;
		OioServerSocketChannel channel =
				(OioServerSocketChannel) event.getChannel();
		ChannelFuture future = event.getFuture();
		ChannelState state = event.getState();
		Object value = event.getValue();

		switch (state) {
		case OPEN:
			if (Boolean.FALSE.equals(value)) {
				close(channel, future);
			}
			break;
		case BOUND:
			if (value != null) {
				bind(channel, future, (SocketAddress) value);
			} else {
				close(channel, future);
			}
			break;
		}
	}

	private void handleAcceptedSocket(ChannelEvent e) {
		if (e instanceof ChannelStateEvent) {
			ChannelStateEvent event = (ChannelStateEvent) e;
			OioAcceptedSocketChannel channel =
					(OioAcceptedSocketChannel) event.getChannel();
			ChannelFuture future = event.getFuture();
			ChannelState state = event.getState();
			Object value = event.getValue();

			switch (state) {
			case OPEN:
				if (Boolean.FALSE.equals(value)) {
					OioWorker.close(channel, future);
				}
				break;
			case BOUND:
			case CONNECTED:
				if (value == null) {
					OioWorker.close(channel, future);
				}
				break;
			case INTEREST_OPS:
				OioWorker.setInterestOps(channel, future, ((Integer) value).intValue());
				break;
			}
		} else if (e instanceof MessageEvent) {
			MessageEvent event = (MessageEvent) e;
			OioSocketChannel channel = (OioSocketChannel) event.getChannel();
			ChannelFuture future = event.getFuture();
			Object message = event.getMessage();
			OioWorker.write(channel, future, message);
		}
	}

	/**
	 * Binds the channel with the SocketAddress
	 */
	 private void bind(
			 OioServerSocketChannel channel, ChannelFuture future,
			 SocketAddress localAddress) {

		boolean bound = false;
		boolean bossStarted = false;
		try {
			channel.socket.bind(localAddress, channel.getConfig().getBacklog());
			bound = true;

			future.setSuccess();
			localAddress = channel.getLocalAddress();
			fireChannelBound(channel, localAddress);

			Executor bossExecutor =
					((OioServerSocketChannelFactory) channel.getFactory()).bossExecutor;
			this.bossInstance = new Boss(channel);
			DeadLockProofWorker.start(
					bossExecutor,
					new ThreadRenamingRunnable(this.bossInstance, "Old I/O server boss (" + channel + ')'));
			bossStarted = true;
		} catch (Throwable t) {
			future.setFailure(t);
			fireExceptionCaught(channel, t);
		} finally {
			if (!bossStarted && bound) {
				close(channel, future);
			}
		}
	 }

	 /**
	  * Closes the channel and socket.
	  */
	 private void close(OioServerSocketChannel channel, ChannelFuture future) {
		 boolean bound = channel.isBound();
		 try {
			 //Stop signal to the boss thread
			 this.bossInstance.stop();
			 //Send a dummy request to actually stop the thread.
			 AFUNIXSocket sock = AFUNIXSocket.newInstance();
			 try {
				 sock.connect(new AFUNIXSocketAddress(this.socketFile));
			 } catch (AFUNIXSocketException e) {
				 logger.warn("Failed to connect to Socket while sending a stop request.");
			 }
			 //Close the socket
			 channel.socket.close();

			 // Make sure the boss thread is not running so that that the future
			 // is notified after a new connection cannot be accepted anymore.
			 // See NETTY-256 for more information.
			 channel.shutdownLock.lock();
			 try {
				 if (channel.setClosed()) {
					 future.setSuccess();
					 if (bound) {
						 fireChannelUnbound(channel);
					 }
					 fireChannelClosed(channel);
				 } else {
					 future.setSuccess();
				 }
			 } finally {
				 channel.shutdownLock.unlock();
			 }
		 } catch (Throwable t) {
			 future.setFailure(t);
			 fireExceptionCaught(channel, t);
		 }
	 }

	 /** The class executing the Boss thread, which receives a command and distributes it's work to the Worker threads */
	 private final class Boss implements Runnable {

		 /** The server socket channel instance for this class */
		 private final OioServerSocketChannel channel;

		 /** Boolean variable for signaling the Boss thread to stop  */
		 private boolean isAlive = true;

		 /** Default constructor */
		 Boss(OioServerSocketChannel channel) {
			 this.channel = channel;
		 }

		 /**
		  * Interface method implementation.
		  * @see java.lang.Runnable#run()
		  */
		 public void run() {
			 channel.shutdownLock.lock();
			 try {
				 while (channel.isBound()) {
					 try {
						 Socket acceptedSocket = channel.socket.accept();
						 acceptedSocket.setSoTimeout(300);
						 if(!this.isAlive) { //If Boss thread has been stopped, break
							 logger.debug("Stopping boss thread");
							 break;
						 }
						 try {
							 ChannelPipeline pipeline =
									 channel.getConfig().getPipelineFactory().getPipeline();
							 final OioAcceptedSocketChannel acceptedChannel =
									 new OioAcceptedSocketChannel(
											 channel,
											 channel.getFactory(),
											 pipeline,
											 OioServerSocketPipelineSink.this,
											 acceptedSocket);
							 DeadLockProofWorker.start(
									 workerExecutor,
									 new ThreadRenamingRunnable(
											 new OioWorker(acceptedChannel),
											 "Old I/O server worker (parentId: " +
													 channel.getId() + ", " + channel + ')'));
						 } catch (Exception e) {
							 logger.warn("Failed to initialize an accepted socket.", e);
							 try {
								 acceptedSocket.close();
							 } catch (IOException e2) {
								 logger.warn("Failed to close a partially accepted socket.", e2);
							 }
						 }
					 } catch (SocketTimeoutException e) {
						 // Thrown every second to stop when requested.
					 } catch (Throwable e) {
						 e.printStackTrace();
						 // Do not log the exception if the server socket was closed
						 // by a user.
						 if (!channel.socket.isBound() || channel.socket.isClosed()) {
							 break;
						 }
						 logger.warn(
								 "Failed to accept a connection.", e);
						 try {
							 Thread.sleep(1000);
						 } catch (InterruptedException e1) {
							 // Ignore
						 }
					 }
				 }
			 } finally {
				 channel.shutdownLock.unlock();
				 logger.debug("Channel unlocked by Boss Thread");
			 }
		 }

		 /** 
		  * Signal to stop Boss Thread. Note that a dummy request should be sent after invoking stop
		  * to stop the Boss Thread.
		  */
		 public void stop() {
			 this.isAlive = false;
		 }
	 }

	 /** Getter/Setter methods */
	 public File getSocketFile() {
		 return socketFile;
	 }
	 public void setSocketFile(File socketFile) {
		 this.socketFile = socketFile;
	 }
}
