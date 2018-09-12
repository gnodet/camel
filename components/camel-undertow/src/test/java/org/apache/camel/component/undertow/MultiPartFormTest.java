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
package org.apache.camel.component.undertow;

import java.io.File;
import java.util.Map;

import javax.activation.DataHandler;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import org.junit.Test;

public class MultiPartFormTest extends BaseUndertowTest {
    private HttpEntity createMultipartRequestEntity() throws Exception {
        File file = new File("src/main/resources/META-INF/NOTICE.txt");
        return MultipartEntityBuilder.create()
                .addTextBody("comment", "A binary file of some kind")
                .addBinaryBody(file.getName(), file)
                .build();

    }

    @Test
    public void testSendMultiPartForm() throws Exception {
        HttpClient httpclient = HttpClientBuilder.create().build();

        HttpPost httppost = new HttpPost("http://localhost:" + getPort() + "/test");

        httppost.setEntity(createMultipartRequestEntity());

        HttpResponse response = httpclient.execute(httppost);

        assertEquals("Get a wrong response status", 200, response.getStatusLine().getStatusCode());
        String result = EntityUtils.toString(httppost.getEntity());

        assertEquals("Get a wrong result", "A binary file of some kind", result);
    }

    @Test
    public void testSendMultiPartFormFromCamelHttpComponnent() throws Exception {
        String result = template.requestBody("http://localhost:" + getPort() + "/test", createMultipartRequestEntity(), String.class);
        assertEquals("Get a wrong result", "A binary file of some kind", result);
    }

    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() throws Exception {
                from("undertow://http://localhost:{{port}}/test").process(new Processor() {

                    public void process(Exchange exchange) throws Exception {
                        Message in = exchange.getIn();
                        assertEquals("Get a wrong attachement size", 1, in.getAttachments().size());
                        // The file name is attachment id
                        DataHandler data = in.getAttachment("NOTICE.txt");

                        assertNotNull("Should get the DataHandler NOTICE.txt", data);
                        assertEquals("Got the wrong name", "NOTICE.txt", data.getName());

                        assertTrue("We should get the data from the DataHandler", data.getDataSource()
                            .getInputStream().available() > 0);

                        // form data should also be available as a body
                        Map body = in.getBody(Map.class);
                        assertEquals("A binary file of some kind", body.get("comment"));
                        assertEquals(data, body.get("NOTICE.txt"));
                        exchange.getOut().setBody(in.getHeader("comment"));
                    }

                });
                // END SNIPPET: e1
            }
        };
    }

}
