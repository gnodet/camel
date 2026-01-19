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
package org.apache.camel.dsl.jbang.core.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LauncherHelperTest {

    @AfterEach
    public void cleanup() {
        // Clean up system property after each test
        System.clearProperty(LauncherHelper.CAMEL_LAUNCHER_MODE);
    }

    @Test
    public void testIsLauncherModeWhenNotSet() {
        // When the system property is not set, should return false
        assertFalse(LauncherHelper.isLauncherMode());
    }

    @Test
    public void testIsLauncherModeWhenSetToTrue() {
        // When the system property is set to "true", should return true
        System.setProperty(LauncherHelper.CAMEL_LAUNCHER_MODE, "true");
        assertTrue(LauncherHelper.isLauncherMode());
    }

    @Test
    public void testIsLauncherModeWhenSetToFalse() {
        // When the system property is set to "false", should return false
        System.setProperty(LauncherHelper.CAMEL_LAUNCHER_MODE, "false");
        assertFalse(LauncherHelper.isLauncherMode());
    }

    @Test
    public void testIsLauncherModeWhenSetToOtherValue() {
        // When the system property is set to something other than "true", should return false
        System.setProperty(LauncherHelper.CAMEL_LAUNCHER_MODE, "yes");
        assertFalse(LauncherHelper.isLauncherMode());
    }
}
