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
