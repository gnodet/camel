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
package org.apache.camel.util.concurrent;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class SizedScheduledExecutorServiceTest extends Assert {
    
    @Test
    public void testSizedScheduledExecutorService() throws Exception {
        ScheduledThreadPoolExecutor delegate = new ScheduledThreadPoolExecutor(5);
        
        SizedScheduledExecutorService sized = new SizedScheduledExecutorService(delegate, 2);
        
        Runnable task = new Runnable() {
            @Override
            public void run() {
                // noop
            }
        };

        sized.schedule(task, 2, TimeUnit.SECONDS);
        sized.schedule(task, 3, TimeUnit.SECONDS);
        
        try {
            sized.schedule(task, 4, TimeUnit.SECONDS);
            fail("Should have thrown exception");
        } catch (RejectedExecutionException e) {
            assertEquals("Task rejected due queue size limit reached", e.getMessage());
        }

        sized.shutdownNow();
        assertTrue("Should be shutdown", sized.isShutdown() || sized.isTerminating());
        assertTrue("Should be shutdown", delegate.isShutdown() || sized.isTerminating());
    }
}
