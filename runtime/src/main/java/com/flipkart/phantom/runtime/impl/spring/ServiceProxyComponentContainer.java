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

import com.flipkart.phantom.runtime.ServiceProxyFrameworkConstants;
import com.flipkart.phantom.runtime.impl.notifier.HystrixEventReceiver;
import com.flipkart.phantom.runtime.impl.server.AbstractNetworkServer;
import com.flipkart.phantom.runtime.impl.spring.admin.SPConfigServiceImpl;
import com.flipkart.phantom.runtime.spi.spring.admin.SPConfigService;
import com.flipkart.phantom.task.spi.registry.AbstractHandlerRegistry;
import com.flipkart.phantom.task.spi.TaskContext;
import com.flipkart.phantom.task.spi.registry.ProxyHandlerConfigInfo;
import com.netflix.hystrix.Hystrix;
import com.netflix.hystrix.strategy.HystrixPlugins;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.trpr.platform.core.PlatformException;
import org.trpr.platform.core.spi.event.PlatformEventProducer;
import org.trpr.platform.model.event.PlatformEvent;
import org.trpr.platform.runtime.common.RuntimeConstants;
import org.trpr.platform.runtime.common.RuntimeVariables;
import org.trpr.platform.runtime.impl.bootstrapext.spring.ApplicationContextFactory;
import org.trpr.platform.runtime.impl.config.FileLocator;
import org.trpr.platform.runtime.spi.bootstrapext.BootstrapExtension;
import org.trpr.platform.runtime.spi.component.ComponentContainer;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

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
public class ServiceProxyComponentContainer  implements ComponentContainer {
	
	/**
	 * The default Event producer bean name 
	 */
	private static final String DEFAULT_EVENT_PRODUCER = "platformEventProducer";

	/** The prefix to be added to file absolute paths when loading Spring XMLs using the FileSystemXmlApplicationContext*/
	private static final String FILE_PREFIX = "file:";	

	/** The bean names of the service proxy framework classes initialized by this container */
	private static final String CONFIG_SERVICE_BEAN = "configService";
	private static final String TASK_CONTEXT_BEAN = "taskContext";

	/** Logger for this class*/
	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceProxyComponentContainer.class);

	/** The common proxy handler beans context*/
	private static AbstractApplicationContext commonProxyHandlerBeansContext;    

	/** The list of ProxyHandlerConfigInfo holding all proxy handler instances loaded by this container */
	private List<ProxyHandlerConfigInfo> proxyHandlerContextsList = new LinkedList<ProxyHandlerConfigInfo>();

	/** Local reference for all BootstrapExtensionS loaded by the Container and set on this ComponentContainer*/
	private BootstrapExtension[] loadedBootstrapExtensions;

	/** The Thread's context class loader that is used in on the fly loading of proxy handler definitions */
	private ClassLoader tccl;

    /** The list of registered registries */
    private List<AbstractHandlerRegistry> registries = new ArrayList<AbstractHandlerRegistry>();

	/** The configService instance */
	private SPConfigService configService;

	/** The TaskContext bean instance*/
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
	public void setLoadedBootstrapExtensions(BootstrapExtension...bootstrapExtensions) {
		this.loadedBootstrapExtensions = bootstrapExtensions;
	}

	/**
	 * Interface method implementation. Locates and loads all configured proxy handlers.
	 * @see ComponentContainer#init()
	 */
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
				defaultCtxFactory = (ApplicationContextFactory)be;
				break;
			}
		}

		ServiceProxyComponentContainer.commonProxyHandlerBeansContext = new ClassPathXmlApplicationContext(new String[]{ServiceProxyFrameworkConstants.COMMON_PROXY_CONFIG},defaultCtxFactory.getCommonBeansContext());
		// add the common proxy beans independently to the list of proxy handler contexts as common handlers are declared there
		this.proxyHandlerContextsList.add(new ProxyHandlerConfigInfo(new File(ServiceProxyFrameworkConstants.COMMON_PROXY_CONFIG), null, ServiceProxyComponentContainer.commonProxyHandlerBeansContext));		

		// Get the Config Service Bean
		this.configService = (SPConfigServiceImpl)ServiceProxyComponentContainer.commonProxyHandlerBeansContext.getBean(ServiceProxyComponentContainer.CONFIG_SERVICE_BEAN);
        ((SPConfigServiceImpl)this.configService).setComponentContainer(this);

		// Load additional if runtime nature is "server". This context is the new common beans context
		if (RuntimeVariables.getRuntimeNature().equalsIgnoreCase(RuntimeConstants.SERVER)) {
			ServiceProxyComponentContainer.commonProxyHandlerBeansContext = new ClassPathXmlApplicationContext(new String[]{ServiceProxyFrameworkConstants.COMMON_PROXY_SERVER_NATURE_CONFIG},
					ServiceProxyComponentContainer.commonProxyHandlerBeansContext);
			// now add the common server nature proxy hander beans to the contexts list
			this.proxyHandlerContextsList.add(new ProxyHandlerConfigInfo(new File(ServiceProxyFrameworkConstants.COMMON_PROXY_SERVER_NATURE_CONFIG), null, 
					ServiceProxyComponentContainer.commonProxyHandlerBeansContext));
		}

       // // locate and load the individual proxy handler bean XML files using the common proxy handler beans context as parent
        File[] proxyHandlerBeansFiles = FileLocator.findFiles(ServiceProxyFrameworkConstants.SPRING_PROXY_HANDLER_CONFIG);
        for (File proxyHandlerBeansFile : proxyHandlerBeansFiles) {
            ProxyHandlerConfigInfo proxyHandlerConfigInfo = new ProxyHandlerConfigInfo(proxyHandlerBeansFile);
            // load the proxy handler's appcontext
            this.loadProxyHandlerContext(proxyHandlerConfigInfo);
        }

		// add the proxy listener beans to the contexts list (these have the thrift handlers)
		File[] proxyListenerBeanFiles = FileLocator.findFiles(ServiceProxyFrameworkConstants.SPRING_PROXY_LISTENER_CONFIG);			
		for (File proxyListenerBeanFile : proxyListenerBeanFiles) {
			// locate and load the service proxy listener defined in the file identified by {@link ServiceProxyFrameworkConstants#SPRING_PROXY_LISTENER_CONFIG}
			AbstractApplicationContext listenerContext = new FileSystemXmlApplicationContext(
					new String[] {FILE_PREFIX + proxyListenerBeanFile.getAbsolutePath()},
					ServiceProxyComponentContainer.commonProxyHandlerBeansContext);
			this.proxyHandlerContextsList.add(new ProxyHandlerConfigInfo(proxyListenerBeanFile, null, listenerContext));
		}

        // load all registries
        for (ProxyHandlerConfigInfo proxyHandlerConfigInfo : proxyHandlerContextsList) {

            // handler registries
            String[] registryBeans = proxyHandlerConfigInfo.getProxyHandlerContext().getBeanNamesForType(AbstractHandlerRegistry.class);
            for (String registryBean:registryBeans) {
                AbstractHandlerRegistry registry = (AbstractHandlerRegistry) proxyHandlerConfigInfo.getProxyHandlerContext().getBean(registryBean);
                // init the Registry
                try {
                    this.taskContext = (TaskContext) ServiceProxyComponentContainer.commonProxyHandlerBeansContext.getBean(ServiceProxyComponentContainer.TASK_CONTEXT_BEAN);
                    registry.init(this.proxyHandlerContextsList,this.taskContext);
                } catch (Exception e) {
                    LOGGER.error("Error initializing registry: " + registry.getClass().getName());
                    throw new PlatformException("Error initializing registry: " + registry.getClass().getName(), e);
                }
                // add registry to config
                configService.addHandlerRegistry(registry);
            }

            // add all network servers to config
            String[] networkServerBeans = proxyHandlerConfigInfo.getProxyHandlerContext().getBeanNamesForType(AbstractNetworkServer.class);
            for (String networkServerBean : networkServerBeans) {
                AbstractNetworkServer networkServer = (AbstractNetworkServer) proxyHandlerConfigInfo.getProxyHandlerContext().getBean(networkServerBean);
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
        for (AbstractHandlerRegistry registry:registries) {
            try {
                registry.shutdown(taskContext);
            } catch (Exception e) {
                LOGGER.warn("Error shutting down registry: " + registry.getClass().getName());
            }
        }
        // finally close the context
		for (ProxyHandlerConfigInfo proxyHandlerConfigInfo : this.proxyHandlerContextsList) {
			proxyHandlerConfigInfo.getProxyHandlerContext().close();
		}
		this.proxyHandlerContextsList = null;
	}

	/**
	 * Interface method implementation. Publishes the specified event to using a named bean DEFAULT_EVENT_PRODUCER looked up from the 
	 * common proxy handler context (i.e. ServiceProxyFrameworkConstants.COMMON_PROXY_CONFIG).
	 * Note that typically no consumers are registered when running this container
	 */ 
	public void publishEvent(PlatformEvent event) {
		PlatformEventProducer publisher= (PlatformEventProducer)ServiceProxyComponentContainer.commonProxyHandlerBeansContext.getBean(DEFAULT_EVENT_PRODUCER);
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
	 * Interface method implementation. Loads/Reloads proxy handler(s) defined in the specified {@link FileSystemResource} 
	 * @see org.trpr.platform.runtime.spi.component.ComponentContainer#loadComponent(org.springframework.core.io.Resource)
	 */
	public void loadComponent(Resource resource) {
		if (!FileSystemResource.class.isAssignableFrom(resource.getClass()) || 
				!resource.getFilename().equalsIgnoreCase(ServiceProxyFrameworkConstants.SPRING_PROXY_HANDLER_CONFIG)) {
			throw new UnsupportedOperationException("Proxy handers can be loaded only from files by name : " + 
					ServiceProxyFrameworkConstants.SPRING_PROXY_HANDLER_CONFIG + ". Specified resource is : " + resource.toString());
		}
		loadProxyHandlerContext(new ProxyHandlerConfigInfo(((FileSystemResource)resource).getFile()));
	}

	/**
	 * Loads the proxy handler context from path specified in the ProxyHandlerConfigInfo. Looks for file by name ServiceProxyFrameworkConstants.SPRING_PROXY_HANDLER_CONFIG. 
	 * @param proxyHandlerConfigInfo containing absolute path to the proxy handler's configuration location i.e. folder
	 */
	private void loadProxyHandlerContext(ProxyHandlerConfigInfo proxyHandlerConfigInfo) {
		// check if a context exists already for this config path 
		for (ProxyHandlerConfigInfo loadedProxyHandlerConfigInfo : this.proxyHandlerContextsList) {
			if (loadedProxyHandlerConfigInfo.equals(proxyHandlerConfigInfo)) {
				proxyHandlerConfigInfo = loadedProxyHandlerConfigInfo;
				break;
			}
		}
		if (proxyHandlerConfigInfo.getProxyHandlerContext() != null) {
			// close the context and remove from list
			proxyHandlerConfigInfo.getProxyHandlerContext().close();
			this.proxyHandlerContextsList.remove(proxyHandlerConfigInfo);
		}
		ClassLoader proxyHandlerCL = this.tccl;
		// check to see if the proxy has handler and dependent binaries deployed outside of the runtime class path. If yes, include them using a custom URL classloader.
		File customLibPath = new File (proxyHandlerConfigInfo.getProxyHandlerConfigXML().getParentFile(), ProxyHandlerConfigInfo.BINARIES_PATH);
		if (customLibPath.exists() && customLibPath.isDirectory()) {
			try {
				File[] libFiles = customLibPath.listFiles();
				URL[] libURLs = new URL[libFiles.length];
				for (int i=0; i < libFiles.length; i++) {
					libURLs[i] = new URL(ProxyHandlerConfigInfo.FILE_PREFIX + libFiles[i].getAbsolutePath());
				}
				proxyHandlerCL = new URLClassLoader(libURLs, this.tccl);
			} catch (MalformedURLException e) {
				throw new PlatformException(e);
			}
		} 
		// now load the proxy handler context and add it into the proxyHandlerContextsList list
		proxyHandlerConfigInfo.loadProxyHandlerContext(proxyHandlerCL,ServiceProxyComponentContainer.getCommonProxyHandlerBeansContext());
		this.proxyHandlerContextsList.add(proxyHandlerConfigInfo);
	}

}
