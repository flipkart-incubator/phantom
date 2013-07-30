package com.flipkart.phantom.http.impl.registry;

import com.flipkart.phantom.http.spi.HttpProxy;
import com.flipkart.phantom.task.spi.TaskContext;
import com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kartikbu
 * @version 1.0
 * @created 30/7/13 1:57 AM
 */
public class HttpProxyRegistry extends AbstractHandlerRegistry {

    private static Logger LOGGER = LoggerFactory.getLogger(HttpProxyRegistry.class);

    private List<HttpProxy> proxies = new ArrayList<HttpProxy>();

    @Override
    public void init(TaskContext taskContext) throws Exception {
        for (HttpProxy proxy : proxies) {
            try {
                proxy.init();
            } catch (Exception e) {
                LOGGER.error("Failed to initialize http proxy: " + proxy.getName());
                throw new Exception("Failed to initialize http proxy: " + proxy.getName(), e);
            }
        }

    }

    @Override
    public void shutdown(TaskContext taskContext) throws Exception {
        for (HttpProxy proxy: proxies) {
            try {
                proxy.shutdown();
            } catch (Exception e) {
                LOGGER.warn("Failed to shutdown thrift proxy: " + proxy.getName(), e);
            }
        }
    }

}
