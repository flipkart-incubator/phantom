package com.flipkart.phantom.event;

/**
 * EventType based on type of the server where event originates.
 *
 * @author amanpreet.singh
 * @version 1.0.0
 * @since 24/10/13 5:44 PM.
 */
public enum ServiceProxyEventType {
    TASK_HANDLER, HTTP_HANDLER, THRIFT_HANDLER
}
