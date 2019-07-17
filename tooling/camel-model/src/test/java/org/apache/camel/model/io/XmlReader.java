package org.apache.camel.model.io;

import java.io.IOException;
import java.io.Reader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.camel.model.structs.RoutesDefinition;

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
