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
package org.apache.camel.tooling;

import java.util.Comparator;

import org.apache.camel.maven.packaging.model.EipModel;
import org.apache.camel.maven.packaging.model.EipOptionModel;

public final class EipOptionComparator implements Comparator<EipOptionModel> {

    private final EipModel model;

    public EipOptionComparator(EipModel model) {
        this.model = model;
    }

    @Override
    public int compare(EipOptionModel o1, EipOptionModel o2) {
        int weigth = weigth(o1);
        int weigth2 = weigth(o2);

        if (weigth == weigth2) {
            // keep the current order
            return 1;
        } else {
            // sort according to weight
            return weigth2 - weigth;
        }
    }

    private int weigth(EipOptionModel o) {
        String name = o.getName();

        // these should be first
        if ("expression".equals(name)) {
            return 10;
        }

        // these should be last
        if ("description".equals(name)) {
            return -10;
        } else if ("id".equals(name)) {
            return -9;
        } else if ("pattern".equals(name) && "to".equals(model.getName())) {
            // and pattern only for the to model
            return -8;
        }
        return 0;
    }
}
