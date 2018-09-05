/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.camel.api.management.mbean.ManagedCamelContextMBean;
import org.apache.camel.api.management.mbean.ManagedProcessorMBean;
import org.apache.camel.api.management.mbean.ManagedRouteMBean;
import org.apache.camel.builder.ErrorHandlerBuilder;
import org.apache.camel.health.HealthCheckRegistry;
import org.apache.camel.management.ManagedCamelContext;
import org.apache.camel.model.DataFormatDefinition;
import org.apache.camel.model.HystrixConfigurationDefinition;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.model.cloud.ServiceCallConfigurationDefinition;
import org.apache.camel.model.rest.RestDefinition;
import org.apache.camel.model.rest.RestsDefinition;
import org.apache.camel.model.transformer.TransformerDefinition;
import org.apache.camel.model.validator.ValidatorDefinition;
import org.apache.camel.runtimecatalog.RuntimeCamelCatalog;
import org.apache.camel.spi.AsyncProcessorAwaitManager;
import org.apache.camel.spi.CamelContextNameStrategy;
import org.apache.camel.spi.ClassResolver;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.DataFormatResolver;
import org.apache.camel.spi.DataType;
import org.apache.camel.spi.Debugger;
import org.apache.camel.spi.EndpointRegistry;
import org.apache.camel.spi.EndpointStrategy;
import org.apache.camel.spi.ExecutorServiceManager;
import org.apache.camel.spi.FactoryFinder;
import org.apache.camel.spi.FactoryFinderResolver;
import org.apache.camel.spi.HeadersMapFactory;
import org.apache.camel.spi.InflightRepository;
import org.apache.camel.spi.Injector;
import org.apache.camel.spi.InterceptStrategy;
import org.apache.camel.spi.Language;
import org.apache.camel.spi.LifecycleStrategy;
import org.apache.camel.spi.LogListener;
import org.apache.camel.spi.ManagementMBeanAssembler;
import org.apache.camel.spi.ManagementNameStrategy;
import org.apache.camel.spi.ManagementStrategy;
import org.apache.camel.spi.MessageHistoryFactory;
import org.apache.camel.spi.ModelJAXBContextFactory;
import org.apache.camel.spi.NodeIdFactory;
import org.apache.camel.spi.PackageScanClassResolver;
import org.apache.camel.spi.ProcessorFactory;
import org.apache.camel.spi.Registry;
import org.apache.camel.spi.ReloadStrategy;
import org.apache.camel.spi.RestConfiguration;
import org.apache.camel.spi.RestRegistry;
import org.apache.camel.spi.RouteController;
import org.apache.camel.spi.RoutePolicyFactory;
import org.apache.camel.spi.RouteStartupOrder;
import org.apache.camel.spi.RuntimeEndpointRegistry;
import org.apache.camel.spi.ServicePool;
import org.apache.camel.spi.ShutdownStrategy;
import org.apache.camel.spi.StreamCachingStrategy;
import org.apache.camel.spi.Transformer;
import org.apache.camel.spi.TransformerRegistry;
import org.apache.camel.spi.TypeConverterRegistry;
import org.apache.camel.spi.UnitOfWorkFactory;
import org.apache.camel.spi.UuidGenerator;
import org.apache.camel.spi.Validator;
import org.apache.camel.spi.ValidatorRegistry;
import org.apache.camel.util.LoadPropertiesException;
import org.apache.camel.util.ValueHolder;
import org.apache.camel.util.jsse.SSLContextParameters;

/**
 * Interface used to represent the CamelContext used to configure routes and the
 * policies to use during message exchanges between endpoints.
 * <p/>
 * The CamelContext offers the following methods to control the lifecycle:
 * <ul>
 *   <li>{@link #start()}  - to start (<b>important:</b> the start method is not blocked, see more details
 *     <a href="http://camel.apache.org/running-camel-standalone-and-have-it-keep-running.html">here</a>)</li>
 *   <li>{@link #stop()} - to shutdown (will stop all routes/components/endpoints etc and clear internal state/cache)</li>
 *   <li>{@link #suspend()} - to pause routing messages</li>
 *   <li>{@link #resume()} - to resume after a suspend</li>
 * </ul>
 * <p/>
 * <b>Notice:</b> {@link #stop()} and {@link #suspend()} will gracefully stop/suspend routes ensuring any messages
 * in progress will be given time to complete. See more details at {@link org.apache.camel.spi.ShutdownStrategy}.
 * <p/>
 * If you are doing a hot restart then it's advised to use the suspend/resume methods which ensure a faster
 * restart but also allows any internal state to be kept as is.
 * The stop/start approach will do a <i>cold</i> restart of Camel, where all internal state is reset.
 * <p/>
 * End users are advised to use suspend/resume. Using stop is for shutting down Camel and it's not guaranteed that
 * when it's being started again using the start method that Camel will operate consistently.
 *
 * @version 
 */
public interface CamelContext extends SuspendableService, RuntimeConfiguration {

    /**
     * Adapts this {@link org.apache.camel.CamelContext} to the specialized type.
     * <p/>
     * For example to adapt to {@link org.apache.camel.model.ModelCamelContext},
     * or <tt>SpringCamelContext</tt>, or <tt>CdiCamelContext</tt>, etc.
     *
     * @param type the type to adapt to
     * @return this {@link org.apache.camel.CamelContext} adapted to the given type
     */
    <T extends CamelContext> T adapt(Class<T> type);

    /**
     * If CamelContext during the start procedure was vetoed, and therefore causing Camel to not start.
     */
    boolean isVetoStarted();

    /**
     * Starts the {@link CamelContext} (<b>important:</b> the start method is not blocked, see more details
     *     <a href="http://camel.apache.org/running-camel-standalone-and-have-it-keep-running.html">here</a>)</li>.
     * <p/>
     * See more details at the class-level javadoc of this class.
     *
     * @throws Exception is thrown if starting failed
     */
    void start() throws Exception;

    /**
     * Stop and shutdown the {@link CamelContext} (will stop all routes/components/endpoints etc and clear internal state/cache).
     * <p/>
     * See more details at the class-level javadoc of this class.
     *
     * @throws Exception is thrown if stopping failed
     */
    void stop() throws Exception;

    /**
     * Gets the name (id) of the this CamelContext.
     *
     * @return the name
     */
    String getName();

    /**
     * Gets the current name strategy
     *
     * @return name strategy
     */
    CamelContextNameStrategy getNameStrategy();

    /**
     * Gets the current management name strategy
     *
     * @return management name strategy
     */
    ManagementNameStrategy getManagementNameStrategy();

    /**
     * Gets the name this {@link CamelContext} was registered in JMX.
     * <p/>
     * The reason that a {@link CamelContext} can have a different name in JMX is the fact to remedy for name clash
     * in JMX when having multiple {@link CamelContext}s in the same JVM. Camel will automatic reassign and use
     * a free name to avoid failing to start.
     *
     * @return the management name
     */
    String getManagementName();

    /**
     * Gets the version of the this CamelContext.
     *
     * @return the version
     */
    String getVersion();

    /**
     * Get the status of this CamelContext
     *
     * @return the status
     */
    ServiceStatus getStatus();

    /**
     * Gets the uptime in a human readable format
     *
     * @return the uptime in days/hours/minutes
     */
    String getUptime();

    /**
     * Gets the uptime in milli seconds
     *
     * @return the uptime in millis seconds
     */
    long getUptimeMillis();

    // Service Methods
    //-----------------------------------------------------------------------

    /**
     * Adds a service to this CamelContext, which allows this CamelContext to control the lifecycle, ensuring
     * the service is stopped when the CamelContext stops.
     * <p/>
     * The service will also have {@link CamelContext} injected if its {@link CamelContextAware}.
     * The service will also be enlisted in JMX for management (if JMX is enabled).
     * The service will be started, if its not already started.
     *
     * @param object the service
     * @throws Exception can be thrown when starting the service
     */
    void addService(Object object) throws Exception;

    /**
     * Adds a service to this CamelContext.
     * <p/>
     * The service will also have {@link CamelContext} injected if its {@link CamelContextAware}.
     * The service will also be enlisted in JMX for management (if JMX is enabled).
     * The service will be started, if its not already started.
     * <p/>
     * If the option <tt>closeOnShutdown</tt> is <tt>true</tt> then this CamelContext will control the lifecycle, ensuring
     * the service is stopped when the CamelContext stops.
     * If the option <tt>closeOnShutdown</tt> is <tt>false</tt> then this CamelContext will not stop the service when the CamelContext stops.
     *
     * @param object the service
     * @param stopOnShutdown whether to stop the service when this CamelContext shutdown.
     * @throws Exception can be thrown when starting the service
     */
    void addService(Object object, boolean stopOnShutdown) throws Exception;

    /**
     * Adds a service to this CamelContext.
     * <p/>
     * The service will also have {@link CamelContext} injected if its {@link CamelContextAware}.
     * The service will also be enlisted in JMX for management (if JMX is enabled).
     * The service will be started, if its not already started.
     * <p/>
     * If the option <tt>closeOnShutdown</tt> is <tt>true</tt> then this CamelContext will control the lifecycle, ensuring
     * the service is stopped when the CamelContext stops.
     * If the option <tt>closeOnShutdown</tt> is <tt>false</tt> then this CamelContext will not stop the service when the CamelContext stops.
     *
     * @param object the service
     * @param stopOnShutdown whether to stop the service when this CamelContext shutdown.
     * @param forceStart whether to force starting the service right now, as otherwise the service may be deferred being started
     *                   to later using {@link #deferStartService(Object, boolean)}
     * @throws Exception can be thrown when starting the service
     */
    void addService(Object object, boolean stopOnShutdown, boolean forceStart) throws Exception;

    /**
     * Removes a service from this CamelContext.
     * <p/>
     * The service is assumed to have been previously added using {@link #addService(Object)} method.
     * This method will <b>not</b> change the service lifecycle.
     *
     * @param object the service
     * @throws Exception can be thrown if error removing the service
     * @return <tt>true</tt> if the service was removed, <tt>false</tt> if no service existed
     */
    boolean removeService(Object object) throws Exception;

    /**
     * Has the given service already been added to this CamelContext?
     *
     * @param object the service
     * @return <tt>true</tt> if already added, <tt>false</tt> if not.
     */
    boolean hasService(Object object);

    /**
     * Has the given service type already been added to this CamelContext?
     *
     * @param type the class type
     * @return the service instance or <tt>null</tt> if not already added.
     */
    <T> T hasService(Class<T> type);

    /**
     * Has the given service type already been added to this CamelContext?
     *
     * @param type the class type
     * @return the services instance or empty set.
     */
    <T> Set<T> hasServices(Class<T> type);

    /**
     * Defers starting the service until {@link CamelContext} is (almost started) or started and has initialized all its prior services and routes.
     * <p/>
     * If {@link CamelContext} is already started then the service is started immediately.
     *
     * @param object the service
     * @param stopOnShutdown whether to stop the service when this CamelContext shutdown. Setting this to <tt>true</tt> will keep a reference to the service in
     *                       this {@link CamelContext} until the CamelContext is stopped. So do not use it for short lived services.
     * @throws Exception can be thrown when starting the service, which is only attempted if {@link CamelContext} has already been started when calling this method.
     */
    void deferStartService(Object object, boolean stopOnShutdown) throws Exception;

    /**
     * Adds the given listener to be invoked when {@link CamelContext} have just been started.
     * <p/>
     * This allows listeners to do any custom work after the routes and other services have been started and are running.
     * <p/><b>Important:</b> The listener will always be invoked, also if the {@link CamelContext} has already been
     * started, see the {@link org.apache.camel.StartupListener#onCamelContextStarted(CamelContext, boolean)} method.
     *
     * @param listener the listener
     * @throws Exception can be thrown if {@link CamelContext} is already started and the listener is invoked
     *                   and cause an exception to be thrown
     */
    void addStartupListener(StartupListener listener) throws Exception;

    // Component Management Methods
    //-----------------------------------------------------------------------

    /**
     * Adds a component to the context.
     *
     * @param componentName the name the component is registered as
     * @param component     the component
     */
    void addComponent(String componentName, Component component);

    /**
     * Is the given component already registered?
     *
     * @param componentName the name of the component
     * @return the registered Component or <tt>null</tt> if not registered
     */
    Component hasComponent(String componentName);

    /**
     * Gets a component from the CamelContext by name.
     * <p/>
     * Notice the returned component will be auto-started. If you do not intend to do that
     * then use {@link #getComponent(String, boolean, boolean)}.
     *
     * @param componentName the name of the component
     * @return the component
     */
    Component getComponent(String componentName);

    /**
     * Gets a component from the CamelContext by name.
     * <p/>
     * Notice the returned component will be auto-started. If you do not intend to do that
     * then use {@link #getComponent(String, boolean, boolean)}.
     *
     * @param name                 the name of the component
     * @param autoCreateComponents whether or not the component should
     *                             be lazily created if it does not already exist
     * @return the component
     */
    Component getComponent(String name, boolean autoCreateComponents);

    /**
     * Gets a component from the CamelContext by name.
     *
     * @param name                 the name of the component
     * @param autoCreateComponents whether or not the component should
     *                             be lazily created if it does not already exist
     * @param autoStart            whether to auto start the component if {@link CamelContext} is already started.
     * @return the component
     */
    Component getComponent(String name, boolean autoCreateComponents, boolean autoStart);

    /**
     * Gets a component from the CamelContext by name and specifying the expected type of component.
     *
     * @param name          the name to lookup
     * @param componentType the expected type
     * @return the component
     */
    <T extends Component> T getComponent(String name, Class<T> componentType);

    /**
     * Gets a readonly list of names of the components currently registered
     *
     * @return a readonly list with the names of the components
     */
    List<String> getComponentNames();

    /**
     * Removes a previously added component.
     * <p/>
     * The component being removed will be stopped first.
     *
     * @param componentName the component name to remove
     * @return the previously added component or null if it had not been previously added.
     */
    Component removeComponent(String componentName);

    // Endpoint Management Methods
    //-----------------------------------------------------------------------

    /**
     * Gets the {@link org.apache.camel.spi.EndpointRegistry}
     */
    EndpointRegistry<? extends ValueHolder<String>> getEndpointRegistry();

    /**
     * Resolves the given name to an {@link Endpoint} of the specified type.
     * If the name has a singleton endpoint registered, then the singleton is returned.
     * Otherwise, a new {@link Endpoint} is created and registered in the {@link org.apache.camel.spi.EndpointRegistry}.
     *
     * @param uri the URI of the endpoint
     * @return the endpoint
     */
    Endpoint getEndpoint(String uri);

    /**
     * Resolves the given name to an {@link Endpoint} of the specified type.
     * If the name has a singleton endpoint registered, then the singleton is returned.
     * Otherwise, a new {@link Endpoint} is created and registered in the {@link org.apache.camel.spi.EndpointRegistry}.
     *
     * @param name         the name of the endpoint
     * @param endpointType the expected type
     * @return the endpoint
     */
    <T extends Endpoint> T getEndpoint(String name, Class<T> endpointType);

    /**
     * Returns a new {@link Collection} of all of the endpoints from the {@link org.apache.camel.spi.EndpointRegistry}
     *
     * @return all endpoints
     */
    Collection<Endpoint> getEndpoints();

    /**
     * Returns a new {@link Map} containing all of the endpoints from the {@link org.apache.camel.spi.EndpointRegistry}
     *
     * @return map of endpoints
     */
    Map<String, Endpoint> getEndpointMap();

    /**
     * Is the given endpoint already registered in the {@link org.apache.camel.spi.EndpointRegistry}
     *
     * @param uri the URI of the endpoint
     * @return the registered endpoint or <tt>null</tt> if not registered
     */
    Endpoint hasEndpoint(String uri);

    /**
     * Adds the endpoint to the {@link org.apache.camel.spi.EndpointRegistry} using the given URI.
     *
     * @param uri      the URI to be used to resolve this endpoint
     * @param endpoint the endpoint to be added to the registry
     * @return the old endpoint that was previously registered or <tt>null</tt> if none was registered
     * @throws Exception if the new endpoint could not be started or the old endpoint could not be stopped
     */
    Endpoint addEndpoint(String uri, Endpoint endpoint) throws Exception;

    /**
     * Removes the endpoint from the {@link org.apache.camel.spi.EndpointRegistry}.
     * <p/>
     * The endpoint being removed will be stopped first.
     *
     * @param endpoint  the endpoint
     * @throws Exception if the endpoint could not be stopped
     */
    void removeEndpoint(Endpoint endpoint) throws Exception;

    /**
     * Removes all endpoints with the given URI from the {@link org.apache.camel.spi.EndpointRegistry}.
     * <p/>
     * The endpoints being removed will be stopped first.
     *
     * @param pattern an uri or pattern to match
     * @return a collection of endpoints removed which could be empty if there are no endpoints found for the given <tt>pattern</tt>
     * @throws Exception if at least one endpoint could not be stopped
     * @see org.apache.camel.util.EndpointHelper#matchEndpoint(CamelContext, String, String) for pattern
     */
    Collection<Endpoint> removeEndpoints(String pattern) throws Exception;

    /**
     * Registers a {@link org.apache.camel.spi.EndpointStrategy callback} to allow you to do custom
     * logic when an {@link Endpoint} is about to be registered to the {@link org.apache.camel.spi.EndpointRegistry}.
     * <p/>
     * When a callback is added it will be executed on the already registered endpoints allowing you to catch-up
     *
     * @param strategy callback to be invoked
     */
    void addRegisterEndpointCallback(EndpointStrategy strategy);

    // Route Management Methods
    //-----------------------------------------------------------------------

    /**
     * NOTE: experimental api
     *
     * @return the route controller or null if not set.
     */
    RouteController getRouteController();

    /**
     * Method to signal to {@link CamelContext} that the process to initialize setup routes is in progress.
     *
     * @param done <tt>false</tt> to start the process, call again with <tt>true</tt> to signal its done.
     * @see #isSetupRoutes()
     */
    void setupRoutes(boolean done);

    /**
     * Adds a collection of rest definitions to the context
     *
     * @param restDefinitions the rest(s) definition to add
     */
    void addRestDefinitions(Collection<RestDefinition> restDefinitions) throws Exception;

    /**
     * Sets a custom {@link org.apache.camel.spi.RestConfiguration}
     *
     * @param restConfiguration the REST configuration
     */
    void setRestConfiguration(RestConfiguration restConfiguration);

    /**
     * Gets the default REST configuration
     *
     * @return the configuration, or <tt>null</tt> if none has been configured.
     */
    RestConfiguration getRestConfiguration();
    
    /**
     * Sets a custom {@link org.apache.camel.spi.RestConfiguration}
     *
     * @param restConfiguration the REST configuration
     */
    void addRestConfiguration(RestConfiguration restConfiguration);

    /**
     * Gets the REST configuration for the given component
     *
     * @param component the component name to get the configuration
     * @param defaultIfNotFound determine if the default configuration is returned if there isn't a 
     *        specific configuration for the given component  
     * @return the configuration, or <tt>null</tt> if none has been configured.
     */
    RestConfiguration getRestConfiguration(String component, boolean defaultIfNotFound);
    
    /**
     * Gets all the RestConfiguration's
     */
    Collection<RestConfiguration> getRestConfigurations();

    /**
     * Gets the service call configuration by the given name. If no name is given
     * the default configuration is returned, see <tt>setServiceCallConfiguration</tt>
     *
     * @param serviceName name of service, or <tt>null</tt> to return the default configuration
     * @return the configuration, or <tt>null</tt> if no configuration has been registered
     */
    ServiceCallConfigurationDefinition getServiceCallConfiguration(String serviceName);

    /**
     * Sets the default service call configuration
     *
     * @param configuration the configuration
     */
    void setServiceCallConfiguration(ServiceCallConfigurationDefinition configuration);

    /**
     * Sets the service call configurations
     *
     * @param configurations the configuration list
     */
    void setServiceCallConfigurations(List<ServiceCallConfigurationDefinition> configurations);

    /**
     * Adds the service call configuration
     *
     * @param serviceName name of the service
     * @param configuration the configuration
     */
    void addServiceCallConfiguration(String serviceName, ServiceCallConfigurationDefinition configuration);

    /**
     * Gets the Hystrix configuration by the given name. If no name is given
     * the default configuration is returned, see <tt>setHystrixConfiguration</tt>
     *
     * @param id id of the configuration, or <tt>null</tt> to return the default configuration
     * @return the configuration, or <tt>null</tt> if no configuration has been registered
     */
    HystrixConfigurationDefinition getHystrixConfiguration(String id);

    /**
     * Sets the default Hystrix configuration
     *
     * @param configuration the configuration
     */
    void setHystrixConfiguration(HystrixConfigurationDefinition configuration);

    /**
     * Sets the Hystrix configurations
     *
     * @param configurations the configuration list
     */
    void setHystrixConfigurations(List<HystrixConfigurationDefinition> configurations);

    /**
     * Adds the Hystrix configuration
     *
     * @param id name of the configuration
     * @param configuration the configuration
     */
    void addHystrixConfiguration(String id, HystrixConfigurationDefinition configuration);

    /**
     * Returns the order in which the route inputs was started.
     * <p/>
     * The order may not be according to the startupOrder defined on the route.
     * For example a route could be started manually later, or new routes added at runtime.
     *
     * @return a list in the order how routes was started
     */
    List<RouteStartupOrder> getRouteStartupOrder();

    /**
     * Returns the current routes in this CamelContext
     *
     * @return the current routes
     */
    List<Route> getRoutes();

    /**
     * Gets the route with the given id
     *
     * @param id id of the route
     * @return the route or <tt>null</tt> if not found
     */
    Route getRoute(String id);

    /**
     * Gets the processor from any of the routes which with the given id
     *
     * @param id id of the processor
     * @return the processor or <tt>null</tt> if not found
     */
    Processor getProcessor(String id);

    /**
     * Gets the processor from any of the routes which with the given id
     *
     * @param id id of the processor
     * @param type the processor type
     * @return the processor or <tt>null</tt> if not found
     * @throws java.lang.ClassCastException is thrown if the type is not correct type
     */
    <T extends Processor> T getProcessor(String id, Class<T> type);

    /**
     * Removes the given route (the route <b>must</b> be stopped before it can be removed).
     * <p/>
     * A route which is removed will be unregistered from JMX, have its services stopped/shutdown and the route
     * definition etc. will also be removed. All the resources related to the route will be stopped and cleared.
     * <p/>
     * <b>Important:</b> When removing a route, the {@link Endpoint}s which are in the static cache of
     * {@link org.apache.camel.spi.EndpointRegistry} and are <b>only</b> used by the route (not used by other routes)
     * will also be removed. But {@link Endpoint}s which may have been created as part of routing messages by the route,
     * and those endpoints are enlisted in the dynamic cache of {@link org.apache.camel.spi.EndpointRegistry} are
     * <b>not</b> removed. To remove those dynamic kind of endpoints, use the {@link #removeEndpoints(String)} method.
     * If not removing those endpoints, they will be kept in the dynamic cache of {@link org.apache.camel.spi.EndpointRegistry},
     * but my eventually be removed (evicted) when they have not been in use for a longer period of time; and the
     * dynamic cache upper limit is hit, and it evicts the least used endpoints.
     * <p/>
     * End users can use this method to remove unwanted routes or temporary routes which no longer is in demand.
     *
     * @param routeId the route id
     * @return <tt>true</tt> if the route was removed, <tt>false</tt> if the route could not be removed because its not stopped
     * @throws Exception is thrown if the route could not be shutdown for whatever reason
     */
    boolean removeRoute(String routeId) throws Exception;

    /**
     * Indicates whether current thread is setting up route(s) as part of starting Camel from spring/blueprint.
     * <p/>
     * This can be useful to know by {@link LifecycleStrategy} or the likes, in case
     * they need to react differently.
     * <p/>
     * As the startup procedure of {@link CamelContext} is slightly different when using plain Java versus
     * Spring or Blueprint, then we need to know when Spring/Blueprint is setting up the routes, which
     * can happen after the {@link CamelContext} itself is in started state, due the asynchronous event nature
     * of especially Blueprint.
     *
     * @return <tt>true</tt> if current thread is setting up route(s), or <tt>false</tt> if not.
     */
    boolean isSetupRoutes();

    // Properties
    //-----------------------------------------------------------------------

    /**
     * Returns the type converter used to coerce types from one type to another
     *
     * @return the converter
     */
    TypeConverter getTypeConverter();

    /**
     * Returns the type converter registry where type converters can be added or looked up
     *
     * @return the type converter registry
     */
    TypeConverterRegistry getTypeConverterRegistry();

    /**
     * Returns the registry used to lookup components by name and type such as the Spring ApplicationContext,
     * JNDI or the OSGi Service Registry
     *
     * @return the registry
     */
    Registry getRegistry();

    /**
     * Returns the registry used to lookup components by name and as the given type
     *
     * @param type the registry type such as {@link org.apache.camel.impl.JndiRegistry}
     * @return the registry, or <tt>null</tt> if the given type was not found as a registry implementation
     */
    <T> T getRegistry(Class<T> type);

    /**
     * Returns the injector used to instantiate objects by type
     *
     * @return the injector
     */
    Injector getInjector();

    /**
     * Returns the management mbean assembler
     *
     * @return the mbean assembler
     */
    ManagementMBeanAssembler getManagementMBeanAssembler();

    /**
     * Returns the lifecycle strategies used to handle lifecycle notifications
     *
     * @return the lifecycle strategies
     */
    List<LifecycleStrategy> getLifecycleStrategies();

    /**
     * Adds the given lifecycle strategy to be used.
     *
     * @param lifecycleStrategy the strategy
     */
    void addLifecycleStrategy(LifecycleStrategy lifecycleStrategy);

    /**
     * Resolves a language for creating expressions
     *
     * @param language name of the language
     * @return the resolved language
     */
    Language resolveLanguage(String language);

    /**
     * Parses the given text and resolve any property placeholders - using {{key}}.
     *
     * @param text the text such as an endpoint uri or the likes
     * @return the text with resolved property placeholders
     * @throws Exception is thrown if property placeholders was used and there was an error resolving them
     */
    String resolvePropertyPlaceholders(String text) throws Exception;
    
    /**
     * Returns the configured property placeholder prefix token if and only if the CamelContext has
     * property placeholder abilities, otherwise returns {@code null}.
     * 
     * @return the prefix token or {@code null}
     */
    String getPropertyPrefixToken();
    
    /**
     * Returns the configured property placeholder suffix token if and only if the CamelContext has
     * property placeholder abilities, otherwise returns {@code null}.
     * 
     * @return the suffix token or {@code null}
     */
    String getPropertySuffixToken();

    /**
     * Gets a readonly list with the names of the languages currently registered.
     *
     * @return a readonly list with the names of the languages
     */
    List<String> getLanguageNames();

    /**
     * Creates a new {@link ProducerTemplate} which is <b>started</b> and therefore ready to use right away.
     * <p/>
     * See this FAQ before use: <a href="http://camel.apache.org/why-does-camel-use-too-many-threads-with-producertemplate.html">
     * Why does Camel use too many threads with ProducerTemplate?</a>
     * <p/>
     * <b>Important:</b> Make sure to call {@link org.apache.camel.ProducerTemplate#stop()} when you are done using the template,
     * to clean up any resources.
     * <p/>
     * Will use cache size defined in Camel property with key {@link Exchange#MAXIMUM_CACHE_POOL_SIZE}.
     * If no key was defined then it will fallback to a default size of 1000.
     * You can also use the {@link org.apache.camel.ProducerTemplate#setMaximumCacheSize(int)} method to use a custom value
     * before starting the template.
     *
     * @return the template
     * @throws RuntimeCamelException is thrown if error starting the template
     */
    ProducerTemplate createProducerTemplate();

    /**
     * Creates a new {@link ProducerTemplate} which is <b>started</b> and therefore ready to use right away.
     * <p/>
     * See this FAQ before use: <a href="http://camel.apache.org/why-does-camel-use-too-many-threads-with-producertemplate.html">
     * Why does Camel use too many threads with ProducerTemplate?</a>
     * <p/>
     * <b>Important:</b> Make sure to call {@link ProducerTemplate#stop()} when you are done using the template,
     * to clean up any resources.
     *
     * @param maximumCacheSize the maximum cache size
     * @return the template
     * @throws RuntimeCamelException is thrown if error starting the template
     */
    ProducerTemplate createProducerTemplate(int maximumCacheSize);

    /**
     * Creates a new {@link FluentProducerTemplate} which is <b>started</b> and therefore ready to use right away.
     * <p/>
     * See this FAQ before use: <a href="http://camel.apache.org/why-does-camel-use-too-many-threads-with-producertemplate.html">
     * Why does Camel use too many threads with ProducerTemplate?</a>
     * <p/>
     * <b>Important:</b> Make sure to call {@link org.apache.camel.FluentProducerTemplate#stop()} when you are done using the template,
     * to clean up any resources.
     * <p/>
     * Will use cache size defined in Camel property with key {@link Exchange#MAXIMUM_CACHE_POOL_SIZE}.
     * If no key was defined then it will fallback to a default size of 1000.
     * You can also use the {@link org.apache.camel.FluentProducerTemplate#setMaximumCacheSize(int)} method to use a custom value
     * before starting the template.
     *
     * @return the template
     * @throws RuntimeCamelException is thrown if error starting the template
     */
    FluentProducerTemplate createFluentProducerTemplate();

    /**
     * Creates a new {@link FluentProducerTemplate} which is <b>started</b> and therefore ready to use right away.
     * <p/>
     * See this FAQ before use: <a href="http://camel.apache.org/why-does-camel-use-too-many-threads-with-producertemplate.html">
     * Why does Camel use too many threads with ProducerTemplate?</a>
     * <p/>
     * <b>Important:</b> Make sure to call {@link FluentProducerTemplate#stop()} when you are done using the template,
     * to clean up any resources.
     *
     * @param maximumCacheSize the maximum cache size
     * @return the template
     * @throws RuntimeCamelException is thrown if error starting the template
     */
    FluentProducerTemplate createFluentProducerTemplate(int maximumCacheSize);

    /**
     * Creates a new {@link ConsumerTemplate} which is <b>started</b> and therefore ready to use right away.
     * <p/>
     * See this FAQ before use: <a href="http://camel.apache.org/why-does-camel-use-too-many-threads-with-producertemplate.html">
     * Why does Camel use too many threads with ProducerTemplate?</a> as it also applies for ConsumerTemplate.
     * <p/>
     * <b>Important:</b> Make sure to call {@link ConsumerTemplate#stop()} when you are done using the template,
     * to clean up any resources.
     * <p/>
     * Will use cache size defined in Camel property with key {@link Exchange#MAXIMUM_CACHE_POOL_SIZE}.
     * If no key was defined then it will fallback to a default size of 1000.
     * You can also use the {@link org.apache.camel.ConsumerTemplate#setMaximumCacheSize(int)} method to use a custom value
     * before starting the template.
     *
     * @return the template
     * @throws RuntimeCamelException is thrown if error starting the template
     */
    ConsumerTemplate createConsumerTemplate();

    /**
     * Creates a new {@link ConsumerTemplate} which is <b>started</b> and therefore ready to use right away.
     * <p/>
     * See this FAQ before use: <a href="http://camel.apache.org/why-does-camel-use-too-many-threads-with-producertemplate.html">
     * Why does Camel use too many threads with ProducerTemplate?</a> as it also applies for ConsumerTemplate.
     * <p/>
     * <b>Important:</b> Make sure to call {@link ConsumerTemplate#stop()} when you are done using the template,
     * to clean up any resources.
     *
     * @param maximumCacheSize the maximum cache size
     * @return the template
     * @throws RuntimeCamelException is thrown if error starting the template
     */
    ConsumerTemplate createConsumerTemplate(int maximumCacheSize);

    /**
     * Gets the interceptor strategies
     *
     * @return the list of current interceptor strategies
     */
    List<InterceptStrategy> getInterceptStrategies();

    /**
     * Gets the default error handler builder which is inherited by the routes
     *
     * @return the builder
     * @deprecated The return type will be switched to {@link ErrorHandlerFactory} in Camel 3.0
     */
    ErrorHandlerFactory getErrorHandlerFactory();

    /**
     * Gets the default shared thread pool for error handlers which
     * leverages this for asynchronous redelivery tasks.
     */
    ScheduledExecutorService getErrorHandlerExecutorService();

    /**
     * Sets the data formats that can be referenced in the routes.
     *
     * @param dataFormats the data formats
     */
    void setDataFormats(Map<String, DataFormatDefinition> dataFormats);

    /**
     * Gets the data formats that can be referenced in the routes.
     *
     * @return the data formats available
     */
    Map<String, DataFormatDefinition> getDataFormats();

    /**
     * Resolve a data format given its name
     *
     * @param name the data format name or a reference to it in the {@link Registry}
     * @return the resolved data format, or <tt>null</tt> if not found
     */
    DataFormat resolveDataFormat(String name);

    /**
     * Creates the given data format given its name.
     *
     * @param name the data format name or a reference to a data format factory in the {@link Registry}
     * @return the resolved data format, or <tt>null</tt> if not found
     */
    DataFormat createDataFormat(String name);

    /**
     * Resolve a data format definition given its name
     *
     * @param name the data format definition name or a reference to it in the {@link Registry}
     * @return the resolved data format definition, or <tt>null</tt> if not found
     */
    DataFormatDefinition resolveDataFormatDefinition(String name);

    /**
     * Gets the current data format resolver
     *
     * @return the resolver
     */
    DataFormatResolver getDataFormatResolver();

    /**
     * Sets the transformers that can be referenced in the routes.
     *
     * @param transformers the transformers
     */
    void setTransformers(List<TransformerDefinition> transformers);

    /**
     * Gets the transformers that can be referenced in the routes.
     *
     * @return the transformers available
     */
    List<TransformerDefinition> getTransformers();

    /**
     * Resolve a transformer given a scheme
     *
     * @param model data model name.
     * @return the resolved transformer, or <tt>null</tt> if not found
     */
    Transformer resolveTransformer(String model);

    /**
     * Resolve a transformer given from/to data type.
     *
     * @param from from data type
     * @param to to data type
     * @return the resolved transformer, or <tt>null</tt> if not found
     */
    Transformer resolveTransformer(DataType from, DataType to);

    /**
     * Gets the {@link org.apache.camel.spi.TransformerRegistry}
     * @return the TransformerRegistry
     */
    TransformerRegistry<? extends ValueHolder<String>> getTransformerRegistry();

    /**
     * Gets the validators that can be referenced in the routes.
     *
     * @return the validators available
     */
    List<ValidatorDefinition> getValidators();

    /**
     * Resolve a validator given from/to data type.
     *
     * @param type the data type
     * @return the resolved validator, or <tt>null</tt> if not found
     */
    Validator resolveValidator(DataType type);

    /**
     * Gets the {@link org.apache.camel.spi.ValidatorRegistry}
     * @return the ValidatorRegistry
     */
    ValidatorRegistry<? extends ValueHolder<String>> getValidatorRegistry();

    /**
     * Gets global options that can be referenced in the camel context.
     * <p/>
     * <b>Important:</b> This has nothing to do with property placeholders, and is just a plain set of key/value pairs
     * which are used to configure global options on CamelContext, such as a maximum debug logging length etc.
     * For property placeholders use {@link #resolvePropertyPlaceholders(String)} method and see more details
     * at the <a href="http://camel.apache.org/using-propertyplaceholder.html">property placeholder</a> documentation.
     *
     * @return global options for this context
     */
    Map<String, String> getGlobalOptions();

    /**
     * Gets the global option value that can be referenced in the camel context
     * <p/>
     * <b>Important:</b> This has nothing to do with property placeholders, and is just a plain set of key/value pairs
     * which are used to configure global options on CamelContext, such as a maximum debug logging length etc.
     * For property placeholders use {@link #resolvePropertyPlaceholders(String)} method and see more details
     * at the <a href="http://camel.apache.org/using-propertyplaceholder.html">property placeholder</a> documentation.
     *
     * @return the string value of the global option
     */
    String getGlobalOption(String key);

    /**
     * Gets the default FactoryFinder which will be used for the loading the factory class from META-INF
     *
     * @return the default factory finder
     */
    FactoryFinder getDefaultFactoryFinder();

    /**
     * Gets the FactoryFinder which will be used for the loading the factory class from META-INF in the given path
     *
     * @param path the META-INF path
     * @return the factory finder
     * @throws NoFactoryAvailableException is thrown if a factory could not be found
     */
    FactoryFinder getFactoryFinder(String path) throws NoFactoryAvailableException;

    /**
     * Returns the class resolver to be used for loading/lookup of classes.
     *
     * @return the resolver
     */
    ClassResolver getClassResolver();

    /**
     * Returns the package scanning class resolver
     *
     * @return the resolver
     */
    PackageScanClassResolver getPackageScanClassResolver();

    /**
     * Gets the node id factory
     *
     * @return the node id factory
     */
    NodeIdFactory getNodeIdFactory();

    /**
     * Gets the management strategy
     *
     * @return the management strategy
     */
    ManagementStrategy getManagementStrategy();

    /**
     * Gets the default tracer
     *
     * @return the default tracer
     */
    InterceptStrategy getDefaultTracer();

    /**
     * Gets the default backlog tracer
     *
     * @return the default backlog tracer
     */
    InterceptStrategy getDefaultBacklogTracer();

    /**
     * Gets the default backlog debugger
     *
     * @return the default backlog debugger
     */
    InterceptStrategy getDefaultBacklogDebugger();

    /**
     * Gets the inflight repository
     *
     * @return the repository
     */
    InflightRepository getInflightRepository();

    /**
     * Gets the {@link org.apache.camel.AsyncProcessor} await manager.
     *
     * @return the manager
     */
    AsyncProcessorAwaitManager getAsyncProcessorAwaitManager();

    /**
     * Gets the application CamelContext class loader which may be helpful for running camel in other containers
     *
     * @return the application CamelContext class loader
     */
    ClassLoader getApplicationContextClassLoader();

    /**
     * Gets the current shutdown strategy
     *
     * @return the strategy
     */
    ShutdownStrategy getShutdownStrategy();

    /**
     * Gets the current {@link org.apache.camel.spi.ExecutorServiceManager}
     *
     * @return the manager
     */
    ExecutorServiceManager getExecutorServiceManager();

    /**
     * Gets the current {@link org.apache.camel.spi.ProcessorFactory}
     *
     * @return the factory, can be <tt>null</tt> if no custom factory has been set
     */
    ProcessorFactory getProcessorFactory();

    /**
     * Gets the current {@link org.apache.camel.spi.MessageHistoryFactory}
     *
     * @return the factory
     */
    MessageHistoryFactory getMessageHistoryFactory();

    /**
     * Gets the current {@link Debugger}
     *
     * @return the debugger
     */
    Debugger getDebugger();

    /**
     * Gets the current {@link UuidGenerator}
     *
     * @return the uuidGenerator
     */
    UuidGenerator getUuidGenerator();
    
    /**
     * Sets whether to load custom type converters by scanning classpath.
     * This can be turned off if you are only using Camel components
     * that does not provide type converters which is needed at runtime.
     * In such situations setting this option to false, can speedup starting
     * Camel.
     */
    Boolean isLoadTypeConverters();

    /**
     * Sets whether to load custom type converters by scanning classpath.
     * This can be turned off if you are only using Camel components
     * that does not provide type converters which is needed at runtime.
     * In such situations setting this option to false, can speedup starting
     * Camel.
     *
     * @param loadTypeConverters whether to load custom type converters.
     */
    void setLoadTypeConverters(Boolean loadTypeConverters);

    /**
     * Whether or not type converter statistics is enabled.
     * <p/>
     * By default the type converter utilization statistics is disabled.
     * <b>Notice:</b> If enabled then there is a slight performance impact under very heavy load.
     *
     * @return <tt>true</tt> if enabled, <tt>false</tt> if disabled (default).
     */
    Boolean isTypeConverterStatisticsEnabled();

    /**
     * Whether or not <a href="http://www.slf4j.org/api/org/slf4j/MDC.html">MDC</a> logging is being enabled.
     *
     * @return <tt>true</tt> if MDC logging is enabled
     */
    Boolean isUseMDCLogging();

    /**
     * Whether to enable using data type on Camel messages.
     * <p/>
     * Data type are automatic turned on if one ore more routes has been explicit configured with input and output types.
     * Otherwise data type is default off.
     *
     * @return <tt>true</tt> if data type is enabled
     */
    Boolean isUseDataType();

    /**
     * Whether or not breadcrumb is enabled.
     *
     * @return <tt>true</tt> if breadcrumb is enabled
     */
    Boolean isUseBreadcrumb();

    /**
     * Resolves a component's default name from its java type.
     * <p/>
     * A component may be used with a non default name such as <tt>activemq</tt>, <tt>wmq</tt> for the JMS component.
     * This method can resolve the default component name by its java type.
     *
     * @param javaType the FQN name of the java type
     * @return the default component name.
     */
    String resolveComponentDefaultName(String javaType);

    /**
     * Find information about all the Camel components available in the classpath and {@link org.apache.camel.spi.Registry}.
     *
     * @return a map with the component name, and value with component details.
     * @throws LoadPropertiesException is thrown if error during classpath discovery of the components
     * @throws IOException is thrown if error during classpath discovery of the components
     */
    Map<String, Properties> findComponents() throws LoadPropertiesException, IOException;

    /**
     * Find information about all the EIPs from camel-core.
     *
     * @return a map with node id, and value with EIP details.
     * @throws LoadPropertiesException is thrown if error during classpath discovery of the EIPs
     * @throws IOException is thrown if error during classpath discovery of the EIPs
     */
    Map<String, Properties> findEips() throws LoadPropertiesException, IOException;

    /**
     * Returns the JSON schema representation of the component and endpoint parameters for the given component name.
     *
     * @return the json or <tt>null</tt> if the component is <b>not</b> built with JSon schema support
     */
    String getComponentParameterJsonSchema(String componentName) throws IOException;

    /**
     * Returns the JSON schema representation of the {@link DataFormat} parameters for the given data format name.
     *
     * @return the json or <tt>null</tt> if the data format does not exist
     */
    String getDataFormatParameterJsonSchema(String dataFormatName) throws IOException;

    /**
     * Returns the JSON schema representation of the {@link Language} parameters for the given language name.
     *
     * @return the json or <tt>null</tt> if the language does not exist
     */
    String getLanguageParameterJsonSchema(String languageName) throws IOException;

    /**
     * Returns the JSON schema representation of the EIP parameters for the given EIP name.
     *
     * @return the json or <tt>null</tt> if the EIP does not exist
     */
    String getEipParameterJsonSchema(String eipName) throws IOException;

    /**
     * Returns a JSON schema representation of the EIP parameters for the given EIP by its id.
     *
     * @param nameOrId the name of the EIP ({@link NamedNode#getShortName()} or a node id to refer to a specific node from the routes.
     * @param includeAllOptions whether to include non configured options also (eg default options)
     * @return the json or <tt>null</tt> if the eipName or the id was not found
     */
    String explainEipJson(String nameOrId, boolean includeAllOptions);

    /**
     * Returns a JSON schema representation of the component parameters (not endpoint parameters) for the given component by its id.
     *
     * @param componentName the name of the component.
     * @param includeAllOptions whether to include non configured options also (eg default options)
     * @return the json or <tt>null</tt> if the component was not found
     */
    String explainComponentJson(String componentName, boolean includeAllOptions);

    /**
     * Returns a JSON schema representation of the component parameters (not endpoint parameters) for the given component by its id.
     *
     * @param dataFormat the data format instance.
     * @param includeAllOptions whether to include non configured options also (eg default options)
     * @return the json
     */
    String explainDataFormatJson(String dataFormatName, DataFormat dataFormat, boolean includeAllOptions);

    /**
     * Returns a JSON schema representation of the endpoint parameters for the given endpoint uri.
     *
     * @param uri the endpoint uri
     * @param includeAllOptions whether to include non configured options also (eg default options)
     * @return the json or <tt>null</tt> if uri parameters is invalid, or the component is <b>not</b> built with JSon schema support
     */
    String explainEndpointJson(String uri, boolean includeAllOptions);

    /**
     * Creates a JSON representation of all the <b>static</b> and <b>dynamic</b> configured endpoints defined in the given route(s).
     *
     * @param routeId for a particular route, or <tt>null</tt> for all routes
     * @return a JSON string
     */
    String createRouteStaticEndpointJson(String routeId);

    /**
     * Creates a JSON representation of all the <b>static</b> (and possible <b>dynamic</b>) configured endpoints defined in the given route(s).
     *
     * @param routeId for a particular route, or <tt>null</tt> for all routes
     * @param includeDynamic whether to include dynamic endpoints
     * @return a JSON string
     */
    String createRouteStaticEndpointJson(String routeId, boolean includeDynamic);

    /**
     * Gets the {@link StreamCachingStrategy} to use.
     */
    StreamCachingStrategy getStreamCachingStrategy();

    /**
     * Gets the {@link UnitOfWorkFactory} to use.
     */
    UnitOfWorkFactory getUnitOfWorkFactory();

    /**
     * Gets the {@link org.apache.camel.spi.RuntimeEndpointRegistry} to use, or <tt>null</tt> if none is in use.
     */
    RuntimeEndpointRegistry getRuntimeEndpointRegistry();

    /**
     * Gets the {@link org.apache.camel.spi.RestRegistry} to use
     */
    RestRegistry getRestRegistry();

    /**
     * Adds the given route policy factory
     *
     * @param routePolicyFactory the factory
     */
    void addRoutePolicyFactory(RoutePolicyFactory routePolicyFactory);

    /**
     * Gets the route policy factories
     *
     * @return the list of current route policy factories
     */
    List<RoutePolicyFactory> getRoutePolicyFactories();

    /**
     * Returns the JAXB Context factory used to create Models.
     *
     * @return the JAXB Context factory used to create Models.
     */
    ModelJAXBContextFactory getModelJAXBContextFactory();

    /**
     * Returns the {@link ReloadStrategy} if in use.
     *
     * @return the strategy, or <tt>null</tt> if none has been configured.
     */
    ReloadStrategy getReloadStrategy();

    /**
     * Gets the associated {@link RuntimeCamelCatalog} for this CamelContext.
     */
    RuntimeCamelCatalog getRuntimeCamelCatalog();

    /**
     * Gets a list of {@link LogListener}.
     */
    Set<LogListener> getLogListeners();

    /**
     * Adds a {@link LogListener}.
     */
    void addLogListener(LogListener listener);

    /**
     * Gets the global SSL context parameters if configured.
     */
    SSLContextParameters getSSLContextParameters();

    /**
     * Gets the {@link HeadersMapFactory} to use.
     */
    HeadersMapFactory getHeadersMapFactory();

    /**
     * Returns an optional {@link HealthCheckRegistry}, by default no registry is
     * present and it must be explicit activated. Components can register/unregister
     * health checks in response to life-cycle events (i.e. start/stop).
     *
     * This registry is not used by the camel context but it is up to the impl to
     * properly use it, i.e.
     *
     * - a RouteController could use the registry to decide to restart a route
     *   with failing health checks
     * - spring boot could integrate such checks within its health endpoint or
     *   make it available only as separate endpoint.
     */
    HealthCheckRegistry getHealthCheckRegistry();


    //
    //
    // Deprecated methods
    //
    //

    @Deprecated
    default void addRoutes(RoutesBuilder builder) throws Exception {
        adapt(ModelCamelContext.class).addRoutes(builder);
    }

    /**
     * @deprecated use {@link #setGlobalOptions(Map) setGlobalOptions(Map<String,String>) instead}.
     */
    @Deprecated
    default void setProperties(Map<String, String> properties) {
        adapt(ConfigurableCamelContext.class).setGlobalOptions(properties);
    }

    /**
     * Sets global options that can be referenced in the camel context
     * <p/>
     * <b>Important:</b> This has nothing to do with property placeholders, and is just a plain set of key/value pairs
     * which are used to configure global options on CamelContext, such as a maximum debug logging length etc.
     * For property placeholders use {@link #resolvePropertyPlaceholders(String)} method and see more details
     * at the <a href="http://camel.apache.org/using-propertyplaceholder.html">property placeholder</a> documentation.
     *
     * @param globalOptions global options that can be referenced in the camel context
     * @deprecated use {@link ConfigurableCamelContext#setGlobalOptions(Map)}
     */
    @Deprecated
    default void setGlobalOptions(Map<String, String> globalOptions) {
        adapt(ConfigurableCamelContext.class).setGlobalOptions(globalOptions);
    }

    /**
     * @deprecated use {@link #getGlobalOptions()} instead.
     */
    @Deprecated
    default Map<String, String> getProperties() {
        return getGlobalOptions();
    }

    /**
     * @deprecated use {@link #getGlobalOption(String)} instead.
     */
    @Deprecated
    default String getProperty(String key) {
        return getGlobalOption(key);
    }

    /**
     * Sets a pluggable service pool to use for {@link PollingConsumer} pooling.
     *
     * @param servicePool the pool
     */
    @Deprecated
    default void setPollingConsumerServicePool(ServicePool<Endpoint, PollingConsumer> servicePool) {
        adapt(ConfigurableCamelContext.class).setPollingConsumerServicePool(servicePool);
    }

    /**
     * Uses a custom node id factory when generating auto assigned ids to the nodes in the route definitions
     *
     * @param factory custom factory to use
     */
    @Deprecated
    default void setNodeIdFactory(NodeIdFactory factory) {
        adapt(ConfigurableCamelContext.class).setNodeIdFactory(factory);
    }

    /**
     * Sets the management strategy to use
     *
     * @param strategy the management strategy
     */
    @Deprecated
    default void setManagementStrategy(ManagementStrategy strategy) {
        adapt(ConfigurableCamelContext.class).setManagementStrategy(strategy);
    }

    /**
     * Sets a custom tracer to be used as the default tracer.
     * <p/>
     * <b>Note:</b> This must be set before any routes are created,
     * changing the default tracer for existing routes is not supported.
     *
     * @param tracer the custom tracer to use as default tracer
     */
    @Deprecated
    default void setDefaultTracer(InterceptStrategy tracer) {
        adapt(ConfigurableCamelContext.class).setDefaultTracer(tracer);
    }

    /**
     * Sets a custom backlog tracer to be used as the default backlog tracer.
     * <p/>
     * <b>Note:</b> This must be set before any routes are created,
     * changing the default backlog tracer for existing routes is not supported.
     *
     * @param backlogTracer the custom tracer to use as default backlog tracer
     */
    @Deprecated
    default void setDefaultBacklogTracer(InterceptStrategy backlogTracer) {
        adapt(ConfigurableCamelContext.class).setDefaultBacklogTracer(backlogTracer);
    }

    /**
     * Sets a custom backlog debugger to be used as the default backlog debugger.
     * <p/>
     * <b>Note:</b> This must be set before any routes are created,
     * changing the default backlog debugger for existing routes is not supported.
     *
     * @param backlogDebugger the custom debugger to use as default backlog debugger
     */
    @Deprecated
    default void setDefaultBacklogDebugger(InterceptStrategy backlogDebugger) {
        adapt(ConfigurableCamelContext.class).setDefaultBacklogDebugger(backlogDebugger);
    }

    /**
     * Sets a custom inflight repository to use
     *
     * @param repository the repository
     */
    @Deprecated
    default void setInflightRepository(InflightRepository repository) {
        adapt(ConfigurableCamelContext.class).setInflightRepository(repository);
    }

    /**
     * Sets a custom  {@link org.apache.camel.AsyncProcessor} await manager.
     *
     * @param manager the manager
     */
    @Deprecated
    default void setAsyncProcessorAwaitManager(AsyncProcessorAwaitManager manager) {
        adapt(ConfigurableCamelContext.class).setAsyncProcessorAwaitManager(manager);
    }

    /**
     * Sets the application CamelContext class loader
     *
     * @param classLoader the class loader
     */
    @Deprecated
    default void setApplicationContextClassLoader(ClassLoader classLoader) {
        adapt(ConfigurableCamelContext.class).setApplicationContextClassLoader(classLoader);
    }

    /**
     * Sets a custom shutdown strategy
     *
     * @param shutdownStrategy the custom strategy
     */
    @Deprecated
    default void setShutdownStrategy(ShutdownStrategy shutdownStrategy) {
        adapt(ConfigurableCamelContext.class).setShutdownStrategy(shutdownStrategy);
    }

    /**
     * Sets a custom {@link org.apache.camel.spi.ExecutorServiceManager}
     *
     * @param executorServiceManager the custom manager
     */
    @Deprecated
    default void setExecutorServiceManager(ExecutorServiceManager executorServiceManager) {
        adapt(ConfigurableCamelContext.class).setExecutorServiceManager(executorServiceManager);
    }

    /**
     * Sets a custom {@link org.apache.camel.spi.MessageHistoryFactory}
     *
     * @param messageHistoryFactory the custom factory
     */
    @Deprecated
    default void setMessageHistoryFactory(MessageHistoryFactory messageHistoryFactory) {
        adapt(ConfigurableCamelContext.class).setMessageHistoryFactory(messageHistoryFactory);
    }

    /**
     * Sets a custom {@link org.apache.camel.spi.ProcessorFactory}
     *
     * @param processorFactory the custom factory
     */
    @Deprecated
    default void setProcessorFactory(ProcessorFactory processorFactory) {
        adapt(ConfigurableCamelContext.class).setProcessorFactory(processorFactory);
    }

    /**
     * Sets a custom {@link Debugger}
     *
     * @param debugger the debugger
     */
    @Deprecated
    default void setDebugger(Debugger debugger) {
        adapt(ConfigurableCamelContext.class).setDebugger(debugger);
    }

    /**
     * Sets a custom {@link UuidGenerator} (should only be set once)
     *
     * @param uuidGenerator the UUID Generator
     */
    @Deprecated
    default void setUuidGenerator(UuidGenerator uuidGenerator) {
        adapt(ConfigurableCamelContext.class).setUuidGenerator(uuidGenerator);
    }

    /**
     * Set whether breadcrumb is enabled.
     *
     * @param useBreadcrumb <tt>true</tt> to enable breadcrumb, <tt>false</tt> to disable
     */
    @Deprecated
    default void setUseBreadcrumb(Boolean useBreadcrumb) {
        adapt(ConfigurableCamelContext.class).setUseBreadcrumb(useBreadcrumb);
    }

    /**
     * Sets a custom {@link StreamCachingStrategy} to use.
     */
    @Deprecated
    default void setStreamCachingStrategy(StreamCachingStrategy streamCachingStrategy) {
        adapt(ConfigurableCamelContext.class).setStreamCachingStrategy(streamCachingStrategy);
    }

    /**
     * Sets a custom {@link UnitOfWorkFactory} to use.
     */
    @Deprecated
    default void setUnitOfWorkFactory(UnitOfWorkFactory unitOfWorkFactory) {
        adapt(ConfigurableCamelContext.class).setUnitOfWorkFactory(unitOfWorkFactory);
    }

    /**
     * Set whether <a href="http://www.slf4j.org/api/org/slf4j/MDC.html">MDC</a> is enabled.
     *
     * @param useMDCLogging <tt>true</tt> to enable MDC logging, <tt>false</tt> to disable
     */
    @Deprecated
    default void setUseMDCLogging(Boolean useMDCLogging) {
        adapt(ConfigurableCamelContext.class).setUseMDCLogging(useMDCLogging);
    }

    /**
     * Whether to enable using data type on Camel messages.
     * <p/>
     * Data type are automatic turned on if one ore more routes has been explicit configured with input and output types.
     * Otherwise data type is default off.
     *
     * @param  useDataType <tt>true</tt> to enable data type on Camel messages.
     */
    @Deprecated
    default void setUseDataType(Boolean useDataType) {
        adapt(ConfigurableCamelContext.class).setUseDataType(useDataType);
    }

    /**
     * Sets a custom {@link HeadersMapFactory} to be used.
     */
    @Deprecated
    default void setHeadersMapFactory(HeadersMapFactory factory) {
        adapt(ConfigurableCamelContext.class).setHeadersMapFactory(factory);
    }

    /**
     * Sets a {@link HealthCheckRegistry}.
     */
    @Deprecated
    default void setHealthCheckRegistry(HealthCheckRegistry healthCheckRegistry) {
        adapt(ConfigurableCamelContext.class).setHealthCheckRegistry(healthCheckRegistry);
    }

    /**
     * Sets the global SSL context parameters.
     */
    @Deprecated
    default void setSSLContextParameters(SSLContextParameters sslContextParameters) {
        adapt(ConfigurableCamelContext.class).setSSLContextParameters(sslContextParameters);
    }

    /**
     * Sets a custom {@link ReloadStrategy} to be used
     */
    @Deprecated
    default void setReloadStrategy(ReloadStrategy reloadStrategy) {
        adapt(ConfigurableCamelContext.class).setReloadStrategy(reloadStrategy);
    }

    /**
     * Sets a custom JAXB Context factory to be used
     *
     * @param modelJAXBContextFactory a JAXB Context factory
     */
    @Deprecated
    default void setModelJAXBContextFactory(ModelJAXBContextFactory modelJAXBContextFactory) {
        adapt(ConfigurableCamelContext.class).setModelJAXBContextFactory(modelJAXBContextFactory);
    }

    /**
     * Sets a custom {@link org.apache.camel.spi.RuntimeEndpointRegistry} to use.
     */
    @Deprecated
    default void setRuntimeEndpointRegistry(RuntimeEndpointRegistry runtimeEndpointRegistry) {
        adapt(ConfigurableCamelContext.class).setRuntimeEndpointRegistry(runtimeEndpointRegistry);
    }

    /**
     * Sets a custom {@link org.apache.camel.spi.RestRegistry} to use.
     */
    @Deprecated
    default void setRestRegistry(RestRegistry restRegistry) {
        adapt(ConfigurableCamelContext.class).setRestRegistry(restRegistry);
    }

    /**
     * Sets the factory finder resolver to use.
     *
     * @param resolver the factory finder resolver
     */
    @Deprecated
    default void setFactoryFinderResolver(FactoryFinderResolver resolver) {
        adapt(ConfigurableCamelContext.class).setFactoryFinderResolver(resolver);
    }

    /**
     * Sets the class resolver to be use
     *
     * @param resolver the resolver
     */
    @Deprecated
    default void setClassResolver(ClassResolver resolver) {
        adapt(ConfigurableCamelContext.class).setClassResolver(resolver);
    }

    /**
     * Sets the package scanning class resolver to use
     *
     * @param resolver the resolver
     */
    @Deprecated
    default void setPackageScanClassResolver(PackageScanClassResolver resolver) {
        adapt(ConfigurableCamelContext.class).setPackageScanClassResolver(resolver);
    }

    /**
     * Sets a pluggable service pool to use for {@link Producer} pooling.
     *
     * @param servicePool the pool
     */
    @Deprecated
    default void setProducerServicePool(ServicePool<Endpoint, Producer> servicePool) {
        adapt(ConfigurableCamelContext.class).setProducerServicePool(servicePool);
    }

    /**
     * Disables using JMX as {@link org.apache.camel.spi.ManagementStrategy}.
     * <p/>
     * <b>Important:</b> This method must be called <b>before</b> the {@link CamelContext} is started.
     *
     * @throws IllegalStateException is thrown if the {@link CamelContext} is not in stopped state.
     */
    @Deprecated
    default void disableJMX() throws IllegalStateException {
        adapt(ConfigurableCamelContext.class).disableJMX();
    }

    /**
     * Shutdown and <b>removes</b> the given route using {@link org.apache.camel.spi.ShutdownStrategy}.
     *
     * @param routeId the route id
     * @throws Exception is thrown if the route could not be shutdown for whatever reason
     * @deprecated use {@link #stopRoute(String)} and {@link #removeRoute(String)}
     */
    @Deprecated
    default void shutdownRoute(String routeId) throws Exception {
        stopRoute(routeId);
        removeRoute(routeId);
    }

    /**
     * Shutdown and <b>removes</b> the given route using {@link org.apache.camel.spi.ShutdownStrategy} with a specified timeout.
     *
     * @param routeId  the route id
     * @param timeout  timeout
     * @param timeUnit the unit to use
     * @throws Exception is thrown if the route could not be shutdown for whatever reason
     * @deprecated use {@link #stopRoute(String, long, java.util.concurrent.TimeUnit)} and {@link #removeRoute(String)}
     */
    @Deprecated
    default void shutdownRoute(String routeId, long timeout, TimeUnit timeUnit) throws Exception {
        stopRoute(routeId, timeout, timeUnit);
        removeRoute(routeId);
    }

    /**
     * Sets a custom name strategy
     *
     * @param nameStrategy name strategy
     */
    @Deprecated
    default void setNameStrategy(CamelContextNameStrategy nameStrategy) {
        adapt(ConfigurableCamelContext.class).setNameStrategy(nameStrategy);
    }

    /**
     * Sets a custom management name strategy
     *
     * @param nameStrategy name strategy
     */
    @Deprecated
    default void setManagementNameStrategy(ManagementNameStrategy nameStrategy) {
        adapt(ConfigurableCamelContext.class).setManagementNameStrategy(nameStrategy);
    }

    /**
     * NOTE: experimental api
     *
     * @param routeController the route controller
     */
    @Deprecated
    default void setRouteController(RouteController routeController) {
        adapt(ConfigurableCamelContext.class).setRouteController(routeController);
    }

    /**
     * Sets the default error handler builder which is inherited by the routes
     *
     * @param errorHandlerBuilder the builder
     */
    @Deprecated
    default void setErrorHandlerBuilder(ErrorHandlerFactory errorHandlerBuilder) {
        adapt(ConfigurableCamelContext.class).setErrorHandlerBuilder(errorHandlerBuilder);
    }

    /**
     * Starts all the routes which currently is not started.
     *
     * @throws Exception is thrown if a route could not be started for whatever reason
     */
    @Deprecated
    default void startAllRoutes() throws Exception {
        getRouteController().startAllRoutes();
    }

    /**
     * Starts the given route if it has been previously stopped
     *
     * @param routeId the route id
     * @throws Exception is thrown if the route could not be started for whatever reason
     */
    @Deprecated
    default void startRoute(String routeId) throws Exception {
        getRouteController().startRoute(routeId);
    }

    /**
     * Stops the given route using {@link org.apache.camel.spi.ShutdownStrategy}.
     *
     * @param routeId the route id
     * @throws Exception is thrown if the route could not be stopped for whatever reason
     * @see #suspendRoute(String)
     */
    @Deprecated
    default void stopRoute(String routeId) throws Exception {
        getRouteController().stopRoute(routeId);
    }

    /**
     * Stops the given route using {@link org.apache.camel.spi.ShutdownStrategy} with a specified timeout.
     *
     * @param routeId the route id
     * @param timeout  timeout
     * @param timeUnit the unit to use
     * @throws Exception is thrown if the route could not be stopped for whatever reason
     * @see #suspendRoute(String, long, java.util.concurrent.TimeUnit)
     */
    @Deprecated
    default void stopRoute(String routeId, long timeout, TimeUnit timeUnit) throws Exception {
        getRouteController().stopRoute(routeId, timeout, timeUnit);
    }

    /**
     * Stops the given route using {@link org.apache.camel.spi.ShutdownStrategy} with a specified timeout
     * and optional abortAfterTimeout mode.
     *
     * @param routeId the route id
     * @param timeout  timeout
     * @param timeUnit the unit to use
     * @param abortAfterTimeout should abort shutdown after timeout
     * @return <tt>true</tt> if the route is stopped before the timeout
     * @throws Exception is thrown if the route could not be stopped for whatever reason
     * @see #suspendRoute(String, long, java.util.concurrent.TimeUnit)
     */
    @Deprecated
    default boolean stopRoute(String routeId, long timeout, TimeUnit timeUnit, boolean abortAfterTimeout) throws Exception {
        return getRouteController().stopRoute(routeId, timeout, timeUnit, abortAfterTimeout);
    }

    /**
     * Resumes the given route if it has been previously suspended
     * <p/>
     * If the route does <b>not</b> support suspension the route will be started instead
     *
     * @param routeId the route id
     * @throws Exception is thrown if the route could not be resumed for whatever reason
     */
    @Deprecated
    default void resumeRoute(String routeId) throws Exception {
        getRouteController().resumeRoute(routeId);
    }

    /**
     * Suspends the given route using {@link org.apache.camel.spi.ShutdownStrategy}.
     * <p/>
     * Suspending a route is more gently than stopping, as the route consumers will be suspended (if they support)
     * otherwise the consumers will be stopped.
     * <p/>
     * By suspending the route services will be kept running (if possible) and therefore its faster to resume the route.
     * <p/>
     * If the route does <b>not</b> support suspension the route will be stopped instead
     *
     * @param routeId the route id
     * @throws Exception is thrown if the route could not be suspended for whatever reason
     */
    @Deprecated
    default void suspendRoute(String routeId) throws Exception {
        getRouteController().suspendRoute(routeId);
    }

    /**
     * Suspends the given route using {@link org.apache.camel.spi.ShutdownStrategy} with a specified timeout.
     * <p/>
     * Suspending a route is more gently than stopping, as the route consumers will be suspended (if they support)
     * otherwise the consumers will be stopped.
     * <p/>
     * By suspending the route services will be kept running (if possible) and therefore its faster to resume the route.
     * <p/>
     * If the route does <b>not</b> support suspension the route will be stopped instead
     *
     * @param routeId  the route id
     * @param timeout  timeout
     * @param timeUnit the unit to use
     * @throws Exception is thrown if the route could not be suspended for whatever reason
     */
    @Deprecated
    default void suspendRoute(String routeId, long timeout, TimeUnit timeUnit) throws Exception {
        getRouteController().suspendRoute(routeId, timeout, timeUnit);
    }

    /**
     * Returns the current status of the given route
     *
     * @param routeId the route id
     * @return the status for the route
     */
    @Deprecated
    default ServiceStatus getRouteStatus(String routeId) {
        return getRouteController().getRouteStatus(routeId);
    }

    /**
     * Indicates whether current thread is starting route(s).
     * <p/>
     * This can be useful to know by {@link LifecycleStrategy} or the likes, in case
     * they need to react differently.
     *
     * @return <tt>true</tt> if current thread is starting route(s), or <tt>false</tt> if not.
     */
    @Deprecated
    default boolean isStartingRoutes() {
        return getRouteController().isStartingRoutes();
    }

    /**
     * Returns the HTML documentation for the given Camel component
     *
     * @return the HTML or <tt>null</tt> if the component is <b>not</b> built with HTML document included.
     * @deprecated use camel-catalog instead
     */
    @Deprecated
    String getComponentDocumentation(String componentName) throws IOException;

    /**
     * Gets the default error handler builder which is inherited by the routes
     *
     * @return the builder
     * @deprecated The return type will be switched to {@link ErrorHandlerFactory} in Camel 3.0
     */
    @Deprecated
    ErrorHandlerBuilder getErrorHandlerBuilder();

    /**
     * Gets the current {@link org.apache.camel.spi.ExecutorServiceStrategy}
     *
     * @return the manager
     * @deprecated use {@link #getExecutorServiceManager()}
     */
    @Deprecated
    org.apache.camel.spi.ExecutorServiceStrategy getExecutorServiceStrategy();

    /**
     * Whether or not type converters should be loaded lazy
     *
     * @return <tt>true</tt> to load lazy, <tt>false</tt> to load on startup
     * @deprecated this option is no longer supported, will be removed in a future Camel release.
     */
    @Deprecated
    Boolean isLazyLoadTypeConverters();

    /**
     * Sets whether type converters should be loaded lazy
     *
     * @param lazyLoadTypeConverters <tt>true</tt> to load lazy, <tt>false</tt> to load on startup
     * @deprecated this option is no longer supported, will be removed in a future Camel release.
     */
    @Deprecated
    void setLazyLoadTypeConverters(Boolean lazyLoadTypeConverters);

    /**
     * Adds the given interceptor strategy
     *
     * @param interceptStrategy the strategy
     */
    @Deprecated
    default void addInterceptStrategy(InterceptStrategy interceptStrategy) {
        adapt(ConfigurableCamelContext.class).addInterceptStrategy(interceptStrategy);
    }

    /**
     * Sets a custom data format resolver
     *
     * @param dataFormatResolver the resolver
     */
    @Deprecated
    default void setDataFormatResolver(DataFormatResolver dataFormatResolver) {
        adapt(ConfigurableCamelContext.class).setDataFormatResolver(dataFormatResolver);
    }

    /**
     * Gets the service pool for {@link Producer} pooling.
     *
     * @return the service pool
     */
    @Deprecated
    ServicePool<Endpoint, Producer> getProducerServicePool();

    /**
     * Gets the service pool for {@link Producer} pooling.
     *
     * @return the service pool
     */
    @Deprecated
    ServicePool<Endpoint, PollingConsumer> getPollingConsumerServicePool();

    /**
     * Sets whether or not type converter statistics is enabled.
     * <p/>
     * By default the type converter utilization statistics is disabled.
     * <b>Notice:</b> If enabled then there is a slight performance impact under very heavy load.
     * <p/>
     * You can enable/disable the statistics at runtime using the
     * {@link org.apache.camel.spi.TypeConverterRegistry#getStatistics()#setTypeConverterStatisticsEnabled(Boolean)} method,
     * or from JMX on the {@link org.apache.camel.api.management.mbean.ManagedTypeConverterRegistryMBean} mbean.
     *
     * @param typeConverterStatisticsEnabled <tt>true</tt> to enable, <tt>false</tt> to disable
     */
    @Deprecated
    default void setTypeConverterStatisticsEnabled(Boolean typeConverterStatisticsEnabled) {
        adapt(ConfigurableCamelContext.class).setTypeConverterStatisticsEnabled(typeConverterStatisticsEnabled);
    }

    /**
     * Returns a list of the current route definitions
     *
     * @return list of the current route definitions
     */
    @Deprecated
    default List<RouteDefinition> getRouteDefinitions() {
        return adapt(ModelCamelContext.class).getRouteDefinitions();
    }

    /**
     * Gets the route definition with the given id
     *
     * @param id id of the route
     * @return the route definition or <tt>null</tt> if not found
     */
    @Deprecated
    default RouteDefinition getRouteDefinition(String id) {
        return adapt(ModelCamelContext.class).getRouteDefinition(id);
    }

    /**
     * Returns a list of the current REST definitions
     *
     * @return list of the current REST definitions
     */
    @Deprecated
    default List<RestDefinition> getRestDefinitions() {
        return adapt(ModelCamelContext.class).getRestDefinitions();
    }

    /**
     * Gets the managed processor client api from any of the routes which with the given id
     *
     * @param id id of the processor
     * @param type the managed processor type from the {@link org.apache.camel.api.management.mbean} package.
     * @return the processor or <tt>null</tt> if not found
     * @throws IllegalArgumentException if the type is not compliant
     */
    @Deprecated
    default <T extends ManagedProcessorMBean> T getManagedProcessor(String id, Class<T> type) {
        return adapt(ManagedCamelContext.class).getManagedProcessor(id, type);
    }

    /**
     * Gets the managed route client api with the given route id
     *
     * @param routeId id of the route
     * @param type the managed route type from the {@link org.apache.camel.api.management.mbean} package.
     * @return the route or <tt>null</tt> if not found
     * @throws IllegalArgumentException if the type is not compliant
     */
    @Deprecated
    default <T extends ManagedRouteMBean> T getManagedRoute(String routeId, Class<T> type) {
        return adapt(ManagedCamelContext.class).getManagedRoute(routeId, type);
    }

    /**
     * Gets the managed Camel CamelContext client api
     */
    @Deprecated
    default ManagedCamelContextMBean getManagedCamelContext() {
        return adapt(ManagedCamelContext.class).getManagedCamelContext();
    }

    /**
     * Adds a collection of route definitions to the context
     * <p/>
     * <b>Important: </b> Each route in the same {@link org.apache.camel.CamelContext} must have an <b>unique</b> route id.
     * If you use the API from {@link org.apache.camel.CamelContext} or {@link org.apache.camel.model.ModelCamelContext} to add routes, then any
     * new routes which has a route id that matches an old route, then the old route is replaced by the new route.
     *
     * @param routeDefinitions the route(s) definition to add
     * @throws Exception if the route definitions could not be created for whatever reason
     */
    @Deprecated
    default void addRouteDefinitions(Collection<RouteDefinition> routeDefinitions) throws Exception {
        adapt(ModelCamelContext.class).addRouteDefinitions(routeDefinitions);
    }

    /**
     * Loads a collection of route definitions from the given {@link java.io.InputStream}.
     *
     * @param is input stream with the route(s) definition to add
     * @return the route definitions
     * @throws Exception if the route definitions could not be loaded for whatever reason
     */
    @Deprecated
    default RoutesDefinition loadRoutesDefinition(InputStream is) throws Exception {
        return adapt(ModelCamelContext.class).loadRoutesDefinition(is);
    }

    /**
     * Loads a collection of rest definitions from the given {@link java.io.InputStream}.
     *
     * @param is input stream with the rest(s) definition to add
     * @return the rest definitions
     * @throws Exception if the rest definitions could not be loaded for whatever reason
     */
    @Deprecated
    default RestsDefinition loadRestsDefinition(InputStream is) throws Exception {
        return adapt(ModelCamelContext.class).loadRestsDefinition(is);
    }

    /**
     * Add a route definition to the context
     * <p/>
     * <b>Important: </b> Each route in the same {@link org.apache.camel.CamelContext} must have an <b>unique</b> route id.
     * If you use the API from {@link org.apache.camel.CamelContext} or {@link org.apache.camel.model.ModelCamelContext} to add routes, then any
     * new routes which has a route id that matches an old route, then the old route is replaced by the new route.
     *
     * @param routeDefinition the route definition to add
     * @throws Exception if the route definition could not be created for whatever reason
     */
    @Deprecated
    default void addRouteDefinition(RouteDefinition routeDefinition) throws Exception {
        adapt(ModelCamelContext.class).addRouteDefinition(routeDefinition);
    }
}
