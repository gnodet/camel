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
package org.apache.camel.processor;

import java.net.URISyntaxException;

import org.apache.camel.AsyncCallback;
import org.apache.camel.AsyncProducer;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.EndpointAware;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.Traceable;
import org.apache.camel.impl.DefaultProducerCache;
import org.apache.camel.impl.InterceptSendToEndpoint;
import org.apache.camel.spi.IdAware;
import org.apache.camel.spi.ProducerCache;
import org.apache.camel.support.AsyncProcessorSupport;
import org.apache.camel.support.EndpointHelper;
import org.apache.camel.support.ServiceHelper;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.URISupport;

/**
 * Processor for forwarding exchanges to a static endpoint destination.
 *
 * @see SendDynamicProcessor
 */
public class SendProcessor extends AsyncProcessorSupport implements Traceable, EndpointAware, IdAware {

    protected transient String traceLabelToString;
    protected final CamelContext camelContext;
    protected final ExchangePattern pattern;
    protected ProducerCache producerCache;
    protected Endpoint destination;
    protected ExchangePattern destinationExchangePattern;
    protected String id;
    protected volatile long counter;

    public SendProcessor(Endpoint destination) {
        this(destination, null);
    }

    public SendProcessor(Endpoint destination, ExchangePattern pattern) {
        ObjectHelper.notNull(destination, "destination");
        this.destination = destination;
        this.camelContext = destination.getCamelContext();
        this.pattern = pattern;
        try {
            this.destinationExchangePattern = null;
            this.destinationExchangePattern = EndpointHelper.resolveExchangePatternFromUrl(destination.getEndpointUri());
        } catch (URISyntaxException e) {
            throw RuntimeCamelException.wrapRuntimeCamelException(e);
        }
        ObjectHelper.notNull(this.camelContext, "camelContext");
    }

    @Override
    public String toString() {
        return "sendTo(" + destination + ")";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTraceLabel() {
        if (traceLabelToString == null) {
            traceLabelToString = URISupport.sanitizeUri(destination.getEndpointUri());
        }
        return traceLabelToString;
    }

    @Override
    public Endpoint getEndpoint() {
        return destination;
    }

    public void process(Exchange exchange, final AsyncCallback callback) {
        if (!isStarted()) {
            exchange.setException(new IllegalStateException("SendProcessor has not been started: " + this));
            callback.done();
            return;
        }

        // we should preserve existing MEP so remember old MEP
        // if you want to permanently to change the MEP then use .setExchangePattern in the DSL
        final ExchangePattern existingPattern = exchange.getPattern();

        counter++;

        configureExchange(exchange, pattern);
        log.debug(">>>> {} {}", destination, exchange);

        // send the exchange to the destination using the producer cache for the non optimized producers
        producerCache.doInAsyncProducer(destination, exchange, callback, (producer, ex, cb) -> producer.process(ex, () -> {
            // restore previous MEP
            exchange.setPattern(existingPattern);
            // signal we are done
            cb.done();
        }));
    }
    
    public Endpoint getDestination() {
        return destination;
    }

    public ExchangePattern getPattern() {
        return pattern;
    }

    protected Exchange configureExchange(Exchange exchange, ExchangePattern pattern) {
        // destination exchange pattern overrides pattern
        if (destinationExchangePattern != null) {
            exchange.setPattern(destinationExchangePattern);
        } else if (pattern != null) {
            exchange.setPattern(pattern);
        }
        // set property which endpoint we send to
        exchange.setProperty(Exchange.TO_ENDPOINT, destination.getEndpointUri());
        return exchange;
    }

    public long getCounter() {
        return counter;
    }

    public void reset() {
        counter = 0;
    }

    protected void doStart() throws Exception {
        if (producerCache == null) {
            // use a single producer cache as we need to only hold reference for one destination
            // and use a regular HashMap as we do not want a soft reference store that may get re-claimed when low on memory
            // as we want to ensure the producer is kept around, to ensure its lifecycle is fully managed,
            // eg stopping the producer when we stop etc.
            producerCache = new DefaultProducerCache(this, camelContext, 1);
            // do not add as service as we do not want to manage the producer cache
        }
        ServiceHelper.startService(producerCache);

        // the destination could since have been intercepted by a interceptSendToEndpoint so we got to
        // lookup this before we can use the destination
        Endpoint lookup = camelContext.hasEndpoint(destination.getEndpointKey());
        if (lookup instanceof InterceptSendToEndpoint) {
            if (log.isDebugEnabled()) {
                log.debug("Intercepted sending to {} -> {}",
                        URISupport.sanitizeUri(destination.getEndpointUri()), URISupport.sanitizeUri(lookup.getEndpointUri()));
            }
            destination = lookup;
        }
        // warm up the producer by starting it so we can fail fast if there was a problem
        // however must start endpoint first
        ServiceHelper.startService(destination);

        // this SendProcessor is used a lot in Camel (eg every .to in the route DSL) and therefore we
        // want to optimize for regular producers, by using the producer directly instead of the ProducerCache.
        // Only for pooled and non-singleton producers we have to use the ProducerCache as it supports these
        // kind of producer better (though these kind of producer should be rare)

        AsyncProducer producer = producerCache.acquireProducer(destination);
        producerCache.releaseProducer(destination, producer);
    }

    protected void doStop() throws Exception {
        ServiceHelper.stopService(producerCache);
    }

    protected void doShutdown() throws Exception {
        ServiceHelper.stopAndShutdownServices(producerCache);
    }
}
