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
package org.apache.camel.model.io;

import java.io.Reader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.camel.model.RoutesDefinition;

public class XmlReader {

    public RoutesDefinition read(Reader reader, boolean strict) throws XMLStreamException {
        XMLStreamReader xmlStreamReader = XMLInputFactory.newInstance().createXMLStreamReader(reader);
        return read(xmlStreamReader, strict);
    }

    private RoutesDefinition read(XMLStreamReader xmlStreamReader, boolean strict) throws XMLStreamException {
        int eventType = xmlStreamReader.getEventType();
        while (eventType != XMLStreamConstants.END_DOCUMENT) {
            if (eventType == XMLStreamConstants.START_ELEMENT) {
                // TODO : check namespace
                if (strict && !"routes".equals(xmlStreamReader.getLocalName())) {
                    throw new XMLStreamException("Expected root element 'routes' but found '" + xmlStreamReader.getLocalName() + "'", xmlStreamReader.getLocation(), null);
                }
                return ModelParser.parseRoutesDefinition(xmlStreamReader, strict);
            }
            eventType = xmlStreamReader.next();
        }
        throw new XMLStreamException("Expected root element 'routes' but found no element at all: invalid XML document", xmlStreamReader.getLocation(), null);
    }

}
