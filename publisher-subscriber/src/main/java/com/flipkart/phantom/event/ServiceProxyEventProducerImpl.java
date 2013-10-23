package com.flipkart.phantom.event;

import org.trpr.platform.core.impl.event.EndpointEventProducerImpl;
import org.trpr.platform.model.event.PlatformEvent;

/**
 * The <Code>ServiceProxyEventProducerImpl</Code> is an extension of EndpointEventProducerImpl
 *
 * @author amanpreet.singh
 * @version 1.0.0
 * @since 24/10/13 5:44 PM.
 */
public class ServiceProxyEventProducerImpl extends EndpointEventProducerImpl {
    public void publishEvent(ServiceProxyEvent event) {
        super.publishEvent((PlatformEvent) event);
    }
}
