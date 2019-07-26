package org.apache.camel.reifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Endpoint;
import org.apache.camel.Expression;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.builder.EndpointConsumerBuilder;
import org.apache.camel.builder.EndpointProducerBuilder;
import org.apache.camel.model.ExecutorServiceAwareDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.ProcessorDefinitionHelper;
import org.apache.camel.model.language.ExpressionDefinition;
import org.apache.camel.processor.aggregate.AggregationStrategyBeanAdapter;
import org.apache.camel.spi.ExecutorServiceManager;
import org.apache.camel.spi.Policy;
import org.apache.camel.spi.RouteContext;
import org.apache.camel.support.CamelContextHelper;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractReifier<T> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final T definition;

    public AbstractReifier(T definition) {
        this.definition = definition;
    }

    protected static String asString(RouteContext context, Object value) {
        return resolve(context, String.class, value);
    }

    protected static String asString(CamelContext context, Object value) {
        return resolve(context, String.class, value);
    }

    protected static String asString(RouteContext context, Object value, String defaultValue) {
        return asString(context.getCamelContext(), value, defaultValue);
    }

    protected static String asString(CamelContext context, Object value, String defaultValue) {
        return value != null ? resolve(context, String.class, value) : defaultValue;
    }

    protected static boolean asBoolean(RouteContext context, Object value) {
        return resolve(context, boolean.class, value);
    }

    protected static boolean asBoolean(CamelContext context, Object value) {
        return resolve(context, boolean.class, value);
    }

    protected static boolean asBoolean(RouteContext context, Object value, boolean defaultValue) {
        return value != null ? resolve(context, boolean.class, value) : defaultValue;
    }

    // TODO: should be protected
    public static boolean asBoolean(CamelContext context, Object value, boolean defaultValue) {
        return value != null ? resolve(context, boolean.class, value) : defaultValue;
    }

    protected static int asInt(RouteContext context, Object value) {
        return resolve(context, int.class, value);
    }

    protected static int asInt(CamelContext context, Object value) {
        return resolve(context, int.class, value);
    }

    protected static int asInt(RouteContext context, Object value, int defaultValue) {
        return value != null ? resolve(context, int.class, value) : defaultValue;
    }

    protected static long asLong(RouteContext context, Object value) {
        return resolve(context, long.class, value);
    }

    protected static long asLong(RouteContext context, Object value, long defaultValue) {
        return value != null ? resolve(context, long.class, value) : defaultValue;
    }

    protected static Class<?> asClass(RouteContext context, Object value) {
        return resolve(context, Class.class, value);
    }

    protected static Class<?> asClass(CamelContext context, Object value) {
        return resolve(context, Class.class, value);
    }

    protected static <T> T resolve(RouteContext routeContext, Class<T> clazz, Object value) {
        return resolve(routeContext.getCamelContext(), clazz, value);
    }

    // TODO: should be protected
    public static <T> T resolve(CamelContext context, Class<T> clazz, Object value) {
        if (value instanceof String) {
            value = context.resolvePropertyPlaceholders((String) value);
            String str = (String) value;
            if (str.startsWith("#bean:")) {
                String ref = str.substring("#bean:".length());
                value = CamelContextHelper.lookup(context, ref);
            }
        }
        if (value != null) {
            // TODO: is that necessary ? the conversion should do it
            if (clazz == Class.class && value instanceof String) {
                Class<?> res = context.getClassResolver().resolveClass((String) value);
                if (res == null) {
                    throw new IllegalArgumentException("Value " + value + " converted to " + clazz.getName() + " cannot be null");
                }
                return clazz.cast(res);
            } else {
                return CamelContextHelper.mandatoryConvertTo(context, clazz, value);
            }
        }
        return null;
    }

    // TODO: should be protected
    public static <T> List<T> resolveList(CamelContext camelContext, Class<T> clazz, Object value, Supplier<List<T>> defaultValue) {
        if (value instanceof String) {
            value = Arrays.asList(((String) value).split(","));
        }
        if (value instanceof List) {
            return ((List<?>) value).stream().map(o -> resolve(camelContext, clazz, o)).collect(Collectors.toList());
        }
        if (value == null) {
            return defaultValue.get();
        } else {
            throw new IllegalArgumentException("Cannot convert object '" + value + "' to List<" + clazz.getName() + ">");
        }
    }

    public static <T> List<T> resolveList(RouteContext routeContext, Class<T> clazz, Object value, Supplier<List<T>> defaultValue) {
        return resolveList(routeContext.getCamelContext(), clazz, value, defaultValue);
    }

    protected static Processor resolveProcessor(RouteContext routeContext, Object definition) {
        if (definition instanceof Processor) {
            return (Processor) definition;
        } else if (definition instanceof ProcessorDefinition) {
            try {
                return ProcessorReifier.reifier((ProcessorDefinition) definition).createProcessor(routeContext);
            } catch (Exception e) {
                throw new IllegalArgumentException("Unable to create processor from'" + definition + "'", e);
            }
        } else if (definition instanceof String) {
            String str = (String) definition;
            if (str.startsWith("#bean:")) {
                String ref = str.substring("#bean:".length());
                Processor answer = routeContext.lookup(ref, Processor.class);
                if (answer == null) {
                    throw new IllegalArgumentException("Processor reference " + ref
                            + " not found in registry.");
                }
                return answer;
            }
        }
        throw new IllegalArgumentException("Cannot convert object '" + definition + "' to Processor");
    }

    public static List<Class<? extends Throwable>> resolveExceptions(RouteContext routeContext, Object value) {
        // must use the class resolver from CamelContext to load classes to ensure it can
        // be loaded in all kind of environments such as JEE servers and OSGi etc.
        List<Class<? extends Throwable>> answer = new ArrayList<>();
        if (value != null) {
            List<?> list = resolve(routeContext, List.class, value);
            for (Object name : list) {
                Class<?> type = asClass(routeContext, name);
                if (type == null) {
                    throw new IllegalArgumentException("Unable to resolve class " + name);
                }
                if (!Throwable.class.isAssignableFrom(type)) {
                    throw new IllegalArgumentException("Class " + name + " should extend " + Throwable.class.getName());
                }
                answer.add((Class<? extends Throwable>) type);
            }
        }
        return answer;
    }

    protected static <P extends Policy> P resolvePolicy(RouteContext routeContext, Class<P> type, Object value) {
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        if (value instanceof String) {
            value = routeContext.getCamelContext().resolvePropertyPlaceholders((String) value);
            String str = (String) value;
            if (str.startsWith("#bean:")) {
                String ref = str.substring("#bean:".length());
                value = routeContext.lookup(ref, Policy.class);
                if (value == null) {
                    throw new IllegalArgumentException("Policy reference " + ref + " not found in registry.");
                }
            }
        }

        // no explicit reference given from user so we can use some convention over configuration here

        // try to lookup by scoped type
        // try find by type, note that this method is not supported by all registry
        Map<String, ?> types = routeContext.lookupByType(type);
        if (types.size() == 1) {
            // only one policy defined so use it
            Object found = types.values().iterator().next();
            if (type.isInstance(found)) {
                return type.cast(found);
            }
        }

        return null;
    }

    protected static Expression asExpression(RouteContext routeContext, Object expression) {
        if (expression instanceof ExpressionDefinition) {
            return ((ExpressionDefinition) expression).createExpression(routeContext);
        }
        if (expression instanceof String) {
            // TODO
        }
        return null;
    }

    protected static Predicate asPredicate(RouteContext routeContext, Object expression) {
        if (expression instanceof ExpressionDefinition) {
            return ((ExpressionDefinition) expression).createPredicate(routeContext);
        }
        if (expression instanceof String) {
            // TODO
        }
        return null;
    }

    protected static Predicate asPredicate(CamelContext context, Object expression) {
        if (expression instanceof ExpressionDefinition) {
            return ((ExpressionDefinition) expression).createPredicate(context);
        }
        if (expression instanceof String) {
            // TODO
        }
        return null;
    }

    protected AggregationStrategy resolveAggregationStrategy(RouteContext routeContext, Object value, Object method, Object allowNull, Supplier<AggregationStrategy> defaultStrategy) {
        AggregationStrategy strategy;
        if (value instanceof AggregationStrategy) {
            strategy = (AggregationStrategy) value;
        } else if (value instanceof String) {
            value = routeContext.getCamelContext().resolvePropertyPlaceholders((String) value);
            String str = (String) value;
            if (str.startsWith("#bean:")) {
                String ref = str.substring("#bean:".length());
                Object aggStrategy = routeContext.lookup(ref, Object.class);
                if (aggStrategy instanceof AggregationStrategy) {
                    strategy = (AggregationStrategy) aggStrategy;
                } else if (aggStrategy != null) {
                    AggregationStrategyBeanAdapter adapter = new AggregationStrategyBeanAdapter(aggStrategy, asString(routeContext, method));
                    if (allowNull != null) {
                        adapter.setAllowNullNewExchange(asBoolean(routeContext, allowNull));
                        adapter.setAllowNullOldExchange(asBoolean(routeContext, allowNull));
                    }
                    strategy = adapter;
                } else {
                    throw new IllegalArgumentException("Cannot find AggregationStrategy in Registry with name: " + ref);
                }
            } else {
                throw new IllegalArgumentException("Cannot convert string '" + value + "' to AggregationStrategy");
            }
        } else if (value != null) {
            throw new IllegalArgumentException("Cannot convert object '" + value + "' to AggregationStrategy");
        } else {
            strategy = defaultStrategy.get();
        }

        if (strategy instanceof CamelContextAware) {
            ((CamelContextAware) strategy).setCamelContext(routeContext.getCamelContext());
        }

        return strategy;
    }

    protected static boolean willCreateNewThreadPool(RouteContext routeContext, Object executorService, boolean useDefault) {
        ExecutorServiceManager manager = routeContext.getCamelContext().getExecutorServiceManager();
        ObjectHelper.notNull(manager, "ExecutorServiceManager", routeContext.getCamelContext());

        if (executorService != null) {
            return resolve(routeContext, ExecutorService.class, executorService) == null;
        } else {
            return useDefault;
        }

    }

    /**
     * Will lookup and get the configured {@link ScheduledExecutorService} from the given definition.
     * <p/>
     * This method will lookup for configured thread pool in the following order
     * <ul>
     * <li>from the definition if any explicit configured executor service.</li>
     * <li>from the {@link org.apache.camel.spi.Registry} if found</li>
     * <li>from the known list of {@link org.apache.camel.spi.ThreadPoolProfile ThreadPoolProfile(s)}.</li>
     * <li>if none found, then <tt>null</tt> is returned.</li>
     * </ul>
     * The various {@link ExecutorServiceAwareDefinition} should use this helper method to ensure they support
     * configured executor services in the same coherent way.
     *
     * @param routeContext the rout context
     * @param name         name which is appended to the thread name, when the {@link ExecutorService}
     *                     is created based on a {@link org.apache.camel.spi.ThreadPoolProfile}.
     * @param definition   the node definition which may leverage executor service.
     * @param useDefault   whether to fallback and use a default thread pool, if no explicit configured
     * @return the configured executor service, or <tt>null</tt> if none was configured.
     * @throws IllegalArgumentException is thrown if the found instance is not a ScheduledExecutorService type,
     *                                  or lookup of executor service in {@link org.apache.camel.spi.Registry} was not found
     */
    protected static ScheduledExecutorService getConfiguredScheduledExecutorService(RouteContext routeContext, String name,
                                                                             Object definition,
                                                                             boolean useDefault) throws IllegalArgumentException {
        ExecutorServiceManager manager = routeContext.getCamelContext().getExecutorServiceManager();
        ObjectHelper.notNull(manager, "ExecutorServiceManager", routeContext.getCamelContext());

        // prefer to use explicit configured executor on the definition
        if (definition instanceof ExecutorService) {
            ExecutorService executorService = (ExecutorService) definition;
            if (executorService instanceof ScheduledExecutorService) {
                return (ScheduledExecutorService) executorService;
            }
            throw new IllegalArgumentException("ExecutorService " + definition + " is not an ScheduledExecutorService instance");
        } else if (definition instanceof String) {
            String str = (String) definition;
            if (str.startsWith("#bean:")) {
                String ref = str.substring("#bean:".length());
                ScheduledExecutorService answer = ProcessorDefinitionHelper.lookupScheduledExecutorServiceRef(routeContext, name, definition, ref);
                if (answer == null) {
                    throw new IllegalArgumentException("ExecutorServiceRef " + ref
                            + " not found in registry (as an ScheduledExecutorService instance) or as a thread pool profile.");
                }
                return answer;
            } else {
                throw new IllegalArgumentException("Cannot convert object '" + definition + "' to ScheduledExecutorService");
            }
        } else if (useDefault) {
            return manager.newDefaultScheduledThreadPool(definition, name);
        }

        return null;
    }

    /**
     * Will lookup and get the configured {@link ScheduledExecutorService} from the given definition.
     * <p/>
     * This method will lookup for configured thread pool in the following order
     * <ul>
     * <li>from the definition if any explicit configured executor service.</li>
     * <li>from the {@link org.apache.camel.spi.Registry} if found</li>
     * <li>from the known list of {@link org.apache.camel.spi.ThreadPoolProfile ThreadPoolProfile(s)}.</li>
     * <li>if none found, then <tt>null</tt> is returned.</li>
     * </ul>
     * The various {@link ExecutorServiceAwareDefinition} should use this helper method to ensure they support
     * configured executor services in the same coherent way.
     *
     * @param routeContext the rout context
     * @param name         name which is appended to the thread name, when the {@link ExecutorService}
     *                     is created based on a {@link org.apache.camel.spi.ThreadPoolProfile}.
     * @param definition   the node definition which may leverage executor service.
     * @param useDefault   whether to fallback and use a default thread pool, if no explicit configured
     * @return the configured executor service, or <tt>null</tt> if none was configured.
     * @throws IllegalArgumentException is thrown if the found instance is not a ScheduledExecutorService type,
     *                                  or lookup of executor service in {@link org.apache.camel.spi.Registry} was not found
     */
    protected static ExecutorService getConfiguredExecutorService(RouteContext routeContext, String name,
                                                           Object definition,
                                                           boolean useDefault) throws IllegalArgumentException {
        ExecutorServiceManager manager = routeContext.getCamelContext().getExecutorServiceManager();
        ObjectHelper.notNull(manager, "ExecutorServiceManager", routeContext.getCamelContext());

        // prefer to use explicit configured executor on the definition
        if (definition instanceof ExecutorService) {
            return (ExecutorService) definition;
        } else if (definition instanceof String) {
            String str = (String) definition;
            if (str.startsWith("#bean:")) {
                String ref = str.substring("#bean:".length());
                ExecutorService answer = ProcessorDefinitionHelper.lookupExecutorServiceRef(routeContext, name, definition, ref);
                if (answer == null) {
                    throw new IllegalArgumentException("ExecutorServiceRef " + ref
                            + " not found in registry (as an ExecutorService instance) or as a thread pool profile.");
                }
                return answer;
            } else {
                throw new IllegalArgumentException("Cannot convert object '" + definition + "' to ExecutorService");
            }
        } else if (useDefault) {
            return manager.newDefaultThreadPool(definition, name);
        }

        return null;
    }

    protected static Endpoint resolveEndpoint(RouteContext routeContext, Object endpoint, Object uri) {
        return resolveEndpoint(routeContext.getCamelContext(), endpoint, uri);
    }

    protected static Endpoint resolveEndpoint(CamelContext camelContext, Object endpoint, Object uri) {
        if (endpoint instanceof Endpoint) {
            return (Endpoint) endpoint;
        }
        if (endpoint instanceof String) {
            Endpoint e = resolve(camelContext, Endpoint.class, endpoint);
            if (e == null) {
                throw new IllegalArgumentException("Could not find Endpoint with name " + endpoint);
            }
            return e;
        }
        if (uri instanceof EndpointProducerBuilder) {
            return ((EndpointProducerBuilder) uri).resolve(camelContext);
        }
        if (uri instanceof EndpointConsumerBuilder) {
            return ((EndpointConsumerBuilder) uri).resolve(camelContext);
        }
        if (uri instanceof String) {
            return CamelContextHelper.getMandatoryEndpoint(camelContext, (String) uri);
        }
        throw new IllegalArgumentException("Endpoint or Uri must be set");
    }

}
