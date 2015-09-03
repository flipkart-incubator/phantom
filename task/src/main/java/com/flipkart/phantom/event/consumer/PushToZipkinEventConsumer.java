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
package com.flipkart.phantom.event.consumer;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.trpr.platform.core.impl.event.AbstractEndpointEventConsumerImpl;
import org.trpr.platform.model.event.PlatformEvent;

import com.flipkart.phantom.event.ServiceProxyEvent;
import com.github.kristofa.brave.SpanCollector;

/**
 * <code>PushToZipkinEventConsumer</code> is a sub-type of {@link AbstractEndpointEventConsumerImpl} that pushes consumed events to a Zipkin collector.
 * 
 * @author Regunath B
 * @version 1.0, 21st Nov, 2014
 */
public class PushToZipkinEventConsumer extends AbstractEndpointEventConsumerImpl implements InitializingBean {

    /** The SpanCollector instance*/
    private SpanCollector spanCollector;
    
    /** The RequestLogger to registered tracing subscription with*/
    private RequestLogger requestLogger;

    /**
     * Interface method implementation. Checks if all mandatory properties have been set
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(this.requestLogger, "The 'requestLogger' may not be null");
        // Register the subscriptions with the RequestLogger
        for (String subscription : this.getSubscriptions()) {
        	this.requestLogger.addSubscriptionAndConsumer(subscription, this);
        }
    }
    
    /**
     * Abstract method implementation. Pushes the tracing information contained in the event to a Zipkin collector 
     * @see org.trpr.platform.core.impl.event.AbstractEndpointEventConsumerImpl#handlePlatformEvent(org.trpr.platform.model.event.PlatformEvent)
     */
	protected void handlePlatformEvent(PlatformEvent platformEvent) {
        if (platformEvent instanceof ServiceProxyEvent) {
        	this.spanCollector.collect(((ServiceProxyEvent)platformEvent).getSpan());
        }		
	}

	/** Getter/Setter methods */
	public void setSpanCollector(SpanCollector spanCollector) {
		this.spanCollector = spanCollector;
	}
	public void setRequestLogger(RequestLogger requestLogger) {
		this.requestLogger = requestLogger;
	}

}
