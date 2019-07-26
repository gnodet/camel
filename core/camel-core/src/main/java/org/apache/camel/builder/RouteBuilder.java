/*
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
package org.apache.camel.builder;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.Route;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.InterceptDefinition;
import org.apache.camel.model.InterceptFromDefinition;
import org.apache.camel.model.InterceptSendToEndpointDefinition;
import org.apache.camel.model.Model;
import org.apache.camel.model.OnCompletionDefinition;
import org.apache.camel.model.OnExceptionDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.ProcessorDefinitionHelper;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.model.ToDefinition;
import org.apache.camel.model.ToDynamicDefinition;
import org.apache.camel.model.rest.RestBindingDefinition;
import org.apache.camel.model.rest.RestConfigurationDefinition;
import org.apache.camel.model.rest.RestConstants;
import org.apache.camel.model.rest.RestDefinition;
import org.apache.camel.model.rest.RestOperationParamDefinition;
import org.apache.camel.model.rest.RestParamType;
import org.apache.camel.model.rest.RestsDefinition;
import org.apache.camel.model.rest.VerbDefinition;
import org.apache.camel.reifier.rest.RestConfigurationReifier;
import org.apache.camel.spi.PropertiesComponent;
import org.apache.camel.spi.RestConfiguration;
import org.apache.camel.support.CamelContextHelper;
import org.apache.camel.util.FileUtil;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.StringHelper;
import org.apache.camel.util.URISupport;
import org.apache.camel.util.function.ThrowingConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A <a href="http://camel.apache.org/dsl.html">Java DSL</a> which is
 * used to build {@link Route} instances in a {@link CamelContext} for smart routing.
 */
public abstract class RouteBuilder extends BuilderSupport implements RoutesBuilder {
    protected Logger log = LoggerFactory.getLogger(getClass());
    private AtomicBoolean initialized = new AtomicBoolean(false);
    private RestsDefinition restCollection = new RestsDefinition();
    private Map<String, RestConfigurationDefinition> restConfigurations;
    private List<TransformerBuilder> transformerBuilders = new ArrayList<>();
    private List<ValidatorBuilder> validatorBuilders = new ArrayList<>();
    private RoutesDefinition routeCollection = new RoutesDefinition();

    public RouteBuilder() {
        this(null);
    }

    public RouteBuilder(CamelContext context) {
        super(context);
    }

    /**
     * Add routes to a context using a lambda expression.
     * It can be used as following:
     * <pre>
     * RouteBuilder.addRoutes(context, rb ->
     *     rb.from("direct:inbound").bean(ProduceTemplateBean.class)));
     * </pre>
     *
     * @param context the camel context to add routes
     * @param rbc a lambda expression receiving the {@code RouteBuilder} to use to create routes
     * @throws Exception if an error occurs
     */
    public static void addRoutes(CamelContext context, ThrowingConsumer<RouteBuilder, Exception> rbc) throws Exception {
        context.addRoutes(new RouteBuilder(context) {
            @Override
            public void configure() throws Exception {
                rbc.accept(this);
            }
        });
    }

    @Override
    public String toString() {
        return getRouteCollection().toString();
    }

    /**
     * <b>Called on initialization to build the routes using the fluent builder syntax.</b>
     * <p/>
     * This is a central method for RouteBuilder implementations to implement
     * the routes using the Java fluent builder syntax.
     *
     * @throws Exception can be thrown during configuration
     */
    public abstract void configure() throws Exception;

    /**
     * Binds the bean to the repository (if possible).
     *
     * @param id   the id of the bean
     * @param bean the bean
     */
    public void bindToRegistry(String id, Object bean) {
        getContext().getRegistry().bind(id, bean);
    }

    /**
     * Binds the bean to the repository (if possible).
     *
     * @param id   the id of the bean
     * @param type the type of the bean to associate the binding
     * @param bean the bean
     */
    public void bindToRegistry(String id, Class<?> type, Object bean) {
        getContext().getRegistry().bind(id, type, bean);
    }

    /**
     * Configures the REST services
     *
     * @return the builder
     */
    public RestConfigurationDefinition restConfiguration() {
        return restConfiguration("");
    }

    /**
     * Configures the REST service for the given component
     *
     * @return the builder
     */
    public RestConfigurationDefinition restConfiguration(String component) {
        if (restConfigurations == null) {
            restConfigurations = new HashMap<>();
        }
        RestConfigurationDefinition restConfiguration = restConfigurations.get(component);
        if (restConfiguration == null) {
            restConfiguration = new RestConfigurationDefinition();
            if (!component.isEmpty()) {
                restConfiguration.component(component);
            }
            restConfigurations.put(component, restConfiguration);
        }
        return restConfiguration;
    }
    /**
     * Creates a new REST service
     *
     * @return the builder
     */
    public RestDefinition rest() {
        getRestCollection().setCamelContext(getContext());
        RestDefinition answer = new RestDefinition();
        getRestCollection().addRest(answer);
        configureRest(answer);
        return answer;
    }

    /**
     * Creates a new REST service
     *
     * @param path  the base path
     * @return the builder
     */
    public RestDefinition rest(String path) {
        getRestCollection().setCamelContext(getContext());
        RestDefinition answer = new RestDefinition().path(path);
        getRestCollection().addRest(answer);
        configureRest(answer);
        return answer;
    }

    /**
     * Create a new {@code TransformerBuilder}.
     * 
     * @return the builder
     */
    public TransformerBuilder transformer() {
        TransformerBuilder tdb = new TransformerBuilder();
        transformerBuilders.add(tdb);
        return tdb;
    }

    /**
     * Create a new {@code ValidatorBuilder}.
     * 
     * @return the builder
     */
    public ValidatorBuilder validator() {
        ValidatorBuilder vb = new ValidatorBuilder();
        validatorBuilders.add(vb);
        return vb;
    }

    /**
     * Creates a new route from the given URI input
     *
     * @param uri  the from uri
     * @return the builder
     */
    public RouteDefinition<?> from(String uri) {
        getRouteCollection().setCamelContext(getContext());
        RouteDefinition<?> answer = new RouteDefinition<>().input(new FromDefinition().uri(uri));
        getRouteCollection().addRoute(answer);
        configureRoute(answer);
        return answer;
    }

    /**
     * Creates a new route from the given URI input
     *
     * @param uri  the String formatted from uri
     * @param args arguments for the string formatting of the uri
     * @return the builder
     */
    public RouteDefinition fromF(String uri, Object... args) {
        return from(String.format(uri, args));
    }

    /**
     * Creates a new route from the given endpoint
     *
     * @param endpoint  the from endpoint
     * @return the builder
     */
    public RouteDefinition from(Endpoint endpoint) {
        getRouteCollection().setCamelContext(getContext());
        RouteDefinition<?> answer = new RouteDefinition<>().input(new FromDefinition().endpoint(endpoint));
        getRouteCollection().addRoute(answer);
        configureRoute(answer);
        return answer;
    }

    public RouteDefinition from(EndpointConsumerBuilder endpointDefinition) {
        getRouteCollection().setCamelContext(getContext());
        RouteDefinition<?> answer = new RouteDefinition<>().input(new FromDefinition().uri(endpointDefinition));
        getRouteCollection().addRoute(answer);
        return answer;
    }

    /**
     * Installs the given <a href="http://camel.apache.org/error-handler.html">error handler</a> builder
     *
     * @param errorHandlerBuilder  the error handler to be used by default for all child routes
     */
    public void errorHandler(ErrorHandlerBuilder errorHandlerBuilder) {
        if (!getRouteCollection().getRoutes().isEmpty()) {
            throw new IllegalArgumentException("errorHandler must be defined before any routes in the RouteBuilder");
        }
        getRouteCollection().setCamelContext(getContext());
        setErrorHandlerBuilder(errorHandlerBuilder);
    }

    /**
     * Injects a property placeholder value with the given key converted to the given type.
     *
     * @param key  the property key
     * @param type the type to convert the value as
     * @return the value, or <tt>null</tt> if value is empty
     * @throws Exception is thrown if property with key not found or error converting to the given type.
     */
    public <T> T propertyInject(String key, Class<T> type) throws Exception {
        StringHelper.notEmpty(key, "key");
        ObjectHelper.notNull(type, "Class type");

        // the properties component is mandatory
        PropertiesComponent pc = getContext().getPropertiesComponent();
        // resolve property
        Optional<String> value = pc.resolveProperty(key);

        if (value.isPresent()) {
            return getContext().getTypeConverter().mandatoryConvertTo(type, value.get());
        } else {
            return null;
        }
    }

    /**
     * Adds a route for an interceptor that intercepts every processing step.
     *
     * @return the builder
     */
    public InterceptDefinition<?> intercept() {
        if (!getRouteCollection().getRoutes().isEmpty()) {
            throw new IllegalArgumentException("intercept must be defined before any routes in the RouteBuilder");
        }
        getRouteCollection().setCamelContext(getContext());
        InterceptDefinition answer = new InterceptDefinition();
        getRouteCollection().addIntercept(answer);
        return answer;
    }

    /**
     * Adds a route for an interceptor that intercepts incoming messages on any inputs in this route
     *
     * @return the builder
     */
    public InterceptFromDefinition<?> interceptFrom() {
        if (!getRouteCollection().getRoutes().isEmpty()) {
            throw new IllegalArgumentException("interceptFrom must be defined before any routes in the RouteBuilder");
        }
        getRouteCollection().setCamelContext(getContext());
        InterceptFromDefinition answer = new InterceptFromDefinition();
        getRouteCollection().addInterceptFrom(answer);
        return answer;
    }

    /**
     * Adds a route for an interceptor that intercepts incoming messages on the given endpoint.
     *
     * @param uri  endpoint uri
     * @return the builder
     */
    public InterceptFromDefinition<?> interceptFrom(String uri) {
        if (!getRouteCollection().getRoutes().isEmpty()) {
            throw new IllegalArgumentException("interceptFrom must be defined before any routes in the RouteBuilder");
        }
        getRouteCollection().setCamelContext(getContext());
        InterceptFromDefinition answer = new InterceptFromDefinition().uri(uri);
        getRouteCollection().addInterceptFrom(answer);
        return answer;
    }

    /**
     * Applies a route for an interceptor if an exchange is send to the given endpoint
     *
     * @param uri  endpoint uri
     * @return the builder
     */
    public InterceptSendToEndpointDefinition interceptSendToEndpoint(String uri) {
        if (!getRouteCollection().getRoutes().isEmpty()) {
            throw new IllegalArgumentException("interceptSendToEndpoint must be defined before any routes in the RouteBuilder");
        }
        getRouteCollection().setCamelContext(getContext());
        InterceptSendToEndpointDefinition answer = new InterceptSendToEndpointDefinition().uri(uri);
        getRouteCollection().addInterceptSendTo(answer);
        return answer;
    }

    /**
     * <a href="http://camel.apache.org/exception-clause.html">Exception clause</a>
     * for catching certain exceptions and handling them.
     *
     * @param exception exception to catch
     * @return the builder
     */
    public OnExceptionDefinition<?> onException(Class<? extends Throwable> exception) {
        // is only allowed at the top currently
        if (!getRouteCollection().getRoutes().isEmpty()) {
            throw new IllegalArgumentException("onException must be defined before any routes in the RouteBuilder");
        }
        getRouteCollection().setCamelContext(getContext());
        OnExceptionDefinition answer = new OnExceptionDefinition();
        getRouteCollection().addOnException(answer);
        return answer;
    }

    /**
     * <a href="http://camel.apache.org/exception-clause.html">Exception clause</a>
     * for catching certain exceptions and handling them.
     *
     * @param exceptions list of exceptions to catch
     * @return the builder
     */
    public OnExceptionDefinition<?> onException(Class<? extends Throwable>... exceptions) {
        OnExceptionDefinition last = null;
        for (Class<? extends Throwable> ex : exceptions) {
            last = last == null ? onException(ex) : last.onException(ex);
        }
        return last != null ? last : onException(Exception.class);
    }

    /**
     * <a href="http://camel.apache.org/oncompletion.html">On completion</a>
     * callback for doing custom routing when the {@link org.apache.camel.Exchange} is complete.
     *
     * @return the builder
     */
    public OnCompletionDefinition<?> onCompletion() {
        // is only allowed at the top currently
        if (!getRouteCollection().getRoutes().isEmpty()) {
            throw new IllegalArgumentException("onCompletion must be defined before any routes in the RouteBuilder");
        }
        getRouteCollection().setCamelContext(getContext());
        OnCompletionDefinition<?> onCompletion = new OnCompletionDefinition<>();
        getRouteCollection().addOnCompletion(onCompletion);
        return onCompletion;
    }
    
    public void addRoutesToCamelContext(CamelContext context) throws Exception {
        // must configure routes before rests
        configureRoutes(context);
        configureRests(context);

        // but populate rests before routes, as we want to turn rests into routes
        populateRests();
        populateTransformers();
        populateValidators();
        populateRoutes();
    }

    /**
     * Configures the routes
     *
     * @param context the Camel context
     * @return the routes configured
     * @throws Exception can be thrown during configuration
     */
    public RoutesDefinition configureRoutes(CamelContext context) throws Exception {
        setContext(context);
        checkInitialized();
        routeCollection.setCamelContext(context);
        return routeCollection;
    }

    /**
     * Configures the rests
     *
     * @param context the Camel context
     * @return the rests configured
     * @throws Exception can be thrown during configuration
     */
    public RestsDefinition configureRests(CamelContext context) throws Exception {
        setContext(context);
        restCollection.setCamelContext(context);
        return restCollection;
    }
    
    @Override
    public void setErrorHandlerBuilder(ErrorHandlerBuilder errorHandlerBuilder) {
        super.setErrorHandlerBuilder(errorHandlerBuilder);
        getRouteCollection().setErrorHandlerFactory(getErrorHandlerBuilder());
    }

    // Implementation methods
    // -----------------------------------------------------------------------
    protected void checkInitialized() throws Exception {
        if (initialized.compareAndSet(false, true)) {
            // Set the CamelContext ErrorHandler here
            CamelContext camelContext = getContext();
            if (camelContext.adapt(ExtendedCamelContext.class).getErrorHandlerFactory() instanceof ErrorHandlerBuilder) {
                setErrorHandlerBuilder((ErrorHandlerBuilder) camelContext.adapt(ExtendedCamelContext.class).getErrorHandlerFactory());
            }
            configure();
            // mark all route definitions as custom prepared because
            // a route builder prepares the route definitions correctly already
            for (RouteDefinition route : getRouteCollection().getRoutes()) {
                route.markPrepared();
            }
        }
    }

    protected void populateRoutes() throws Exception {
        CamelContext camelContext = getContext();
        if (camelContext == null) {
            throw new IllegalArgumentException("CamelContext has not been injected!");
        }
        getRouteCollection().setCamelContext(camelContext);
        camelContext.getExtension(Model.class).addRouteDefinitions(getRouteCollection().getRoutes());
    }

    protected void populateRests() throws Exception {
        CamelContext camelContext = getContext();
        if (camelContext == null) {
            throw new IllegalArgumentException("CamelContext has not been injected!");
        }
        getRestCollection().setCamelContext(camelContext);

        // setup rest configuration before adding the rests
        if (getRestConfigurations() != null) {
            for (Map.Entry<String, RestConfigurationDefinition> entry : getRestConfigurations().entrySet()) {
                RestConfiguration config = new RestConfigurationReifier(entry.getValue()).asRestConfiguration(getContext());
                if ("".equals(entry.getKey())) {
                    camelContext.setRestConfiguration(config);
                } else {
                    camelContext.addRestConfiguration(config);
                }
            }
        }
        // cannot add rests as routes yet as we need to initialize this specially
        camelContext.getExtension(Model.class).addRestDefinitions(getRestCollection().getRests(), false);

        // convert rests api-doc into routes so they are routes for runtime
        for (RestConfiguration config : camelContext.getRestConfigurations()) {
            if (config.getApiContextPath() != null) {
                // avoid adding rest-api multiple times, in case multiple RouteBuilder classes is added
                // to the CamelContext, as we only want to setup rest-api once
                // so we check all existing routes if they have rest-api route already added
                boolean hasRestApi = false;
                for (RouteDefinition route : camelContext.getExtension(Model.class).getRouteDefinitions()) {
                    FromDefinition from = route.getInput();
                    if (from.getEndpointUri() != null && from.getEndpointUri().startsWith("rest-api:")) {
                        hasRestApi = true;
                    }
                }
                if (!hasRestApi) {
                    RouteDefinition route = asRouteApiDefinition(camelContext, config);
                    log.debug("Adding routeId: {} as rest-api route", route.getId());
                    getRouteCollection().addRoute(route);
                }
            }
        }
        // add rest as routes and have them prepared as well via routeCollection.route method
        getRestCollection().getRests()
            .forEach(rest -> asRouteDefinition(getContext(), rest)
                .forEach(route -> getRouteCollection().addRoute(route)));
    }

    /**
     * Transforms the rest api configuration into a {@link org.apache.camel.model.RouteDefinition} which
     * Camel routing engine uses to service the rest api docs.
     */
    public static RouteDefinition asRouteApiDefinition(CamelContext camelContext, RestConfiguration configuration) {
        RouteDefinition answer = new RouteDefinition();

        // create the from endpoint uri which is using the rest-api component
        String from = "rest-api:" + configuration.getApiContextPath();

        // append options
        Map<String, Object> options = new HashMap<String, Object>();

        String routeId = configuration.getApiContextRouteId();
        if (routeId == null) {
            routeId = answer.idOrCreate(camelContext.adapt(ExtendedCamelContext.class).getNodeIdFactory());
        }
        options.put("routeId", routeId);
        if (configuration.getComponent() != null && !configuration.getComponent().isEmpty()) {
            options.put("componentName", configuration.getComponent());
        }
        if (configuration.getApiContextIdPattern() != null) {
            options.put("contextIdPattern", configuration.getApiContextIdPattern());
        }

        if (!options.isEmpty()) {
            String query;
            try {
                query = URISupport.createQueryString(options);
            } catch (URISyntaxException e) {
                throw ObjectHelper.wrapRuntimeCamelException(e);
            }
            from = from + "?" + query;
        }

        // we use the same uri as the producer (so we have a little route for the rest api)
        String to = from;
        answer.rest();
        answer.input(from);
        answer.id(routeId);
        answer.to(to);

        return answer;
    }

    /**
     * Transforms this REST definition into a list of {@link org.apache.camel.model.RouteDefinition} which
     * Camel routing engine can add and run. This allows us to define REST services using this
     * REST DSL and turn those into regular Camel routes.
     *
     * @param camelContext The Camel context
     */
    public static List<RouteDefinition> asRouteDefinition(CamelContext camelContext, RestDefinition definition) {
        ObjectHelper.notNull(camelContext, "CamelContext");

        // sanity check this rest definition do not have duplicates
        validateUniquePaths(definition);

        List<RouteDefinition> answer = new ArrayList<>();
        if (camelContext.getRestConfigurations().isEmpty()) {
            // make sure to initialize a rest configuration when its empty
            // lookup a global which may have been setup via camel-spring-boot etc
            RestConfiguration conf = CamelContextHelper.lookup(camelContext, RestConstants.DEFAULT_REST_CONFIGURATION_ID, RestConfiguration.class);
            if (conf == null) {
                conf = CamelContextHelper.findByType(camelContext, RestConfiguration.class);
            }
            if (conf != null) {
                camelContext.setRestConfiguration(conf);
            } else {
                camelContext.setRestConfiguration(new RestConfiguration());
            }
        }
        for (RestConfiguration config : camelContext.getRestConfigurations()) {
            addRouteDefinition(camelContext, definition, answer, config.getComponent());
        }
        return answer;
    }

    private static void validateUniquePaths(RestDefinition rest) {
        Set<String> paths = new HashSet<>();
        for (VerbDefinition verb : rest.getVerbs()) {
            String path = verb.asVerb();
            if (verb.getUri() != null) {
                path += ":" + verb.getUri();
            }
            if (!paths.add(path)) {
                throw new IllegalArgumentException("Duplicate verb detected in rest-dsl: " + path);
            }
        }
    }

    private static void addRouteDefinition(CamelContext camelContext, RestDefinition definition, List<RouteDefinition> answer, String component) {
        for (VerbDefinition verb : definition.getVerbs()) {
            RouteDefinition route = asRouteDefinition(camelContext, definition, component, verb);
            answer.add(route);
        }
    }

    private static RouteDefinition asRouteDefinition(CamelContext camelContext, RestDefinition definition, String component, VerbDefinition verb) {
        // either the verb has a singular to or a embedded route
        RouteDefinition<?> route = verb.getRoute();
        ObjectHelper.notNull(route, "route");

        // ensure property placeholders is resolved on the verb
        try {
            ProcessorDefinitionHelper.resolvePropertyPlaceholders(camelContext, verb);
            for (RestOperationParamDefinition param : verb.getParams()) {
                ProcessorDefinitionHelper.resolvePropertyPlaceholders(camelContext, param);
            }
        } catch (Exception e) {
            throw ObjectHelper.wrapRuntimeCamelException(e);
        }

        // add the binding
        RestBindingDefinition binding = new RestBindingDefinition();
        binding.setComponent(component);
        binding.setType(verb.getType());
        binding.setOutType(verb.getOutType());
        // verb takes precedence over configuration on rest
        if (verb.getConsumes() != null) {
            binding.setConsumes(verb.getConsumes());
        } else {
            binding.setConsumes(definition.getConsumes());
        }
        if (verb.getProduces() != null) {
            binding.setProduces(verb.getProduces());
        } else {
            binding.setProduces(definition.getProduces());
        }
        if (verb.getBindingMode() != null) {
            binding.setBindingMode(verb.getBindingMode());
        } else {
            binding.setBindingMode(definition.getBindingMode());
        }
        if (verb.getSkipBindingOnErrorCode() != null) {
            binding.setSkipBindingOnErrorCode(verb.getSkipBindingOnErrorCode());
        } else {
            binding.setSkipBindingOnErrorCode(definition.getSkipBindingOnErrorCode());
        }
        if (verb.getEnableCORS() != null) {
            binding.setEnableCORS(verb.getEnableCORS());
        } else {
            binding.setEnableCORS(definition.getEnableCORS());
        }
        // register all the default values for the query parameters
        for (RestOperationParamDefinition param : verb.getParams()) {
            if (RestParamType.query == param.getType() && ObjectHelper.isNotEmpty(param.getDefaultValue())) {
//                binding.addDefaultValue(param.getName(), param.getDefaultValue());
                throw new UnsupportedOperationException("TODO");
            }
        }

        route.setRestBinding(binding);

        // create the from endpoint uri which is using the rest component
        String from = "rest:" + verb.asVerb() + ":" + definition.buildUri(verb);

        // append options
        Map<String, Object> options = new HashMap<String, Object>();
        // verb takes precedence over configuration on rest
        if (verb.getConsumes() != null) {
            options.put("consumes", verb.getConsumes());
        } else if (definition.getConsumes() != null) {
            options.put("consumes", definition.getConsumes());
        }
        if (verb.getProduces() != null) {
            options.put("produces", verb.getProduces());
        } else if (definition.getProduces() != null) {
            options.put("produces", definition.getProduces());
        }

        // append optional type binding information
        String inType = binding.getType();
        if (inType != null) {
            options.put("inType", inType);
        }
        String outType = binding.getOutType();
        if (outType != null) {
            options.put("outType", outType);
        }
        // if no route id has been set, then use the verb id as route id
        if (!route.hasCustomIdAssigned()) {
            // use id of verb as route id
            String id = verb.getId();
            if (id != null) {
                route.setId(id);
            }
        }

        String routeId = verb.idOrCreate(camelContext.adapt(ExtendedCamelContext.class).getNodeIdFactory());

        if (!verb.getUsedForGeneratingNodeId()) {
            routeId = route.idOrCreate(camelContext.adapt(ExtendedCamelContext.class).getNodeIdFactory());
        }

        verb.setRouteId(routeId);
        options.put("routeId", routeId);
        if (component != null && !component.isEmpty()) {
            options.put("componentName", component);
        }

        // include optional description, which we favor from 1) to/route description 2) verb description 3) rest description
        // this allows end users to define general descriptions and override then per to/route or verb
        String description = null;
        if (!route.getOutputs().isEmpty()) {
            ProcessorDefinition<?> p = route.getOutputs().get(0);
            description = p.getDescriptionText();
        }
        if (description == null) {
            description = route.getDescriptionText();
        }
        if (description == null) {
            description = verb.getDescriptionText();
        }
        if (description == null) {
            description = definition.getDescriptionText();
        }
        if (description != null) {
            options.put("description", description);
        }

        if (!options.isEmpty()) {
            String query;
            try {
                query = URISupport.createQueryString(options);
            } catch (URISyntaxException e) {
                throw ObjectHelper.wrapRuntimeCamelException(e);
            }
            from = from + "?" + query;
        }

        String path = definition.getPath();
        String s1 = FileUtil.stripTrailingSeparator(path);
        String s2 = FileUtil.stripLeadingSeparator(verb.getUri());
        String allPath;
        if (s1 != null && s2 != null) {
            allPath = s1 + "/" + s2;
        } else if (path != null) {
            allPath = path;
        } else {
            allPath = verb.getUri();
        }

        // each {} is a parameter (url templating)
        if (allPath != null) {
            String[] arr = allPath.split("\\/");
            for (String a : arr) {
                // need to resolve property placeholders first
                try {
                    a = camelContext.resolvePropertyPlaceholders(a);
                } catch (Exception e) {
                    throw ObjectHelper.wrapRuntimeCamelException(e);
                }
                if (a.startsWith("{") && a.endsWith("}")) {
                    String key = a.substring(1, a.length() - 1);
                    //  merge if exists
                    boolean found = false;
                    for (RestOperationParamDefinition param : verb.getParams()) {
                        // name is mandatory
                        String name = param.getName();
                        StringHelper.notEmpty(name, "parameter name");
                        // need to resolve property placeholders first
                        try {
                            name = camelContext.resolvePropertyPlaceholders(name);
                        } catch (Exception e) {
                            throw ObjectHelper.wrapRuntimeCamelException(e);
                        }
                        if (name.equalsIgnoreCase(key)) {
                            param.type(RestParamType.path);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        param(verb).name(key).type(RestParamType.path).endParam();
                    }
                }
            }
        }

        if (verb.getType() != null) {
            String bodyType = verb.getType();
            if (bodyType.endsWith("[]")) {
                bodyType = "List[" + bodyType.substring(0, bodyType.length() - 2) + "]";
            }
            RestOperationParamDefinition param = findParam(verb, RestParamType.body.name());
            if (param == null) {
                // must be body type and set the model class as data type
                param(verb).name(RestParamType.body.name()).type(RestParamType.body).dataType(bodyType).endParam();
            } else {
                // must be body type and set the model class as data type
                param.type(RestParamType.body).dataType(bodyType);
            }
        }

        // the route should be from this rest endpoint
        route.rest();
        route.rest(definition);
        route.input(from);
        route.routeId(routeId);
        return route;
    }

    private static RestOperationParamDefinition findParam(VerbDefinition verb, String name) {
        for (RestOperationParamDefinition param : verb.getParams()) {
            if (name.equals(param.getName())) {
                return param;
            }
        }
        return null;
    }

    public static RestOperationParamDefinition param(VerbDefinition verb) {
        return new RestOperationParamDefinition(verb);
    }

    protected void populateTransformers() {
        CamelContext camelContext = getContext();
        if (camelContext == null) {
            throw new IllegalArgumentException("CamelContext has not been injected!");
        }
        for (TransformerBuilder tdb : transformerBuilders) {
            tdb.configure(camelContext);
        }
    }

    protected void populateValidators() {
        CamelContext camelContext = getContext();
        if (camelContext == null) {
            throw new IllegalArgumentException("CamelContext has not been injected!");
        }
        for (ValidatorBuilder vb : validatorBuilders) {
            vb.configure(camelContext);
        }
    }

    public RestsDefinition getRestCollection() {
        return restCollection;
    }

    public Map<String, RestConfigurationDefinition> getRestConfigurations() {
        return restConfigurations;
    }

    public void setRestCollection(RestsDefinition restCollection) {
        this.restCollection = restCollection;
    }

    public void setRouteCollection(RoutesDefinition routeCollection) {
        this.routeCollection = routeCollection;
    }

    public RoutesDefinition getRouteCollection() {
        return this.routeCollection;
    }

    protected void configureRest(RestDefinition rest) {
        // noop
    }

    protected void configureRoute(RouteDefinition route) {
        // noop
    }

}
