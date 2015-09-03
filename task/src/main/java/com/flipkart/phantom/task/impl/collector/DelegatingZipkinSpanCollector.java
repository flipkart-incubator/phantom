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
package com.flipkart.phantom.task.impl.collector;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import com.github.kristofa.brave.SpanCollector;
import com.github.kristofa.brave.zipkin.ZipkinSpanCollector;
import com.github.kristofa.brave.zipkin.ZipkinSpanCollectorParams;
import com.twitter.zipkin.gen.Span;

/**
 * <code>DelegatingZipkinSpanCollector</code> is an implementation of {@link SpanCollector} that wraps over the Brave {@link ZipkinSpanCollector}. This collector lazy initializes
 * the ZipkinSpanCollector when the first request to {@link SpanCollector#collect(com.twitter.zipkin.gen.Span)} is called. The init call would block across threads.
 * The cost of deferred instantiation and associated increase in latency is justified by these reasons:
 * <pre>
 * <li> The connection to Zipkin host is made only when tracing is turned on. Removes dependency on Zipkin during Phantom startup.
 * <li> Allows beans to be configured with Zipkin configurations but with no active connections made   
 * </pre>
 * @author Regunath B
 * @version 1.0, 23rd June, 2015
 */

public class DelegatingZipkinSpanCollector implements SpanCollector,InitializingBean {

	/** Params for initializing the ZipkinSpanCollector*/
	private ZipkinSpanCollectorParams zipkinSpanCollectorParams = new ZipkinSpanCollectorParams();
	
	/** The ZipkinSpanCollector to which all calls are delegated to*/
	private ZipkinSpanCollector zipkinSpanCollector;
	
	/** The Zipkin collector host details*/
	private String zipkinCollectorHost;
	private int zipkinCollectorPort;

	/**
	 * Interface method implementation. Checks for mandatory properties
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
        Assert.notNull(this.zipkinCollectorHost, "The 'zipkinCollectorHost' may not be null");		
        Assert.notNull(this.zipkinCollectorPort, "The 'zipkinCollectorPort' may not be null");		
	}
	
	/**
	 * Interface method implementation. Initializes a ZipkinSpanCollector, if required, and submits the span to it.
	 * @see com.github.kristofa.brave.SpanCollector#collect(com.twitter.zipkin.gen.Span)
	 */
	public void collect(Span span) {
		if (zipkinSpanCollector == null) {
			synchronized(this) { // synchronized across concurrent access by threads
				if (zipkinSpanCollector == null) { // double check before creating the instance
					this.zipkinSpanCollector = new ZipkinSpanCollector(zipkinCollectorHost, zipkinCollectorPort,this.zipkinSpanCollectorParams);
				}
			}
		}
		zipkinSpanCollector.collect(span);
	}

	/**
	 * Interface method implementation. Initializes a ZipkinSpanCollector, if required, and adds the annotations to it.
	 * @see com.github.kristofa.brave.SpanCollector#addDefaultAnnotation(java.lang.String, java.lang.String)
	 */
	public void addDefaultAnnotation(String key, String value) {
		if (zipkinSpanCollector == null) {
			synchronized(this) { // synchronized across concurrent access by threads
				if (zipkinSpanCollector == null) { // double check before creating the instance
					this.zipkinSpanCollector = new ZipkinSpanCollector(zipkinCollectorHost, zipkinCollectorPort,this.zipkinSpanCollectorParams);
				}
			}
		}
		zipkinSpanCollector.addDefaultAnnotation(key, value);
	}

	/**
	 * Interface method implementation. Closes the ZipkinSpanCollector
	 * @see com.github.kristofa.brave.SpanCollector#close()
	 */
	public void close() {
		zipkinSpanCollector.close();
	}
	
	/** Setter methods for properties*/
	public void setZipkinCollectorHost(String zipkinCollectorHost) {
		this.zipkinCollectorHost = zipkinCollectorHost;
	}
	public void setZipkinCollectorPort(int zipkinCollectorPort) {
		this.zipkinCollectorPort = zipkinCollectorPort;
	}
	
	/** Setter methods for Zipkin span collection params*/
	public void setQueueSize(final int queueSize) {this.zipkinSpanCollectorParams.setQueueSize(queueSize);}
	public void setBatchSize(final int batchSize) {this.zipkinSpanCollectorParams.setBatchSize(batchSize);}
	public void setNrOfThreads(final int nrOfThreads) {this.zipkinSpanCollectorParams.setNrOfThreads(nrOfThreads);}
	public void setSocketTimeout(final int socketTimeout) {this.zipkinSpanCollectorParams.setSocketTimeout(socketTimeout);}

}
