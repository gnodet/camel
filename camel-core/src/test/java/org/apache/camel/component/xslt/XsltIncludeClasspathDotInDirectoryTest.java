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
package org.apache.camel.component.xslt;
import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.apache.camel.ContextTestSupport;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.util.FileUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class XsltIncludeClasspathDotInDirectoryTest extends ContextTestSupport {

    Path thePath;

    @Override
    @Before
    public void setUp() throws Exception {
        URL url = getClass().getClassLoader().getResource(getClass().getName().replace('.', '/') + ".class");
        Path p = Paths.get(url.toURI());
        while (!Objects.equals("org", p.getFileName().toString())) {
            p = p.getParent();
        }
        thePath = p.getParent().resolve("com.mycompany");

        deleteDirectory(thePath);
        createDirectory(thePath);

        // copy templates to this directory
        FileUtil.copyFile(getBaseDir().resolve("src/test/resources/org/apache/camel/component/xslt/staff_include_classpath2.xsl").toFile(),
                thePath.resolve("staff_include_classpath2.xsl").toFile());

        FileUtil.copyFile(getBaseDir().resolve("src/test/resources/org/apache/camel/component/xslt/staff_template.xsl").toFile(),
                thePath.resolve("staff_template.xsl").toFile());

         super.setUp();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        deleteDirectory(thePath);
        super.tearDown();
    }

    @Test
    public void testXsltIncludeClasspath() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(1);
        // the include file has the span style so check that its there
        mock.message(0).body().contains("<span style=\"font-size=22px;\">Minnie Mouse</span>");

        assertMockEndpointsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("file:src/test/data/?fileName=staff.xml&noop=true&initialDelay=0&delay=10")
                    .to("xslt:com.mycompany/staff_include_classpath2.xsl")
                    .to("log:foo")
                    .to("mock:result");
            }
        };
    }
}