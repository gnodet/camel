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
package org.apache.camel.processor.loadbalancer;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.camel.AsyncProcessor;
import org.apache.camel.LoadBalancer;
import org.apache.camel.Navigate;
import org.apache.camel.Processor;
import org.apache.camel.spi.IdAware;
import org.apache.camel.support.AsyncProcessorSupport;
import org.apache.camel.support.ServiceHelper;

/**
 * A default base class for a {@link LoadBalancer} implementation.
 */
public abstract class LoadBalancerSupport extends AsyncProcessorSupport implements LoadBalancer, Navigate<Processor>, IdAware {

    private final AtomicReference<AsyncProcessor[]> processors = new AtomicReference<>(new AsyncProcessor[0]);
    private String id;

    public void addProcessor(AsyncProcessor processor) {
        processors.updateAndGet(op -> doAdd(processor, op));
    }

    public void removeProcessor(AsyncProcessor processor) {
        processors.updateAndGet(op -> doRemove(processor, op));
    }

    private AsyncProcessor[] doAdd(AsyncProcessor processor, AsyncProcessor[] op) {
        int len = op.length;
        AsyncProcessor[] np = Arrays.copyOf(op, len + 1, op.getClass());
        np[len] = processor;
        return np;
    }

    private AsyncProcessor[] doRemove(AsyncProcessor processor, AsyncProcessor[] op) {
        int len = op.length;
        for (int index = 0; index < len; index++) {
            if (op[index].equals(processor)) {
                AsyncProcessor[] np = (AsyncProcessor[]) Array.newInstance(AsyncProcessor.class, len - 1);
                System.arraycopy(op, 0, np, 0, index);
                System.arraycopy(op, index + 1, np, index, len - index - 1);
                return np;
            }
        }
        return op;
    }

    public List<AsyncProcessor> getProcessors() {
        return Arrays.asList(processors.get());
    }

    protected AsyncProcessor[] doGetProcessors() {
        return processors.get();
    }

    @SuppressWarnings("unchecked")
    public List<Processor> next() {
        if (!hasNext()) {
            return null;
        }
        return (List) getProcessors();
    }

    public boolean hasNext() {
        return doGetProcessors().length > 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    protected void doStart() throws Exception {
        ServiceHelper.startService((Object[]) processors.get());
    }

    protected void doStop() throws Exception {
        ServiceHelper.stopService((Object[]) processors.get());
    }

    @Override
    protected void doShutdown() throws Exception {
        AsyncProcessor[] p = processors.get();
        ServiceHelper.stopAndShutdownServices((Object[]) p);
        for (AsyncProcessor processor : p) {
            removeProcessor(processor);
        }
    }

    public String toString() {
        return getClass().getSimpleName() + Arrays.toString(doGetProcessors());
    }

}
