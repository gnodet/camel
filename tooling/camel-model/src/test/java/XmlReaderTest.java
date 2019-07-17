import java.io.StringReader;

import org.apache.camel.model.io.XmlReader;
import org.apache.camel.model.structs.RoutesDefinition;
import org.junit.Test;

public class XmlReaderTest {

    @Test
    public void testReadRoutes() throws Exception {
        String xml =
                "<routes>\n" +
                "  <route id=\"myRoute\">\n" +
                "    <from uri=\"file:mydir\" />\n" +
                "    <to uri=\"file:anotherdir\" />\n" +
                "  </route>\n" +
                "</routes>\n";
        RoutesDefinition routes = new XmlReader().read(new StringReader(xml), true);
    }
}
