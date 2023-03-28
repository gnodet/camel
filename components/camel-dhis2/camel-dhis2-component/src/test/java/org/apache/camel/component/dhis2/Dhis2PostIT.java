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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.dhis2.internal.Dhis2ApiCollection;
import org.apache.camel.component.dhis2.internal.Dhis2PostApiMethod;
import org.hisp.dhis.api.model.v2_38_1.OrganisationUnit;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test class for {@link org.apache.camel.component.dhis2.api.Dhis2Post} APIs.
 */
public class Dhis2PostIT extends AbstractDhis2TestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(Dhis2PostIT.class);
    private static final String PATH_PREFIX = Dhis2ApiCollection.getCollection().getApiName(Dhis2PostApiMethod.class).getName();

    @Test
    public void testResource() throws Exception {
        final Map<String, Object> headers = new HashMap<String, Object>();
        // parameter type is String
        headers.put("CamelDhis2.path", "organisationUnits");
        // parameter type is java.util.Map
        headers.put("CamelDhis2.queryParams", new HashMap<>());

        final java.io.InputStream result = requestBodyAndHeaders("direct://RESOURCE",
                new OrganisationUnit().withName("Foo").withShortName("Foo").withOpeningDate(new Date()),
                headers);

        assertNotNull(result, "resource result");
        LOG.debug("resource: " + result);
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() {
                // test route for resource
                from("direct://RESOURCE")
                        .to("dhis2://" + PATH_PREFIX + "/resource?inBody=resource");

            }
        };
    }
}
