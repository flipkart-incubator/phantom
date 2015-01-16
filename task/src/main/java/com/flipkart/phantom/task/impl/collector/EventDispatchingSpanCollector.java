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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import com.flipkart.phantom.event.ServiceProxyEvent;
import com.flipkart.phantom.event.ServiceProxyEventProducer;
import com.github.kristofa.brave.SpanCollector;
import com.twitter.zipkin.gen.AnnotationType;
import com.twitter.zipkin.gen.BinaryAnnotation;
import com.twitter.zipkin.gen.Span;

/**
 * <code>EventDispatchingSpanCollector</code> is an implementation of {@link SpanCollector} that dispatches the span data using the event publisher infrastructure
 * for it to be suitably consumed and stored, logged or simply ignored by a registered event consumer.
 * 
 * @author Regunath B
 * @version 1.0, 18th Nov, 2014
 */
public class EventDispatchingSpanCollector implements SpanCollector {

	/** The UTF encoding string identifier*/
    private static final String UTF_8 = "UTF-8";
    
    /** Event Type for publishing all events which are generated here */
    private final static String TRACING_COLLECTOR = "TRACING_COLLECTOR";    

	/** A set of default annotations that might be added for this collector*/
    private final Set<BinaryAnnotation> defaultAnnotations = new HashSet<BinaryAnnotation>();

    /** The publisher used to broadcast events to Service Proxy Subscribers */
    private ServiceProxyEventProducer eventProducer;
    
	/**
	 * Interface method implementation.
	 * @see com.github.kristofa.brave.SpanCollector#collect(com.twitter.zipkin.gen.Span)
	 */
	public void collect(Span span) {
        if (!defaultAnnotations.isEmpty()) {
            for (final BinaryAnnotation ba : defaultAnnotations) {
                span.addToBinary_annotations(ba);
            }
        }
        ServiceProxyEvent.Builder eventBuilder  = new ServiceProxyEvent.Builder(span.getName(),TRACING_COLLECTOR);
        eventBuilder.withSpan(span);
        this.eventProducer.publishEvent(eventBuilder.build());
	}

	/**
	 * Interface method implementation.
	 * @see com.github.kristofa.brave.SpanCollector#addDefaultAnnotation(java.lang.String, java.lang.String)
	 */
	public void addDefaultAnnotation(String key, String value) {
        try {
            final ByteBuffer bb = ByteBuffer.wrap(value.getBytes(UTF_8));
            final BinaryAnnotation binaryAnnotation = new BinaryAnnotation();
            binaryAnnotation.setKey(key);
            binaryAnnotation.setValue(bb);
            binaryAnnotation.setAnnotation_type(AnnotationType.STRING);
            this.defaultAnnotations.add(binaryAnnotation);
        } catch (final UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
	}

	/**
	 * Interface method implementation. Does nothing i.e. does not close this collector as this is anyway just a dispatcher to the real consumer, if any
	 * @see com.github.kristofa.brave.SpanCollector#close()
	 */
	public void close() {
		// no op. Do nothing
	}
	
	/** Getter/Setter methods*/
    public void setEventProducer(final ServiceProxyEventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }	

}
