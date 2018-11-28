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

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.camel.AsyncCallback;
import org.apache.camel.AsyncProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.support.AsyncProcessorConverterHelper;
import org.apache.camel.support.ServiceHelper;
import org.apache.camel.support.ServiceSupport;

/**
 * A bridge to have regular interceptors implemented as {@link org.apache.camel.Processor}
 * work with the asynchronous routing engine without causing side effects.
 */
public class InterceptorToAsyncProcessorBridge extends ServiceSupport implements AsyncProcessor {

    private final AsyncProcessor interceptor;
    private volatile AsyncProcessor target;
    private volatile ThreadLocal<AsyncCallback> callback = new ThreadLocal<>();

    /**
     * Constructs the bridge
     *
     * @param interceptor the interceptor to bridge
     */
    public InterceptorToAsyncProcessorBridge(Processor interceptor) {
        this.interceptor = AsyncProcessorConverterHelper.convert(interceptor);
    }

    /**
     * Process invoked by the interceptor
     * @param exchange the message exchange
     * @throws Exception
     */
    public void process(Exchange exchange) throws Exception {
        // invoke when interceptor wants to invoke
        interceptor.process(exchange, callback.get());
    }

    public void process(Exchange exchange, AsyncCallback callback) {
        // remember the callback to be used by the interceptor
        this.callback.set(callback);
        try {
            // invoke the target
            target.process(exchange, callback);
        } finally {
            // cleanup
            this.callback.remove();
        }
    }

    public void setTarget(Processor target) {
        this.target = AsyncProcessorConverterHelper.convert(target);
    }

    @Override
    public String toString() {
        return "AsyncBridge[" + interceptor.toString() + "]";
    }

    @Override
    protected void doStart() throws Exception {
        ServiceHelper.startService(target, interceptor);
    }

    @Override
    protected void doStop() throws Exception {
        callback.remove();
        ServiceHelper.stopService(interceptor, target);
    }
}
