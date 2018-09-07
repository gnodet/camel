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

import java.util.Map;

import org.apache.camel.health.HealthCheckRegistry;
import org.apache.camel.spi.AsyncProcessorAwaitManager;
import org.apache.camel.spi.CamelContextNameStrategy;
import org.apache.camel.spi.ClassResolver;
import org.apache.camel.spi.DataFormatResolver;
import org.apache.camel.spi.ExecutorServiceManager;
import org.apache.camel.spi.FactoryFinderResolver;
import org.apache.camel.spi.HeadersMapFactory;
import org.apache.camel.spi.InflightRepository;
import org.apache.camel.spi.InterceptStrategy;
import org.apache.camel.spi.LifecycleStrategy;
import org.apache.camel.spi.LogListener;
import org.apache.camel.spi.ManagementNameStrategy;
import org.apache.camel.spi.ManagementStrategy;
import org.apache.camel.spi.MessageHistoryFactory;
import org.apache.camel.spi.ModelJAXBContextFactory;
import org.apache.camel.spi.NodeIdFactory;
import org.apache.camel.spi.PackageScanClassResolver;
import org.apache.camel.spi.ProcessorFactory;
import org.apache.camel.spi.ReloadStrategy;
import org.apache.camel.spi.RestRegistry;
import org.apache.camel.spi.RouteController;
import org.apache.camel.spi.RuntimeEndpointRegistry;
import org.apache.camel.spi.ServicePool;
import org.apache.camel.spi.ShutdownStrategy;
import org.apache.camel.spi.StreamCachingStrategy;
import org.apache.camel.spi.UnitOfWorkFactory;
import org.apache.camel.spi.UuidGenerator;
import org.apache.camel.util.jsse.SSLContextParameters;

public interface ConfigurableCamelContext extends CamelContext {

    /**
     * Sets global options that can be referenced in the camel context
     * <p/>
     * <b>Important:</b> This has nothing to do with property placeholders, and is just a plain set of key/value pairs
     * which are used to configure global options on CamelContext, such as a maximum debug logging length etc.
     * For property placeholders use {@link #resolvePropertyPlaceholders(String)} method and see more details
     * at the <a href="http://camel.apache.org/using-propertyplaceholder.html">property placeholder</a> documentation.
     *
     * @param globalOptions global options that can be referenced in the camel context
     */
    void setGlobalOptions(Map<String, String> globalOptions);

    /**
     * Sets a pluggable service pool to use for {@link PollingConsumer} pooling.
     *
     * @param servicePool the pool
     */
    void setPollingConsumerServicePool(ServicePool<Endpoint, PollingConsumer> servicePool);

    /**
     * Uses a custom node id factory when generating auto assigned ids to the nodes in the route definitions
     *
     * @param factory custom factory to use
     */
    void setNodeIdFactory(NodeIdFactory factory);

    /**
     * Sets the management strategy to use
     *
     * @param strategy the management strategy
     */
    void setManagementStrategy(ManagementStrategy strategy);

    /**
     * Sets a custom backlog tracer to be used as the default backlog tracer.
     * <p/>
     * <b>Note:</b> This must be set before any routes are created,
     * changing the default backlog tracer for existing routes is not supported.
     *
     * @param backlogTracer the custom tracer to use as default backlog tracer
     */
    void setDefaultBacklogTracer(InterceptStrategy backlogTracer);

    /**
     * Sets a custom backlog debugger to be used as the default backlog debugger.
     * <p/>
     * <b>Note:</b> This must be set before any routes are created,
     * changing the default backlog debugger for existing routes is not supported.
     *
     * @param backlogDebugger the custom debugger to use as default backlog debugger
     */
    void setDefaultBacklogDebugger(InterceptStrategy backlogDebugger);

    /**
     * Sets a custom inflight repository to use
     *
     * @param repository the repository
     */
    void setInflightRepository(InflightRepository repository);

    /**
     * Sets a custom  {@link org.apache.camel.AsyncProcessor} await manager.
     *
     * @param manager the manager
     */
    void setAsyncProcessorAwaitManager(AsyncProcessorAwaitManager manager);

    /**
     * Sets the application CamelContext class loader
     *
     * @param classLoader the class loader
     */
    void setApplicationContextClassLoader(ClassLoader classLoader);

    /**
     * Sets a custom shutdown strategy
     *
     * @param shutdownStrategy the custom strategy
     */
    void setShutdownStrategy(ShutdownStrategy shutdownStrategy);

    /**
     * Sets a custom {@link org.apache.camel.spi.ExecutorServiceManager}
     *
     * @param executorServiceManager the custom manager
     */
    void setExecutorServiceManager(ExecutorServiceManager executorServiceManager);

    /**
     * Sets a custom {@link org.apache.camel.spi.MessageHistoryFactory}
     *
     * @param messageHistoryFactory the custom factory
     */
    void setMessageHistoryFactory(MessageHistoryFactory messageHistoryFactory);

    /**
     * Sets a custom {@link org.apache.camel.spi.ProcessorFactory}
     *
     * @param processorFactory the custom factory
     */
    void setProcessorFactory(ProcessorFactory processorFactory);

    /**
     * Sets a custom {@link UuidGenerator} (should only be set once)
     *
     * @param uuidGenerator the UUID Generator
     */
    void setUuidGenerator(UuidGenerator uuidGenerator);

    /**
     * Set whether breadcrumb is enabled.
     *
     * @param useBreadcrumb <tt>true</tt> to enable breadcrumb, <tt>false</tt> to disable
     */
    void setUseBreadcrumb(Boolean useBreadcrumb);

    /**
     * Sets a custom {@link StreamCachingStrategy} to use.
     */
    void setStreamCachingStrategy(StreamCachingStrategy streamCachingStrategy);

    /**
     * Sets a custom {@link UnitOfWorkFactory} to use.
     */
    void setUnitOfWorkFactory(UnitOfWorkFactory unitOfWorkFactory);

    /**
     * Set whether <a href="http://www.slf4j.org/api/org/slf4j/MDC.html">MDC</a> is enabled.
     *
     * @param useMDCLogging <tt>true</tt> to enable MDC logging, <tt>false</tt> to disable
     */
    void setUseMDCLogging(Boolean useMDCLogging);

    /**
     * Whether to enable using data type on Camel messages.
     * <p/>
     * Data type are automatic turned on if one ore more routes has been explicit configured with input and output types.
     * Otherwise data type is default off.
     *
     * @param  useDataType <tt>true</tt> to enable data type on Camel messages.
     */
    void setUseDataType(Boolean useDataType);

    /**
     * Sets a custom {@link HeadersMapFactory} to be used.
     */
    void setHeadersMapFactory(HeadersMapFactory factory);

    /**
     * Sets a {@link HealthCheckRegistry}.
     */
    void setHealthCheckRegistry(HealthCheckRegistry healthCheckRegistry);

    /**
     * Sets the global SSL context parameters.
     */
    void setSSLContextParameters(SSLContextParameters sslContextParameters);

    /**
     * Sets a custom {@link ReloadStrategy} to be used
     */
    void setReloadStrategy(ReloadStrategy reloadStrategy);

    /**
     * Sets a custom JAXB Context factory to be used
     *
     * @param modelJAXBContextFactory a JAXB Context factory
     */
    void setModelJAXBContextFactory(ModelJAXBContextFactory modelJAXBContextFactory);

    /**
     * Sets a custom {@link org.apache.camel.spi.RuntimeEndpointRegistry} to use.
     */
    void setRuntimeEndpointRegistry(RuntimeEndpointRegistry runtimeEndpointRegistry);

    /**
     * Sets a custom {@link org.apache.camel.spi.RestRegistry} to use.
     */
    void setRestRegistry(RestRegistry restRegistry);

    /**
     * Sets the factory finder resolver to use.
     *
     * @param resolver the factory finder resolver
     */
    void setFactoryFinderResolver(FactoryFinderResolver resolver);

    /**
     * Sets the class resolver to be use
     *
     * @param resolver the resolver
     */
    void setClassResolver(ClassResolver resolver);

    /**
     * Sets the package scanning class resolver to use
     *
     * @param resolver the resolver
     */
    void setPackageScanClassResolver(PackageScanClassResolver resolver);

    /**
     * Sets a pluggable service pool to use for {@link Producer} pooling.
     *
     * @param servicePool the pool
     */
    void setProducerServicePool(ServicePool<Endpoint, Producer> servicePool);

    /**
     * Disables using JMX as {@link org.apache.camel.spi.ManagementStrategy}.
     * <p/>
     * <b>Important:</b> This method must be called <b>before</b> the {@link CamelContext} is started.
     *
     * @throws IllegalStateException is thrown if the {@link CamelContext} is not in stopped state.
     */
    void disableJMX() throws IllegalStateException;

    /**
     * Sets a custom name strategy
     *
     * @param nameStrategy name strategy
     */
    void setNameStrategy(CamelContextNameStrategy nameStrategy);

    /**
     * Sets a custom management name strategy
     *
     * @param nameStrategy name strategy
     */
    void setManagementNameStrategy(ManagementNameStrategy nameStrategy);

    /**
     * NOTE: experimental api
     *
     * @param routeController the route controller
     */
    void setRouteController(RouteController routeController);

    /**
     * Sets the default error handler builder which is inherited by the routes
     *
     * @param errorHandlerBuilder the builder
     */
    void setErrorHandlerBuilder(ErrorHandlerFactory errorHandlerBuilder);

    /**
     * Adds the given interceptor strategy
     *
     * @param interceptStrategy the strategy
     */
    void addInterceptStrategy(InterceptStrategy interceptStrategy);

    /**
     * Sets a custom data format resolver
     *
     * @param dataFormatResolver the resolver
     */
    void setDataFormatResolver(DataFormatResolver dataFormatResolver);

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
    void setTypeConverterStatisticsEnabled(Boolean typeConverterStatisticsEnabled);

    /**
     * Adds a {@link LogListener}.
     */
    void addLogListener(LogListener listener);

    /**
     * Adds the given lifecycle strategy to be used.
     *
     * @param lifecycleStrategy the strategy
     */
    void addLifecycleStrategy(LifecycleStrategy lifecycleStrategy);

}
