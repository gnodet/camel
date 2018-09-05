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
package org.apache.camel.model.loadbalancer;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.camel.model.LoadBalancerDefinition;
import org.apache.camel.spi.Metadata;

/**
 * Circuit break load balancer
 * <p/>
 * The Circuit Breaker load balancer is a stateful pattern that monitors all calls for certain exceptions.
 * Initially the Circuit Breaker is in closed state and passes all messages.
 * If there are failures and the threshold is reached, it moves to open state and rejects all calls until halfOpenAfter
 * timeout is reached. After this timeout is reached, if there is a new call, it will pass and if the result is
 * success the Circuit Breaker will move to closed state, or to open state if there was an error.
 *
 * @deprecated use Hystrix EIP instead which is the popular Netflix implementation of circuit breaker
 */
@Metadata(label = "eip,routing,loadbalance,circuitbreaker")
@XmlRootElement(name = "circuitBreaker")
@XmlAccessorType(XmlAccessType.FIELD)
@Deprecated
public class CircuitBreakerLoadBalancerDefinition extends LoadBalancerDefinition {
    @XmlTransient
    private List<Class<?>> exceptionTypes = new ArrayList<>();
    @XmlElement(name = "exception")
    private List<String> exceptions = new ArrayList<>();
    @XmlAttribute
    private Long halfOpenAfter;
    @XmlAttribute
    private Integer threshold;

    public CircuitBreakerLoadBalancerDefinition() {
    }

    @Override
    public int getMaximumNumberOfOutputs() {
        // we can only support 1 output
        return 1;
    }

    public Long getHalfOpenAfter() {
        return halfOpenAfter;
    }

    /**
     * The timeout in millis to use as threshold to move state from closed to half-open or open state
     */
    public void setHalfOpenAfter(Long halfOpenAfter) {
        this.halfOpenAfter = halfOpenAfter;
    }

    public Integer getThreshold() {
        return threshold;
    }

    /**
     * Number of previous failed messages to use as threshold to move state from closed to half-open or open state
     */
    public void setThreshold(Integer threshold) {
        this.threshold = threshold;
    }

    public List<String> getExceptions() {
        return exceptions;
    }

    /**
     * A list of class names for specific exceptions to monitor.
     * If no exceptions is configured then all exceptions is monitored
     */
    public void setExceptions(List<String> exceptions) {
        this.exceptions = exceptions;
    }

    public List<Class<?>> getExceptionTypes() {
        return exceptionTypes;
    }

    /**
     * A list of specific exceptions to monitor.
     * If no exceptions is configured then all exceptions is monitored
     */
    public void setExceptionTypes(List<Class<?>> exceptionTypes) {
        this.exceptionTypes = exceptionTypes;
    }

    @Override
    public String toString() {
        return "CircuitBreakerLoadBalancer";
    }
}
