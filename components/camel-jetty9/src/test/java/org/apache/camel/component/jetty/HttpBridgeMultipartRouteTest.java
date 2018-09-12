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
package org.apache.camel.component.jetty;

import java.io.File;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpEndpoint;
import org.apache.camel.impl.DefaultHeaderFilterStrategy;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import org.junit.Test;

public class HttpBridgeMultipartRouteTest extends BaseJettyTest {

    private int port1;
    private int port2;

    private static class MultipartHeaderFilterStrategy extends DefaultHeaderFilterStrategy {
        MultipartHeaderFilterStrategy() {
            initialize();
        }

        protected void initialize() {
            setLowerCase(true);
            setOutFilterPattern("(?i)(Camel|org\\.apache\\.camel)[\\.|a-z|A-z|0-9]*");
        }
    }
    
    @Test
    public void testHttpClient() throws Exception {
        File jpg = new File("src/test/resources/java.jpg");
        String body = "TEST";
        HttpEntity reqEntity = MultipartEntityBuilder.create()
                .addTextBody("body", body)
                .addBinaryBody(jpg.getName(), jpg)
                .build();

        HttpPost method = new HttpPost("http://localhost:" + port2 + "/test/hello");
        method.setEntity(reqEntity);

        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse response = client.execute(method);
        
        String responseBody = EntityUtils.toString(response.getEntity());
        assertEquals(body, responseBody);
        
        String numAttachments = response.getFirstHeader("numAttachments").getValue();
        assertEquals(numAttachments, "2");
    }

    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() {
                port1 = getPort();
                port2 = getNextPort();

                errorHandler(noErrorHandler());

                Processor serviceProc = new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        Message in = exchange.getIn();
                        // put the number of attachments in a response header
                        exchange.getOut().setHeader("numAttachments", in.getAttachments().size());
                        exchange.getOut().setBody(in.getHeader("body"));
                    }
                };
                
                HttpEndpoint epOut = getContext().getEndpoint("http://localhost:" + port1 + "?bridgeEndpoint=true&throwExceptionOnFailure=false", HttpEndpoint.class);
                epOut.setHeaderFilterStrategy(new MultipartHeaderFilterStrategy());
                
                from("jetty:http://localhost:" + port2 + "/test/hello?enableMultipartFilter=false")
                    .to(epOut);
                
                from("jetty://http://localhost:" + port1 + "?matchOnUriPrefix=true").process(serviceProc);
            }
        };
    }    

}