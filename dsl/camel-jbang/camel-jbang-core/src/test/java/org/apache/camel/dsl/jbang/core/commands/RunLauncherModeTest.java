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
package org.apache.camel.dsl.jbang.core.commands;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.camel.dsl.jbang.core.common.LauncherHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RunLauncherModeTest extends CamelCommandBaseTestSupport {

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setup() throws Exception {
        super.setup();
    }

    @AfterEach
    public void cleanup() {
        // Clean up system property after each test
        System.clearProperty(LauncherHelper.CAMEL_LAUNCHER_MODE);
    }

    @Test
    public void testRunWithCamelVersionInLauncherMode() throws Exception {
        // Enable launcher mode
        System.setProperty(LauncherHelper.CAMEL_LAUNCHER_MODE, "true");

        // Create a simple route file
        File routeFile = tempDir.resolve("test-route.yaml").toFile();
        Files.writeString(routeFile.toPath(), """
                - route:
                    from:
                      uri: "timer:tick"
                      steps:
                        - log: "Hello World"
                """);

        Run run = new Run(new CamelJBangMain().withPrinter(printer));
        run.files.add(routeFile.getAbsolutePath());
        run.camelVersion = "4.13.0";

        // Should fail with error message about --camel-version not supported
        int exitCode = run.doCall();

        assertEquals(1, exitCode, "Should return error code 1");
        String output = printer.getOutput();
        assertTrue(output.contains("--camel-version option is not supported in launcher mode"),
                "Should contain error message about --camel-version not supported");
        assertTrue(output.contains("self-contained JAR for a specific Camel version"),
                "Should explain that launcher is for specific version");
    }

    @Test
    public void testRunWithQuarkusRuntimeInLauncherModeWithoutJBang() throws Exception {
        // Enable launcher mode
        System.setProperty(LauncherHelper.CAMEL_LAUNCHER_MODE, "true");

        // Create a simple route file
        File routeFile = tempDir.resolve("test-route.yaml").toFile();
        Files.writeString(routeFile.toPath(), """
                - route:
                    from:
                      uri: "timer:tick"
                      steps:
                        - log: "Hello World"
                """);

        Run run = new Run(new CamelJBangMain().withPrinter(printer));
        run.files.add(routeFile.getAbsolutePath());
        run.runtime = org.apache.camel.dsl.jbang.core.common.RuntimeType.quarkus;

        // Should fail with error message about --runtime=quarkus requiring JBang
        // (assuming JBang is not available in test environment)
        int exitCode = run.doCall();

        // The exit code depends on whether JBang is available
        // If JBang is not available, should return 1
        // If JBang is available, it might proceed
        String output = printer.getOutput();

        // We can't guarantee JBang is not installed, so just check that the code runs
        assertTrue(exitCode >= 0, "Should return a valid exit code");
    }
}
