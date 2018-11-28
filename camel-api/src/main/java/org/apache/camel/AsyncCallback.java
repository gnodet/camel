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

/**
 * The callback interface for an {@link AsyncProcessor} so that it can
 * notify you when an {@link Exchange} is done.
 * <p/>
 * For example a {@link AsyncProcessor} should invoke the done method when the {@link Exchange} is ready
 * to be continued routed. This allows to implement asynchronous {@link Producer} which can continue
 * routing {@link Exchange} when all the data has been gathered. This allows to build non blocking
 * request/reply communication.
 */
public interface AsyncCallback extends Runnable {

    AsyncCallback EMPTY = () -> { };

    /**
     * This method is invoked once the {@link Exchange} is done.
     * <p/>
     * If an exception occurred while processing the exchange, the exception field of the
     * {@link Exchange} being processed will hold the caused exception.
     */
    void done();

    default void run() {
        done();
    }

    default AsyncCallback then(AsyncCallback cb) {
        return () -> {
            try {
                this.done();
            } finally {
                cb.done();
            }
        };
    }
}
