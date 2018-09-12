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
package org.apache.camel.component.netty4.http;

import org.apache.camel.builder.RouteBuilder;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.impl.client.HttpClientBuilder;

import org.junit.Test;

public class NettyHttpTraceDisabledTest extends BaseNettyTest {

    private int portTraceOn = getNextPort();
    private int portTraceOff = getNextPort();

    @Test
    public void testTraceDisabled() throws Exception {
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpTrace trace = new HttpTrace("http://localhost:" + portTraceOff + "/myservice");
        HttpResponse response = httpclient.execute(trace);

        // TRACE shouldn't be allowed by default
        assertTrue(response.getStatusLine().getStatusCode() == 405);
        trace.releaseConnection();
    }

    @Test
    public void testTraceEnabled() throws Exception {
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpTrace trace = new HttpTrace("http://localhost:" + portTraceOn + "/myservice");
        HttpResponse response = httpclient.execute(trace);

        // TRACE is now allowed
        assertTrue(response.getStatusLine().getStatusCode() == 200);
        trace.releaseConnection();
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("netty4-http:http://localhost:" + portTraceOff + "/myservice").to("log:foo");
                from("netty4-http:http://localhost:" + portTraceOn + "/myservice?traceEnabled=true").to("log:bar");
            }
        };
    }

}
