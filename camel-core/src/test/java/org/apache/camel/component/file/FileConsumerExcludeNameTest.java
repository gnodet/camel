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
package org.apache.camel.component.file;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.camel.ContextTestSupport;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test that file consumer will exclude pre and postfixes
 */
public class FileConsumerExcludeNameTest extends ContextTestSupport {

    Path dir;
    String uri;

    @Override
    @Before
    public void setUp() throws Exception {
        dir = getTestDataDir();
        uri = dir.toUri().toString();
        super.setUp();
    }

    @Test
    @Retry
    public void testExludePreAndPostfixes() throws Exception {
        prepareFiles();

        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedBodiesReceivedInAnyOrder("Reports1", "Reports2", "Reports3");
        mock.expectedMessageCount(3);
        mock.assertIsSatisfied();
    }

    private void prepareFiles() throws Exception {
        template.sendBodyAndHeader(uri, "Hello World", Exchange.FILE_NAME, "hello.xml");
        template.sendBodyAndHeader(uri, "Reports1", Exchange.FILE_NAME, "report1.txt");
        template.sendBodyAndHeader(uri, "Bye World", Exchange.FILE_NAME, "secret.txt");
        template.sendBodyAndHeader(uri, "Reports2", Exchange.FILE_NAME, "report2.txt");
        template.sendBodyAndHeader(uri, "Reports3", Exchange.FILE_NAME, "Report3.txt");
        template.sendBodyAndHeader(uri, "Secret2", Exchange.FILE_NAME, "Secret2.txt");
    }

    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() throws Exception {
                from(uri + "/?initialDelay=0&delay=10&exclude=^secret.*|.*xml$")
                    .convertBodyTo(String.class).to("mock:result");
            }
        };
    }

}
