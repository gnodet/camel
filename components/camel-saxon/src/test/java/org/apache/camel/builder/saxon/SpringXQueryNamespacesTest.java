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
package org.apache.camel.builder.saxon;

import java.io.FileInputStream;

import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

import net.sf.saxon.Configuration;
import net.sf.saxon.om.AxisInfo;
import net.sf.saxon.om.TreeInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.tiny.TinyElementImpl;
import org.apache.camel.Converter;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.apache.camel.util.IOHelper;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringXQueryNamespacesTest extends CamelSpringTestSupport {

    @Test
    public void testTransform() throws Exception {
        String result = template.requestBody("direct:start", "" +
                "<ns:BuscaListaUFResponse xmlns:ns=\"ld:physical/BuscaListaUF_ws\">" +
                "<ns0:BuscaListaUF xmlns:ns0=\"http://www.vivo.com.br/nfo/vivonet\">" +
                "<VO_CDERRO>1</VO_CDERRO>" +
                "</ns0:BuscaListaUF>" +
                "</ns:BuscaListaUFResponse>", String.class);
        System.out.println(result);
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("org/apache/camel/builder/saxon/SpringXQueryNamespacesTest.xml");
    }

}
