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
package org.apache.camel.component.file.remote.sftp;

import java.io.File;

import org.apache.camel.Exchange;
import org.apache.camel.converter.IOConverter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SftpProducerFileWithPathTest extends SftpServerTestSupport {

    private String getFtpUrl() {
        return "sftp://admin@localhost:" + getPort() + "/" + FTP_ROOT_DIR + "?password=admin";
    }

    @Test
    public void testProducerFileWithPath() throws Exception {
        if (!canTest()) {
            return;
        }

        template.sendBodyAndHeader(getFtpUrl(), "Hello World", Exchange.FILE_NAME, "hello/claus.txt");

        File file = new File(FTP_ROOT_DIR + "/hello/claus.txt");
        assertTrue(file.exists(), "The uploaded file should exists");
        assertEquals("Hello World", IOConverter.toString(file, null));
    }

    @Test
    public void testProducerFileWithPathTwice() throws Exception {
        if (!canTest()) {
            return;
        }

        template.sendBodyAndHeader(getFtpUrl(), "Hello World", Exchange.FILE_NAME, "hello/claus.txt");
        template.sendBodyAndHeader(getFtpUrl(), "Hello Again World", Exchange.FILE_NAME, "hello/andrea.txt");

        File file = new File(FTP_ROOT_DIR + "/hello/claus.txt");
        assertTrue(file.exists(), "The uploaded file should exists");
        assertEquals("Hello World", IOConverter.toString(file, null));

        file = new File(FTP_ROOT_DIR + "/hello/andrea.txt");
        assertTrue(file.exists(), "The uploaded file should exists");
        assertEquals("Hello Again World", IOConverter.toString(file, null));
    }

    @Test
    public void testProducerFileWithPathExistDirCheckUsingLs() throws Exception {
        if (!canTest()) {
            return;
        }

        template.sendBodyAndHeader(getFtpUrl() + "&existDirCheckUsingLs=false", "Bye World", Exchange.FILE_NAME,
                "bye/andrea.txt");

        File file = new File(FTP_ROOT_DIR + "/bye/andrea.txt");
        assertTrue(file.exists(), "The uploaded file should exists");
        assertEquals("Bye World", IOConverter.toString(file, null));
    }

    @Test
    public void testProducerFileWithPathExistDirCheckUsingLsTwice() throws Exception {
        if (!canTest()) {
            return;
        }

        template.sendBodyAndHeader(getFtpUrl() + "&existDirCheckUsingLs=false", "Bye World", Exchange.FILE_NAME,
                "bye/andrea.txt");
        template.sendBodyAndHeader(getFtpUrl() + "&existDirCheckUsingLs=false", "Bye Again World", Exchange.FILE_NAME,
                "bye/claus.txt");

        File file = new File(FTP_ROOT_DIR + "/bye/andrea.txt");
        assertTrue(file.exists(), "The uploaded file should exists");
        assertEquals("Bye World", IOConverter.toString(file, null));

        file = new File(FTP_ROOT_DIR + "/bye/claus.txt");
        assertTrue(file.exists(), "The uploaded file should exists");
        assertEquals("Bye Again World", IOConverter.toString(file, null));
    }

}
