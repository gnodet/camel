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
package org.apache.camel.component.dhis2;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.dhis2.internal.Dhis2ApiCollection;
import org.apache.camel.component.dhis2.internal.Dhis2ResourceTablesApiMethod;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test class for {@link org.apache.camel.component.dhis2.api.Dhis2ResourceTables} APIs.
 */
public class Dhis2ResourceTablesIT extends AbstractDhis2TestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(Dhis2ResourceTablesIT.class);
    private static final String PATH_PREFIX
            = Dhis2ApiCollection.getCollection().getApiName(Dhis2ResourceTablesApiMethod.class).getName();

    @Test
    public void testAnalytics() throws Exception {
        final Map<String, Object> headers = new HashMap<String, Object>();
        // parameter type is Boolean
        headers.put("CamelDhis2.skipAggregate", false);
        // parameter type is Boolean
        headers.put("CamelDhis2.skipEvents", false);
        // parameter type is Integer
        headers.put("CamelDhis2.lastYears", 2);
        // parameter type is Integer
        headers.put("CamelDhis2.interval", 10000);

        requestBodyAndHeaders("direct://ANALYTICS", null, headers);
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() {
                // test route for analytics
                from("direct://ANALYTICS")
                        .to("dhis2://" + PATH_PREFIX + "/analytics");

            }
        };
    }
}
