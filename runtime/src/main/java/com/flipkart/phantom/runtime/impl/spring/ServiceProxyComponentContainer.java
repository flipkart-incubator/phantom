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
package com.flipkart.phantom.runtime.impl.spring;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.trpr.platform.core.PlatformException;
import org.trpr.platform.core.impl.logging.LogFactory;
import org.trpr.platform.core.spi.event.PlatformEventProducer;
import org.trpr.platform.core.spi.logging.Logger;
import org.trpr.platform.model.event.PlatformEvent;
import org.trpr.platform.runtime.common.RuntimeConstants;
import org.trpr.platform.runtime.common.RuntimeVariables;
import org.trpr.platform.runtime.impl.bootstrapext.spring.ApplicationContextFactory;
import org.trpr.platform.runtime.impl.config.FileLocator;
import org.trpr.platform.runtime.spi.bootstrapext.BootstrapExtension;
import org.trpr.platform.runtime.spi.component.ComponentContainer;

import com.flipkart.phantom.runtime.ServiceProxyFrameworkConstants;
import com.flipkart.phantom.runtime.impl.hystrix.HystrixEventReceiver;
import com.flipkart.phantom.runtime.impl.server.AbstractNetworkServer;
import com.flipkart.phantom.runtime.impl.spring.admin.SPConfigServiceImpl;
import com.flipkart.phantom.runtime.spi.spring.admin.SPConfigService;
import com.flipkart.phantom.task.spi.AbstractHandler;
import com.flipkart.phantom.task.spi.TaskContext;
import com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry;
import com.flipkart.phantom.task.spi.registry.HandlerConfigInfo;
import com.netflix.hystrix.Hystrix;
import com.netflix.hystrix.strategy.HystrixPlugins;

/**
 * The <code>ServiceProxyComponentContainer</code> class is a ComponentContainer implementation as defined by Trooper {@link "https://github.com/regunathb/Trooper"}
 * that starts up a runtime for proxying service requests.
 * This container loads the service proxy listener from {@link ServiceProxyFrameworkConstants#SPRING_PROXY_LISTENER_CONFIG}.
 * It then locates and loads all service proxy handlers contained in files named by the value of {@link ServiceProxyFrameworkConstants#SPRING_PROXY_HANDLER_CONFIG}.
 * This container also loads all common proxy related Spring beans contained in {@link ServiceProxyFrameworkConstants#COMMON_PROXY_CONFIG} and
 * ensures that all beans declared in Trooper ServerConstants.COMMON_SPRING_BEANS_CONFIG are available to the proxy handler beans by
 * specifying the common beans context as the parent for each proxy app context created by this container.
 *
 * @see org.trpr.platform.runtime.spi.component.ComponentContainer
 * @author Regunath B
 * @version 1.0, 14 Mar 2013
 */
public class ServiceProxyComponentContainer<T extends AbstractHandler> implements ComponentContainer {

    /**
     * The default Event producer bean name
     */
    private static final String DEFAULT_EVENT_PRODUCER = "platformEventProducer";

    /** The prefix to be added to file absolute paths when loading Spring XMLs using the FileSystemXmlApplicationContext */
    private static final String FILE_PREFIX = "file:";

    /** The bean names of the service proxy framework classes initialized by this container */
    private static final String CONFIG_SERVICE_BEAN = "configService";
    private static final String TASK_CONTEXT_BEAN = "taskContext";

    /** Logger for this class */
    private static final Logger LOGGER = LogFactory.getLogger(ServiceProxyComponentContainer.class);

    /** The common proxy handler beans context */
    private static AbstractApplicationContext commonProxyHandlerBeansContext;

    /** The list of HandlerConfigInfo holding all proxy handler instances loaded by this container */
    private List<HandlerConfigInfo> handlerConfigInfoList = new LinkedList<HandlerConfigInfo>();

    /** Local reference for all BootstrapExtensionS loaded by the Container and set on this ComponentContainer */
    private BootstrapExtension[] loadedBootstrapExtensions;

    /** The Thread's context class loader that is used in on the fly loading of proxy handler definitions */
    private ClassLoader tccl;

    /** The list of registered registries */
    private List<AbstractHandlerRegistry<T>> registries = new ArrayList<AbstractHandlerRegistry<T>>();

    /** The configService instance */
    private SPConfigService<T> configService;

    /** The TaskContext bean instance */
    private TaskContext taskContext;

    /**
     * Returns the common Proxy Handler Spring beans application context that is intended as parent of all proxy handler application contexts
     * WARN : this method can return null if this ComponentContainer is not suitably initialized via a call to {@link #init()}
     * @return null or the common proxy handler AbstractApplicationContext
     */
    public static AbstractApplicationContext getCommonProxyHandlerBeansContext() {
        return ServiceProxyComponentContainer.commonProxyHandlerBeansContext;
    }

    /**
     * Interface method implementation. Returns the fully qualified class name of this class
     * @see org.trpr.platform.runtime.spi.component.ComponentContainer#getName()
     */
    public String getName() {
        return this.getClass().getName();
    }

    /**
     * Interface method implementation. Stores local references to the specified BootstrapExtension instances.
     * @see org.trpr.platform.runtime.spi.component.ComponentContainer#setLoadedBootstrapExtensions(org.trpr.platform.runtime.spi.bootstrapext.BootstrapExtension[])
     */
    public void setLoadedBootstrapExtensions(BootstrapExtension... bootstrapExtensions) {
        this.loadedBootstrapExtensions = bootstrapExtensions;
    }

    /**
     * Interface method implementation. Locates and loads all configured proxy handlers.
     * @see ComponentContainer#init()
     */
    @SuppressWarnings("unchecked")
	public void init() throws PlatformException {
        //Register HystrixEventNotifier
        if (HystrixPlugins.getInstance().getEventNotifier() == null) {
            HystrixPlugins.getInstance().registerEventNotifier(new HystrixEventReceiver());
        }
        // store the thread's context class loader for later use in on the fly loading of proxy handler app contexts
        this.tccl = Thread.currentThread().getContextClassLoader();

        // The common proxy handler beans context is loaded first using the Platform common beans context as parent
        // load this from classpath as it is packaged with the binaries
        ApplicationContextFactory defaultCtxFactory = null;
        for (BootstrapExtension be : this.loadedBootstrapExtensions) {
            if (ApplicationContextFactory.class.isAssignableFrom(be.getClass())) {
                defaultCtxFactory = (ApplicationContextFactory) be;
                break;
            }
        }
        // we look up the unique instance of ServiceProxyFrameworkConstants.COMMON_PROXY_CONFIG. This call would throw an exception if multiple are found
        String commonProxyBeansFileName = FileLocator.findUniqueFile(ServiceProxyFrameworkConstants.COMMON_PROXY_CONFIG).getAbsolutePath();
        ServiceProxyComponentContainer.commonProxyHandlerBeansContext = new ClassPathXmlApplicationContext(new String[]{ServiceProxyFrameworkConstants.COMMON_PROXY_CONFIG}, defaultCtxFactory.getCommonBeansContext());
        // add the common proxy beans independently to the list of proxy handler contexts as common handlers are declared there
        this.handlerConfigInfoList.add(new HandlerConfigInfo(new File(commonProxyBeansFileName), null, ServiceProxyComponentContainer.commonProxyHandlerBeansContext));

        // Get the Config Service Bean
        this.configService = (SPConfigServiceImpl<T>) ServiceProxyComponentContainer.commonProxyHandlerBeansContext.getBean(ServiceProxyComponentContainer.CONFIG_SERVICE_BEAN);
        ((SPConfigServiceImpl<T>) this.configService).setComponentContainer(this);

        // Load additional if runtime nature is "server". This context is the new common beans context
        if (RuntimeVariables.getRuntimeNature().equalsIgnoreCase(RuntimeConstants.SERVER)) {
            // we look up the unique instance of ServiceProxyFrameworkConstants.COMMON_PROXY_SERVER_NATURE_CONFIG. This call would throw an exception if multiple are found
            String commonProxyServerNatureBeansFileName = FileLocator.findUniqueFile(ServiceProxyFrameworkConstants.COMMON_PROXY_SERVER_NATURE_CONFIG).getAbsolutePath();
            ServiceProxyComponentContainer.commonProxyHandlerBeansContext = new ClassPathXmlApplicationContext(
                    new String[]{ServiceProxyFrameworkConstants.COMMON_PROXY_SERVER_NATURE_CONFIG},
                    ServiceProxyComponentContainer.getCommonProxyHandlerBeansContext());
            // now add the common server nature proxy hander beans to the contexts list
            this.handlerConfigInfoList.add(new HandlerConfigInfo(new File(commonProxyServerNatureBeansFileName),
                 null,ServiceProxyComponentContainer.getCommonProxyHandlerBeansContext()));
        }

        // locate and load a single common proxy handler bean XML files which is initialized before all other individual handlers
        // in case multiple are found fail the bootstrap.
        File[] commonProxyHandlerConfigFiles = FileLocator.findFiles(ServiceProxyFrameworkConstants.COMMON_PROXY_HANDLER_CONFIG);
        if (commonProxyHandlerConfigFiles.length > 0) {
            if (commonProxyHandlerConfigFiles.length == 1) {
                File commonProxyHandlerConfigFile = commonProxyHandlerConfigFiles[0];
                // load the common proxy handler
                HandlerConfigInfo commonHandlersConfigInfo = new HandlerConfigInfo(commonProxyHandlerConfigFile);
                // set the load order to first order i.e. load before others
                commonHandlersConfigInfo.setLoadOrder(HandlerConfigInfo.FIRST_ORDER);
                this.loadProxyHandlerContext(commonHandlersConfigInfo);
                LOGGER.info("Loaded Common Proxy Handler Config: " + commonProxyHandlerConfigFile);
            } else {
                final String errorMessage = "Found multiple common-proxy-handler-configs, only one is allowed";
                LOGGER.error(errorMessage);
                for (File commonHandlerConfig : commonProxyHandlerConfigFiles) {
                    LOGGER.error(commonHandlerConfig.getAbsolutePath());
                }
                throw new PlatformException(errorMessage);
            }
        }

        // locate and load the individual proxy handler bean XML files using the common proxy handler beans context as parent
        File[] proxyHandlerBeansFiles = FileLocator.findFiles(ServiceProxyFrameworkConstants.SPRING_PROXY_HANDLER_CONFIG);
        for (File proxyHandlerBeansFile : proxyHandlerBeansFiles) {
            HandlerConfigInfo handlerConfigInfo = new HandlerConfigInfo(proxyHandlerBeansFile);
            // load the proxy handler's appcontext
            this.loadProxyHandlerContext(handlerConfigInfo);
            LOGGER.info("Loaded: " + proxyHandlerBeansFile);
        }

        // add the proxy listener beans to the contexts list (these have the thrift handlers)
        File[] proxyListenerBeanFiles = FileLocator.findFiles(ServiceProxyFrameworkConstants.SPRING_PROXY_LISTENER_CONFIG);
        for (File proxyListenerBeanFile : proxyListenerBeanFiles) {
            // locate and load the service proxy listener defined in the file identified by {@link ServiceProxyFrameworkConstants#SPRING_PROXY_LISTENER_CONFIG}
            AbstractApplicationContext listenerContext = new FileSystemXmlApplicationContext(
                    new String[]{FILE_PREFIX + proxyListenerBeanFile.getAbsolutePath()},
                    ServiceProxyComponentContainer.getCommonProxyHandlerBeansContext()
            );
            this.handlerConfigInfoList.add(new HandlerConfigInfo(proxyListenerBeanFile, null, listenerContext));
            LOGGER.info("Loaded: " + proxyListenerBeanFile);
        }

        // store reference to the TaskContext 
        this.taskContext = (TaskContext) ServiceProxyComponentContainer.getCommonProxyHandlerBeansContext().getBean(ServiceProxyComponentContainer.TASK_CONTEXT_BEAN);
        
        // load all registries
        for (HandlerConfigInfo handlerConfigInfo : handlerConfigInfoList) {
            // handler registries
            String[] registryBeans = handlerConfigInfo.getProxyHandlerContext().getBeanNamesForType(AbstractHandlerRegistry.class);
            for (String registryBean : registryBeans) {
                AbstractHandlerRegistry<T> registry = (AbstractHandlerRegistry<T>) handlerConfigInfo.getProxyHandlerContext().getBean(registryBean);
                LOGGER.info("Found handler registry: " + registry.getClass().getName() + " in file : " + handlerConfigInfo.getXmlConfigFile().getAbsolutePath());
                // ensure that the same registry is not added twice in any of the config files
                if (this.configService.getDeployedRegistries().contains(registry)) {
                	 LOGGER.error("Error initializing registry: " + registry.getClass().getName() + ". Duplicate reference in location : " + handlerConfigInfo.getXmlConfigFile().getAbsolutePath());
                	throw new PlatformException("Error initializing registry: " + registry.getClass().getName() + ". Duplicate reference in location : " + handlerConfigInfo.getXmlConfigFile().getAbsolutePath());
                }
                // init the Registry
                try {
                    AbstractHandlerRegistry.InitedHandlerInfo<T>[] initedHandlerInfos = registry.init(this.handlerConfigInfoList, this.taskContext);
                    LOGGER.info("Initialized handler registry: " + registry.getClass().getName());
                    //Add the file path of each inited handler to SPConfigService (for configuration console)
                    for (AbstractHandlerRegistry.InitedHandlerInfo<T> initedHandlerInfo : initedHandlerInfos) {
                    	if (initedHandlerInfo == null) {
                    		System.out.println("Handler is nul!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! " + " Size is : " + initedHandlerInfos.length);
                    	}
                        this.configService.addHandlerConfigPath(initedHandlerInfo.getHandlerConfigInfo().getXmlConfigFile(), initedHandlerInfo.getInitedHandler());
                    }
                } catch (Exception e) {
                    LOGGER.error("Error initializing registry: " + registry.getClass().getName());
                    throw new PlatformException("Error initializing registry: " + registry.getClass().getName(), e);
                }
                // add registry to config
                this.configService.addHandlerRegistry(registry);
                // add registry to local list
                this.registries.add(registry);
            }

            // add all network servers to config
            String[] networkServerBeans = handlerConfigInfo.getProxyHandlerContext().getBeanNamesForType(AbstractNetworkServer.class);
            for (String networkServerBean : networkServerBeans) {
                AbstractNetworkServer networkServer = (AbstractNetworkServer) handlerConfigInfo.getProxyHandlerContext().getBean(networkServerBean);
                // init the server
                try {
                    networkServer.init();
                } catch (Exception e) {
                    LOGGER.error("Error initializeing network server: " + networkServer.getServerType() + ": " + networkServer.getServerEndpoint());
                    throw new PlatformException("Error initializeing network server: " + networkServer.getServerType() + ": " + networkServer.getServerEndpoint(), e);
                }
                configService.addDeployedNetworkServer(networkServer);
            }

        }
    }

    /**
     * Interface method implementation. Destroys the Spring application context containing loaded proxy handler definitions.
     * @see ComponentContainer#destroy()
     */
    public void destroy() throws PlatformException {
        // reset the Hystrix instance
        Hystrix.reset();
        // now shutdown all task handlers
        for (AbstractHandlerRegistry<T> registry : registries) {
            try {
                registry.shutdown(taskContext);
            } catch (Exception e) {
                LOGGER.warn("Error shutting down registry: " + registry.getClass().getName());
            }
        }
        // finally close the context
        for (HandlerConfigInfo handlerConfigInfo : this.handlerConfigInfoList) {
            handlerConfigInfo.getProxyHandlerContext().close();
        }
        this.handlerConfigInfoList = null;
    }

    /**
     * Interface method implementation. Publishes the specified event to using a named bean DEFAULT_EVENT_PRODUCER looked up from the
     * common proxy handler context (i.e. ServiceProxyFrameworkConstants.COMMON_PROXY_CONFIG).
     * Note that typically no consumers are registered when running this container
     */
    public void publishEvent(PlatformEvent event) {
        PlatformEventProducer publisher = (PlatformEventProducer) ServiceProxyComponentContainer.commonProxyHandlerBeansContext.getBean(DEFAULT_EVENT_PRODUCER);
        publisher.publishEvent(event);
    }

    /**
     * Interface method implementation. Publishes the specified event using the {@link #publishEvent(PlatformEvent)} method
     * @see ComponentContainer#publishBootstrapEvent(PlatformEvent)
     */
    public void publishBootstrapEvent(PlatformEvent bootstrapEvent) {
        this.publishEvent(bootstrapEvent);
    }

    /**
     * Reloads and re-initalizes the specified handler. The new definition is loaded from the specified Reosurce location
     * @param handler  the AbstractHandler to be de-registered
     * @param resource the location to load the new definition of the handler from
     */
	public void reloadHandler(T handler, Resource resource) {
        AbstractHandlerRegistry<T> registry = this.getRegistry(handler.getName());
        registry.unregisterTaskHandler(handler);
        LOGGER.debug("Unregistered TaskHandler: " + handler.getName());
        this.loadComponent(resource);
        // now add the newly loaded handler to its registry
        for (HandlerConfigInfo handlerConfigInfo : this.handlerConfigInfoList) {
            if (handlerConfigInfo.getXmlConfigFile().getAbsolutePath().equalsIgnoreCase(((FileSystemResource) resource).getFile().getAbsolutePath())) {
                List<HandlerConfigInfo> reloadHandlerConfigInfoList = new LinkedList<HandlerConfigInfo>();
                reloadHandlerConfigInfoList.add(handlerConfigInfo);
                try {
                    registry.init(reloadHandlerConfigInfoList, taskContext);
                } catch (Exception e) {
                    LOGGER.error("Error updating registry : " + registry.getClass().getName() + " for handler : " + handler.getName(), e);
                    throw new PlatformException("Error updating registry : " + registry.getClass().getName() + " for handler : " + handler.getName(), e);
                }
                return;
            }
        }
    }

    /**
     * Interface method implementation. Loads/Reloads proxy handler(s) defined in the specified {@link FileSystemResource}
     * @see org.trpr.platform.runtime.spi.component.ComponentContainer#loadComponent(org.springframework.core.io.Resource)
     */
    public void loadComponent(Resource resource) {
        if (!FileSystemResource.class.isAssignableFrom(resource.getClass()) ||
                !resource.getFilename().equalsIgnoreCase(ServiceProxyFrameworkConstants.SPRING_PROXY_HANDLER_CONFIG)) {
            throw new UnsupportedOperationException("Proxy handers can be loaded only from files by name : " +
                    ServiceProxyFrameworkConstants.SPRING_PROXY_HANDLER_CONFIG + ". Specified resource is : " + resource.toString());
        }
        loadProxyHandlerContext(new HandlerConfigInfo(((FileSystemResource) resource).getFile()));
    }

    /**
     * Returns the AbstractHandlerRegistry in which an AbstractHandler identified by the specified name has been registered
     * @param handlerName the AbstractHandler name
     * @return AbstractHandlerRegistry where the handler is registered
     * @throws UnsupportedOperationException if a registry is not found
     */
    public AbstractHandlerRegistry<T> getRegistry(String handlerName) {
        for (AbstractHandlerRegistry<T> registry : this.registries) {
            if (registry.getHandler(handlerName) != null) {
                return registry;
            }
        }
        throw new UnsupportedOperationException("No known regsitries exist for AbstractHandler by name : " + handlerName);
    }

    /**
     * Loads the proxy handler context from path specified in the HandlerConfigInfo. Looks for file by name ServiceProxyFrameworkConstants.SPRING_PROXY_HANDLER_CONFIG.
     * @param handlerConfigInfo containing absolute path to the proxy handler's configuration location i.e. folder
     */
    private void loadProxyHandlerContext(HandlerConfigInfo handlerConfigInfo) {
        // check if a context exists already for this config path
        for (HandlerConfigInfo loadedHandlerConfigInfo : this.handlerConfigInfoList) {
            if (loadedHandlerConfigInfo.equals(handlerConfigInfo)) {
                handlerConfigInfo = loadedHandlerConfigInfo;
                break;
            }
        }
        if (handlerConfigInfo.getProxyHandlerContext() != null) {
            // close the context and remove from list
            handlerConfigInfo.getProxyHandlerContext().close();
            this.handlerConfigInfoList.remove(handlerConfigInfo);
        }
        ClassLoader proxyHandlerCL = this.tccl;
        // check to see if the proxy has handler and dependent binaries deployed outside of the runtime class path. If yes, include them using a custom URL classloader.
        File customLibPath = new File(handlerConfigInfo.getXmlConfigFile().getParentFile(), HandlerConfigInfo.BINARIES_PATH);
        if (customLibPath.exists() && customLibPath.isDirectory()) {
            try {
                File[] libFiles = customLibPath.listFiles();
                URL[] libURLs = new URL[libFiles.length];
                for (int i = 0; i < libFiles.length; i++) {
                    libURLs[i] = new URL(HandlerConfigInfo.FILE_PREFIX + libFiles[i].getAbsolutePath());
                }
                proxyHandlerCL = new URLClassLoader(libURLs, this.tccl);
            } catch (MalformedURLException e) {
                throw new PlatformException(e);
            }
        }
        // now load the proxy handler context and add it into the handlerConfigInfoList list
        handlerConfigInfo.loadProxyHandlerContext(proxyHandlerCL, ServiceProxyComponentContainer.getCommonProxyHandlerBeansContext());
        this.handlerConfigInfoList.add(handlerConfigInfo);
    }

}
