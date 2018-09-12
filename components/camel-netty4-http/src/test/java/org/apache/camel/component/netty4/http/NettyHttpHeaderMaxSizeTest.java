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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import org.junit.Test;

public class NettyHttpHeaderMaxSizeTest extends BaseNettyTest {

    @Test
    public void testHttpHeaderMaxSizeOk() throws Exception {
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost method = new HttpPost("http://localhost:" + getPort() + "/myapp/mytest");

        method.setHeader("name", "you");

        HttpResponse response = client.execute(method);

        assertEquals(200, response.getStatusLine().getStatusCode());
        assertEquals("Bye World", EntityUtils.toString(response.getEntity()));
    }

    @Test
    public void testHttpHeaderMaxSizeFail() throws Exception {
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost method = new HttpPost("http://localhost:" + getPort() + "/myapp/mytest");

        method.setHeader("name", "12345678901234567890123456789012345678901234567890123456789012345678901234567890");

        HttpResponse response = client.execute(method);

        assertEquals(400, response.getStatusLine().getStatusCode());
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() throws Exception {
                from("netty4-http:http://localhost:{{port}}/myapp/mytest?maxHeaderSize=100")
                        .transform().constant("Bye World");
            }
        };
    }

}
