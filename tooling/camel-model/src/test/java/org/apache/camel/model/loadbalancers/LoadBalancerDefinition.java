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
package org.apache.camel.model.loadbalancers;

import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.camel.model.IdentifiedType;
import org.apache.camel.model.OtherAttributesAware;
import org.apache.camel.spi.LoadBalancer;
import org.apache.camel.spi.Metadata;

/**
 * Balances message processing among a number of nodes
 */
@Metadata(label = "eip,routing")
public class LoadBalancerDefinition extends IdentifiedType implements OtherAttributesAware {
    private LoadBalancer loadBalancer;
    private String loadBalancerTypeName;
    // use xs:any to support optional property placeholders
    private Map<QName, Object> otherAttributes;

    private Map<String, Object> properties;

    public LoadBalancerDefinition() {
    }

    public LoadBalancerDefinition(LoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    protected LoadBalancerDefinition(String loadBalancerTypeName) {
        this.loadBalancerTypeName = loadBalancerTypeName;
    }

    protected void doSetProperty(String name, Object value) {
        properties.put(name, value);
    }

    protected Object doGetProperty(String name) {
        return properties.get(name);
    }

    /**
     * Maximum number of outputs, as some load balancers only support 1 processor
     */
    public int getMaximumNumberOfOutputs() {
        return Integer.MAX_VALUE;
    }

    /**
     * Allows derived classes to customize the load balancer
     */
    public void configureLoadBalancer(LoadBalancer loadBalancer) {
    }

    public LoadBalancer getLoadBalancer() {
        return loadBalancer;
    }

    public void setLoadBalancer(LoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    public String getLoadBalancerTypeName() {
        return loadBalancerTypeName;
    }

    @Override
    public Map<QName, Object> getOtherAttributes() {
        return otherAttributes;
    }

    @Override
    public void setOtherAttributes(Map<QName, Object> otherAttributes) {
        this.otherAttributes = otherAttributes;
    }

    @Override
    public String toString() {
        if (loadBalancer != null) {
            return loadBalancer.toString();
        } else {
            return loadBalancerTypeName;
        }
    }
}
