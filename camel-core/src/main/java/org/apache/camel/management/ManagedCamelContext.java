package org.apache.camel.management;

import org.apache.camel.CamelContext;
import org.apache.camel.api.management.mbean.ManagedCamelContextMBean;
import org.apache.camel.api.management.mbean.ManagedProcessorMBean;
import org.apache.camel.api.management.mbean.ManagedRouteMBean;

public interface ManagedCamelContext extends CamelContext {

    /**
     * Gets the managed processor client api from any of the routes which with the given id
     *
     * @param id id of the processor
     * @param type the managed processor type from the {@link org.apache.camel.api.management.mbean} package.
     * @return the processor or <tt>null</tt> if not found
     * @throws IllegalArgumentException if the type is not compliant
     */
    <T extends ManagedProcessorMBean> T getManagedProcessor(String id, Class<T> type);

    /**
     * Gets the managed route client api with the given route id
     *
     * @param routeId id of the route
     * @param type the managed route type from the {@link org.apache.camel.api.management.mbean} package.
     * @return the route or <tt>null</tt> if not found
     * @throws IllegalArgumentException if the type is not compliant
     */
    <T extends ManagedRouteMBean> T getManagedRoute(String routeId, Class<T> type);

    /**
     * Gets the managed Camel CamelContext client api
     */
    ManagedCamelContextMBean getManagedCamelContext();

}
