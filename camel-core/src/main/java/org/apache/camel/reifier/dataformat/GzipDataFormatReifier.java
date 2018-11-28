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
package org.apache.camel.reifier.dataformat;

import org.apache.camel.model.DataFormatDefinition;
import org.apache.camel.model.dataformat.GzipDataFormat;
import org.apache.camel.reifier.DataFormatReifier;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.RouteContext;

/**
 * The GZip data format is a message compression and de-compression format (which works with the popular gzip/gunzip tools).
 */
public class GzipDataFormatReifier extends DataFormatReifier<GzipDataFormat> {

    public GzipDataFormatReifier(DataFormatDefinition definition) {
        super(GzipDataFormat.class.cast(definition));
    }

    @Override
    protected DataFormat createDataFormat(RouteContext routeContext) {
        return new org.apache.camel.impl.GzipDataFormat();
    }
}
