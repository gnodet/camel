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

import java.util.zip.Deflater;

import org.apache.camel.model.DataFormatDefinition;
import org.apache.camel.model.dataformat.ZipDataFormat;
import org.apache.camel.reifier.DataFormatReifier;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.RouteContext;

/**
 * Zip Deflate Compression data format is a message compression and de-compression format (not zip files).
 */
public class ZipDataFormatReifier extends DataFormatReifier<ZipDataFormat> {

    public ZipDataFormatReifier(DataFormatDefinition definition) {
        super(ZipDataFormat.class.cast(definition));
    }

    @Override
    protected DataFormat createDataFormat(RouteContext routeContext) {
        if (definition.getCompressionLevel() == null) {
            return new org.apache.camel.impl.ZipDataFormat(Deflater.DEFAULT_COMPRESSION);
        } else {
            return new org.apache.camel.impl.ZipDataFormat(definition.getCompressionLevel());
        }
    }

}
