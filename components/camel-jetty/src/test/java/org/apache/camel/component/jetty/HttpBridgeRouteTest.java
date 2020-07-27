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
package org.apache.camel.component.jetty;

import java.io.ByteArrayInputStream;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.builder.RouteBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class HttpBridgeRouteTest extends BaseJettyTest {

    protected int port1;
    protected int port2;

    @Test
    public void testHttpClient() throws Exception {
        String response = template.requestBodyAndHeader("http://localhost:" + port2 + "/test/hello",
                new ByteArrayInputStream("This is a test".getBytes()), "Content-Type",
                "application/xml", String.class);
        assertEquals("/", response, "Get a wrong response");

        response = template.requestBody("http://localhost:" + port1 + "/hello/world", "hello", String.class);
        assertEquals("/hello/world", response, "Get a wrong response");

        try {
            template.requestBody("http://localhost:" + port2 + "/hello/world", "hello", String.class);
            fail("Expect exception here!");
        } catch (Exception ex) {
            assertTrue(ex instanceof RuntimeCamelException, "We should get a RuntimeCamelException");
        }
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() {
                port1 = getPort();
                port2 = getNextPort();

                errorHandler(noErrorHandler());

                Processor serviceProc = new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        // get the request URL and copy it to the request body
                        String uri = exchange.getIn().getHeader(Exchange.HTTP_URI, String.class);
                        exchange.getOut().setBody(uri);
                    }
                };
                from("jetty:http://localhost:" + port2 + "/test/hello")
                        .to("http://localhost:" + port1 + "?throwExceptionOnFailure=false&bridgeEndpoint=true");

                from("jetty://http://localhost:" + port1 + "?matchOnUriPrefix=true").process(serviceProc);
            }
        };
    }

}
