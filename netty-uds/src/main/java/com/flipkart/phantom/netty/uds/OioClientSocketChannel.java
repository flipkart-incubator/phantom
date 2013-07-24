/*
 * Copyright 2012-2015, Flipkart Internet Pvt Ltd. All rights reserved.
 * 
 * This software is the confidential and proprietary information of Flipkart Internet Pvt Ltd. ("Confidential Information").  
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of the license 
 * agreement you entered into with Flipkart.    
 * 
 */
package com.flipkart.phantom.netty.uds;

import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelSink;

import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.Socket;

import static org.jboss.netty.channel.Channels.fireChannelOpen;

/**
 * Based on: org.jboss.net
    <scm>
        <url>https://github.com/Flipkart/phantom</url>
        <connection>https://github.com/Flipkart/phantom.git</connection>
    </scm>

    <developers>
        <developer>ty.channel.socket.oio.OioClientSocketChannel
 * Ported to com.flipkart.phantom.runtime.impl.server.netty.oio for package compatibility
 * OIO package modified to work for Unix Domain Sockets instead of ServerSocket.
 * 
 * @author devashishshankar
 * @version 1.0, 19th April 2013
 */
class OioClientSocketChannel extends OioSocketChannel {

    volatile PushbackInputStream in;
    volatile OutputStream out;

    OioClientSocketChannel(
            ChannelFactory factory,
            ChannelPipeline pipeline,
            ChannelSink sink) {

        super(null, factory, pipeline, sink, new Socket());

        fireChannelOpen(this);
    }

    @Override
    PushbackInputStream getInputStream() {
        return in;
    }

    @Override
    OutputStream getOutputStream() {
        return out;
    }
}
