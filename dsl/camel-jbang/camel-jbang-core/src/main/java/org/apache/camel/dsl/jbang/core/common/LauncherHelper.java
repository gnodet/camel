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

/**
 * Helper class for launcher-related utilities.
 */
public final class LauncherHelper {

    /**
     * System property to indicate that Camel is running in launcher mode (not JBang mode).
     */
    public static final String CAMEL_LAUNCHER_MODE = "camel.launcher.mode";

    private LauncherHelper() {
    }

    /**
     * Checks if Camel is running in launcher mode (standalone JAR) rather than JBang mode.
     *
     * @return true if running in launcher mode, false otherwise
     */
    public static boolean isLauncherMode() {
        return "true".equals(System.getProperty(CAMEL_LAUNCHER_MODE));
    }

    /**
     * Checks if JBang is available on the system.
     *
     * @return true if JBang is available, false otherwise
     */
    public static boolean isJBangAvailable() {
        // If we're in launcher mode, we still might have jbang available
        // Check for JBANG_HOME or try to find jbang command
        String jbangHome = System.getenv("JBANG_HOME");
        if (jbangHome != null && !jbangHome.isBlank()) {
            return true;
        }

        // Try to find jbang in PATH
        try {
            ProcessBuilder pb = new ProcessBuilder();
            if (org.apache.camel.util.FileUtil.isWindows()) {
                pb.command("where", "jbang");
            } else {
                pb.command("which", "jbang");
            }
            Process p = pb.start();
            int exitCode = p.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
