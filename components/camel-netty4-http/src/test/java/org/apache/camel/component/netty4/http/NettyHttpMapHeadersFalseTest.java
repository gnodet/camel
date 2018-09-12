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

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import org.junit.Test;

public class NettyHttpMapHeadersFalseTest extends BaseNettyTest {

    @Test
    public void testHttpHeaderCase() throws Exception {
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost method = new HttpPost("http://localhost:" + getPort() + "/myapp/mytest");

        method.setHeader("clientHeader", "fooBAR");
        method.setHeader("OTHER", "123");
        method.setHeader("beer", "Carlsberg");

        HttpResponse response = client.execute(method);

        assertEquals("Bye World", EntityUtils.toString(response.getEntity()));
        assertEquals("aBc123", response.getFirstHeader("MyCaseHeader").getValue());
        assertEquals("456DEf", response.getFirstHeader("otherCaseHeader").getValue());
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() throws Exception {
                from("netty4-http:http://localhost:{{port}}/myapp/mytest?mapHeaders=false").process(new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        // these headers is not mapped
                        assertNull(exchange.getIn().getHeader("clientHeader"));
                        assertNull(exchange.getIn().getHeader("OTHER"));
                        assertNull(exchange.getIn().getHeader("beer"));

                        // but we can find them in the http request from netty
                        assertEquals("fooBAR", exchange.getIn(NettyHttpMessage.class).getHttpRequest().headers().get("clientHeader"));
                        assertEquals("123", exchange.getIn(NettyHttpMessage.class).getHttpRequest().headers().get("OTHER"));
                        assertEquals("Carlsberg", exchange.getIn(NettyHttpMessage.class).getHttpRequest().headers().get("beer"));

                        exchange.getOut().setBody("Bye World");
                        exchange.getOut().setHeader("MyCaseHeader", "aBc123");
                        exchange.getOut().setHeader("otherCaseHeader", "456DEf");
                    }
                });
            }
        };
    }

}
