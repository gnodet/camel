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
package org.apache.camel.opentracing.decorators;

import org.apache.camel.Exchange;

public class AwsSnsSpanDecorator extends AbstractMessagingSpanDecorator {

    public static final String CAMEL_AWS_SNS_MESSAGE_ID = "CamelAwsSnsMessageId";

    @Override
    public String getComponent() {
        return "aws-sns";
    }

    @Override
    public String getComponentClassName() {
        return "org.apache.camel.component.aws.sns.SnsComponent";
    }

    @Override
    protected String getMessageId(Exchange exchange) {
        return (String) exchange.getIn().getHeader(CAMEL_AWS_SNS_MESSAGE_ID);
    }

}
